package com.randallengineering.finances.data.repository

import android.content.Context
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.randallengineering.finances.core.network.Resource
import com.randallengineering.finances.data.model.RuleEntity
import com.randallengineering.finances.domain.model.Rule
import com.randallengineering.finances.domain.model.Transaction
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
    private val _autoRunFlow = MutableStateFlow(prefs.getBoolean("auto_run_rules_enabled", true))

    init {
        loadLocalRules()
        attachFirestoreListenerIfAvailable()
    }

    fun isAutoRunEnabled(): Boolean = _autoRunFlow.value

    fun getAutoRunEnabledFlow(): Flow<Boolean> = _autoRunFlow.asStateFlow()

    fun setAutoRunEnabled(enabled: Boolean) {
        _autoRunFlow.value = enabled
        prefs.edit().putBoolean("auto_run_rules_enabled", enabled).apply()
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
        val updated = rules.mapIndexed { idx, r -> r.copy(priority = idx + 1) }
        saveLocalRules(updated)

        try {
            val batch = firestore?.batch()
            if (batch != null) {
                for (r in updated) {
                    val docRef = firestore.collection("rules").document(r.id)
                    batch.update(docRef, "priority", r.priority)
                }
                batch.commit()
            }
        } catch (e: Exception) {
            // Offline fallback
        }
        return Resource.Success(Unit)
    }

    /**
     * Executes a single rule against transactions, updating matching transactions.
     */
    fun applySingleRule(rule: Rule, transactions: List<Transaction>): Pair<List<Transaction>, Int> {
        val updated = mutableListOf<Transaction>()
        var count = 0

        for (tx in transactions) {
            if (rule.matches(tx.originalDesc, tx.amount)) {
                if (tx.category != rule.category || tx.subCategory != rule.subCategory) {
                    updated.add(
                        tx.copy(
                            category = rule.category,
                            subCategory = rule.subCategory,
                            matchedRuleId = rule.id
                        )
                    )
                    count++
                }
            }
        }
        return Pair(updated, count)
    }

    /**
     * Executes all rules sequentially ordered by priority.
     */
    fun applyAllRules(rules: List<Rule>, transactions: List<Transaction>): Pair<List<Transaction>, Int> {
        val sortedRules = rules.filter { it.isActive }.sortedBy { it.priority }
        val updatedMap = mutableMapOf<String, Transaction>()
        var totalUpdatedCount = 0

        for (tx in transactions) {
            var currentTx = tx
            for (rule in sortedRules) {
                if (rule.matches(currentTx.originalDesc, currentTx.amount)) {
                    if (currentTx.category != rule.category || currentTx.subCategory != rule.subCategory) {
                        currentTx = currentTx.copy(
                            category = rule.category,
                            subCategory = rule.subCategory,
                            matchedRuleId = rule.id
                        )
                        updatedMap[currentTx.id] = currentTx
                        totalUpdatedCount++
                    }
                    break // Stop on highest priority matching rule
                }
            }
        }
        return Pair(updatedMap.values.toList(), totalUpdatedCount)
    }
}
