package com.randallengineering.finances.data.repository

import android.content.Context
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.randallengineering.finances.core.network.Resource
import com.randallengineering.finances.data.model.RuleEntity
import com.randallengineering.finances.domain.model.Rule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class RuleRepository(
    private val context: Context,
    private val firestore: FirebaseFirestore? = null
) {
    private val prefs = context.getSharedPreferences("randall_finances_rules", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private val _rulesFlow = MutableStateFlow<List<Rule>>(emptyList())

    init {
        loadLocalRules()
        attachFirestoreListenerIfAvailable()
    }

    private fun loadLocalRules() {
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

    private fun saveLocalRules(list: List<Rule>) {
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
            firestore?.collection("rules")
                ?.orderBy("priority", Query.Direction.ASCENDING)
                ?.addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null && !snapshot.isEmpty) {
                        val list = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(RuleEntity::class.java)?.copy(id = doc.id)?.toDomain()
                        }
                        if (list.isNotEmpty()) {
                            saveLocalRules(list)
                        }
                    }
                }
        } catch (e: Exception) {
            // Offline fallback handles state
        }
    }

    fun getRulesFlow(): Flow<Resource<List<Rule>>> {
        return _rulesFlow.asStateFlow().map { list ->
            Resource.Success(list)
        }
    }

    suspend fun saveRule(rule: Rule): Resource<Unit> {
        val safeRule = if (rule.id.isBlank()) rule.copy(id = UUID.randomUUID().toString()) else rule
        val current = _rulesFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == safeRule.id }
        if (index >= 0) {
            current[index] = safeRule
        } else {
            current.add(safeRule)
        }
        saveLocalRules(current)

        try {
            firestore?.collection("rules")?.document(safeRule.id)
                ?.set(RuleEntity.fromDomain(safeRule))
        } catch (e: Exception) {
            // Ignored if offline
        }
        return Resource.Success(Unit)
    }

    suspend fun deleteRule(ruleId: String): Resource<Unit> {
        val current = _rulesFlow.value.filter { it.id != ruleId }
        saveLocalRules(current)

        try {
            firestore?.collection("rules")?.document(ruleId)?.delete()
        } catch (e: Exception) {
            // Ignored if offline
        }
        return Resource.Success(Unit)
    }

    suspend fun updateRulesPriority(rules: List<Rule>): Resource<Unit> {
        val updated = rules.mapIndexed { index, rule -> rule.copy(priority = index + 1) }
        saveLocalRules(updated)

        try {
            if (firestore != null) {
                val batch = firestore.batch()
                updated.forEach { r ->
                    batch.update(firestore.collection("rules").document(r.id), "priority", r.priority)
                }
                batch.commit()
            }
        } catch (e: Exception) {
            // Ignored if offline
        }
        return Resource.Success(Unit)
    }

    suspend fun incrementMatchCount(ruleId: String): Resource<Unit> {
        val current = _rulesFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == ruleId }
        if (index >= 0) {
            current[index] = current[index].copy(matchCount = current[index].matchCount + 1)
            saveLocalRules(current)

            try {
                firestore?.collection("rules")?.document(ruleId)
                    ?.update("matchCount", FieldValue.increment(1))
            } catch (e: Exception) {
                // Ignored if offline
            }
        }
        return Resource.Success(Unit)
    }
}
