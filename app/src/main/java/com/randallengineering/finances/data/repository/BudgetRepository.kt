package com.randallengineering.finances.data.repository

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.randallengineering.finances.core.network.Resource
import com.randallengineering.finances.data.model.BudgetEntity
import com.randallengineering.finances.domain.model.Budget
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

class BudgetRepository(
    private val context: Context,
    private val firestore: FirebaseFirestore? = null
) {
    private val prefs = context.getSharedPreferences("randall_finances_budgets", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val _budgetsFlow = MutableStateFlow<List<Budget>>(emptyList())

    init {
        ioScope.launch {
            loadLocalBudgets()
            attachFirestoreListenerIfAvailable()
        }
    }

    private suspend fun loadLocalBudgets() = withContext(Dispatchers.IO) {
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

    private suspend fun saveLocalBudgets(list: List<Budget>) = withContext(Dispatchers.IO) {
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
                        ioScope.launch {
                            val list = snapshot.documents.mapNotNull { doc ->
                                doc.toObject(BudgetEntity::class.java)?.copy(id = doc.id)?.toDomain()
                            }
                            if (list.isNotEmpty()) {
                                saveLocalBudgets(list)
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            // Offline fallback
        }
    }

    fun getBudgetsFlow(): Flow<Resource<List<Budget>>> {
        return _budgetsFlow.asStateFlow()
            .map { Resource.Success(it) }
            .flowOn(Dispatchers.Default)
    }

    suspend fun saveBudget(budget: Budget): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val current = _budgetsFlow.value.toMutableList()
            val existingIndex = current.indexOfFirst {
                it.id == budget.id || (it.category.equals(budget.category, ignoreCase = true) && it.subCategory.equals(budget.subCategory, ignoreCase = true))
            }
            val toSave = if (budget.id.isBlank()) budget.copy(id = UUID.randomUUID().toString()) else budget
            if (existingIndex >= 0) {
                current[existingIndex] = toSave
            } else {
                current.add(toSave)
            }
            saveLocalBudgets(current)

            firestore?.collection("budgets")
                ?.document(toSave.id)
                ?.set(BudgetEntity.fromDomain(toSave))
                ?.await()

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to save budget")
        }
    }

    suspend fun deleteBudget(id: String): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val current = _budgetsFlow.value.filterNot { it.id == id }
            saveLocalBudgets(current)

            firestore?.collection("budgets")
                ?.document(id)
                ?.delete()
                ?.await()

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to delete budget")
        }
    }
}
