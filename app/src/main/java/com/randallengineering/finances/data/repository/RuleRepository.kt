package com.randallengineering.finances.data.repository

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.randallengineering.finances.core.auth.SyncScope
import com.randallengineering.finances.core.network.Resource
import com.randallengineering.finances.data.model.RuleEntity
import com.randallengineering.finances.domain.model.Rule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class RuleRepository(
    private val context: Context,
    private val firestore: FirebaseFirestore? = null
) {
    private val prefs = context.getSharedPreferences("randall_finances_rules", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val _rulesFlow = MutableStateFlow<List<Rule>>(emptyList())
    private val _autoRunFlow = MutableStateFlow(prefs.getBoolean("auto_run_rules", true))

    fun isAutoRunEnabled(): Boolean = _autoRunFlow.value
    fun getAutoRunEnabledFlow(): Flow<Boolean> = _autoRunFlow.asStateFlow()

    fun setAutoRunEnabled(enabled: Boolean) {
        _autoRunFlow.value = enabled
        prefs.edit().putBoolean("auto_run_rules", enabled).apply()
    }

    init {
        ioScope.launch {
            loadLocalRules()
            attachFirestoreListenerIfAvailable()
        }
    }

    private suspend fun loadLocalRules() = withContext(Dispatchers.IO) {
        try {
            val raw = prefs.getString("cached_rules", null)
            if (!raw.isNullOrBlank()) {
                val list = json.decodeFromString<List<Rule>>(raw)
                _rulesFlow.value = list.sortedBy { it.priority }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun saveLocalRules(list: List<Rule>) = withContext(Dispatchers.IO) {
        try {
            val sorted = list.sortedBy { it.priority }
            _rulesFlow.value = sorted
            prefs.edit().putString("cached_rules", json.encodeToString(sorted)).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun attachFirestoreListenerIfAvailable() {
        try {
            firestore?.collection(SyncScope.path("rules"))
                ?.addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null && !snapshot.isEmpty) {
                        ioScope.launch {
                            val list = snapshot.documents.mapNotNull { doc ->
                                doc.toObject(RuleEntity::class.java)?.copy(id = doc.id)?.toDomain()
                            }
                            if (list.isNotEmpty()) {
                                saveLocalRules(list)
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            // Offline fallback
        }
    }

    fun getRulesFlow(): Flow<Resource<List<Rule>>> {
        return _rulesFlow.asStateFlow()
            .map { Resource.Success(it) }
            .flowOn(Dispatchers.Default)
    }

    suspend fun saveRule(rule: Rule): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val current = _rulesFlow.value.toMutableList()
            val existingIndex = current.indexOfFirst { it.id == rule.id }
            val toSave = if (rule.id.isBlank()) rule.copy(id = UUID.randomUUID().toString()) else rule
            if (existingIndex >= 0) {
                current[existingIndex] = toSave
            } else {
                current.add(toSave)
            }
            saveLocalRules(current)

            firestore?.collection(SyncScope.path("rules"))
                ?.document(toSave.id)
                ?.set(RuleEntity.fromDomain(toSave))
                ?.await()

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to save rule")
        }
    }

    suspend fun deleteRule(id: String): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val current = _rulesFlow.value.filterNot { it.id == id }
            saveLocalRules(current)

            firestore?.collection(SyncScope.path("rules"))
                ?.document(id)
                ?.delete()
                ?.await()

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to delete rule")
        }
    }

    suspend fun updateRulesPriority(rules: List<Rule>): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            saveLocalRules(rules)
            firestore?.let { db ->
                val batch = db.batch()
                rules.forEach { r ->
                    val docRef = db.collection(SyncScope.path("rules")).document(r.id)
                    batch.set(docRef, RuleEntity.fromDomain(r))
                }
                batch.commit().await()
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to update rule priorities")
        }
    }
}
