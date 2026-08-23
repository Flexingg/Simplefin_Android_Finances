package com.randallengineering.finances.data.repository

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.randallengineering.finances.core.network.Resource
import com.randallengineering.finances.data.model.BudgetEntity
import com.randallengineering.finances.domain.model.Budget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class BudgetRepository(
    private val context: Context,
    private val firestore: FirebaseFirestore? = null
) {
    private val prefs = context.getSharedPreferences("randall_finances_budgets", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private val _budgetsFlow = MutableStateFlow<List<Budget>>(emptyList())

    init {
        loadLocalBudgets()
        attachFirestoreListenerIfAvailable()
    }

    private fun loadLocalBudgets() {
        try {
            val raw = prefs.getString("cached_budgets", null)
            if (!raw.isNullOrBlank()) {
                val list = json.decodeFromString<List<Budget>>(raw)
                _budgetsFlow.value = list
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveLocalBudgets(list: List<Budget>) {
        try {
            _budgetsFlow.value = list
            prefs.edit().putString("cached_budgets", json.encodeToString(list)).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun attachFirestoreListenerIfAvailable() {
        try {
            firestore?.collection("budgets")
                ?.addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null && !snapshot.isEmpty) {
                        val list = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(BudgetEntity::class.java)?.copy(id = doc.id)?.toDomain()
                        }
                        if (list.isNotEmpty()) {
                            saveLocalBudgets(list)
                        }
                    }
                }
        } catch (e: Exception) {
            // Offline fallback
        }
    }

    fun getBudgetsFlow(): Flow<Resource<List<Budget>>> {
        return _budgetsFlow.asStateFlow().map { list ->
            Resource.Success(list)
        }
    }

    suspend fun saveBudget(budget: Budget): Resource<Unit> {
        val safeBudget = if (budget.id.isBlank()) budget.copy(id = UUID.randomUUID().toString()) else budget
        val current = _budgetsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == safeBudget.id }
        if (index >= 0) {
            current[index] = safeBudget
        } else {
            current.add(safeBudget)
        }
        saveLocalBudgets(current)

        try {
            firestore?.collection("budgets")?.document(safeBudget.id)
                ?.set(BudgetEntity.fromDomain(safeBudget))
        } catch (e: Exception) {
            // Ignored if offline
        }
        return Resource.Success(Unit)
    }

    suspend fun deleteBudget(budgetId: String): Resource<Unit> {
        val current = _budgetsFlow.value.filter { it.id != budgetId }
        saveLocalBudgets(current)

        try {
            firestore?.collection("budgets")?.document(budgetId)?.delete()
        } catch (e: Exception) {
            // Ignored if offline
        }
        return Resource.Success(Unit)
    }
}
