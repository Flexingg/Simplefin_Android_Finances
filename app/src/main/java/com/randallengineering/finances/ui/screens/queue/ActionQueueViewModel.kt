package com.randallengineering.finances.ui.screens.queue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.randallengineering.finances.core.network.Resource
import com.randallengineering.finances.data.repository.CategoryRepository
import com.randallengineering.finances.data.repository.GamificationRepository
import com.randallengineering.finances.data.repository.TransactionRepository
import com.randallengineering.finances.domain.model.CategoryHierarchy
import com.randallengineering.finances.domain.model.Transaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ActionQueueUiState(
    val pendingTransactions: List<Transaction> = emptyList(),
    val currentCardIndex: Int = 0,
    val comboMultiplier: Int = 1,
    val totalXpEarnedInSession: Int = 0,
    val isSessionComplete: Boolean = false,
    val availableCategories: List<CategoryHierarchy> = emptyList()
) {
    val currentTransaction: Transaction?
        get() = pendingTransactions.getOrNull(currentCardIndex)
}

class ActionQueueViewModel(
    private val transactionRepository: TransactionRepository,
    private val gamificationRepository: GamificationRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _currentIndex = MutableStateFlow(0)
    private val _combo = MutableStateFlow(1)
    private val _sessionXp = MutableStateFlow(0)

    val uiState: StateFlow<ActionQueueUiState> = combine(
        transactionRepository.getTransactionsFlow(),
        categoryRepository.getCategoriesFlow(),
        _currentIndex,
        _combo,
        _sessionXp
    ) { txResource, catResource, index, combo, xpEarned ->
        val allTxs = (txResource as? Resource.Success)?.data.orEmpty()
        val customCats = (catResource as? Resource.Success)?.data.orEmpty()

        // Extract all distinct categories from transactions in database
        val txCategories = allTxs
            .map { it.category.trim() }
            .filter { it.isNotBlank() && !it.equals("Uncategorized", ignoreCase = true) }
            .distinct()

        // Build combined category list
        val customMainNames = customCats.map { it.mainCategory.lowercase() }.toSet()
        val extraFromTxs = txCategories.filter { it.lowercase() !in customMainNames }.map { CategoryHierarchy(mainCategory = it) }
        
        val defaultFallback = if (customCats.isEmpty() && extraFromTxs.isEmpty()) {
            listOf(
                CategoryHierarchy("Dining", listOf("Restaurants", "Fast Food", "Coffee")),
                CategoryHierarchy("Groceries", listOf("Supermarket", "Pantry")),
                CategoryHierarchy("Automotive", listOf("Gas", "Maintenance")),
                CategoryHierarchy("Utilities", listOf("Electric", "Internet", "Water")),
                CategoryHierarchy("Shopping", listOf("Clothing", "Electronics")),
                CategoryHierarchy("Entertainment", listOf("Movies", "Games")),
                CategoryHierarchy("Health & Medical", listOf("Pharmacy", "Doctor")),
                CategoryHierarchy("Income", listOf("Salary", "Deposit"))
            )
        } else {
            emptyList()
        }

        val allCategories = (customCats + extraFromTxs + defaultFallback).distinctBy { it.mainCategory.lowercase() }

        val pending = if (allTxs.isNotEmpty()) allTxs.take(20) else generateSampleQueue()
        val isComplete = index >= pending.size

        ActionQueueUiState(
            pendingTransactions = pending,
            currentCardIndex = index,
            comboMultiplier = combo,
            totalXpEarnedInSession = xpEarned,
            isSessionComplete = isComplete,
            availableCategories = allCategories
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ActionQueueUiState()
    )

    fun confirmCategory(tx: Transaction) {
        viewModelScope.launch {
            val combo = _combo.value
            val gainedXp = gamificationRepository.addXp(15 * combo, tx.category)
            _sessionXp.value += gainedXp
            _combo.value = (combo + 1).coerceAtMost(5)
            _currentIndex.value += 1
        }
    }

    fun editCategory(tx: Transaction, newCategory: String, newSubCategory: String) {
        viewModelScope.launch {
            val updated = tx.copy(category = newCategory, subCategory = newSubCategory)
            transactionRepository.saveTransaction(updated)
            val gainedXp = gamificationRepository.addXp(10, newCategory)
            _sessionXp.value += gainedXp
            _currentIndex.value += 1
        }
    }

    fun addNewCategory(mainCategory: String, subCategory: String?) {
        viewModelScope.launch {
            categoryRepository.addOrUpdateCategory(mainCategory, subCategory)
        }
    }

    fun resetSession() {
        _currentIndex.value = 0
        _combo.value = 1
        _sessionXp.value = 0
    }

    private fun generateSampleQueue(): List<Transaction> {
        val now = System.currentTimeMillis() / 1000
        return listOf(
            Transaction(id = "tx_sample_1", originalDesc = "Trader Joe's Grocery", payee = "Trader Joe's", amount = -42.50, category = "Groceries", postedEpochSeconds = now),
            Transaction(id = "tx_sample_2", originalDesc = "Chipotle Mexican Grill", payee = "Chipotle", amount = -14.85, category = "Dining", postedEpochSeconds = now - 86400),
            Transaction(id = "tx_sample_3", originalDesc = "Chevron Gas Station", payee = "Chevron", amount = -38.20, category = "Automotive", postedEpochSeconds = now - 172800),
            Transaction(id = "tx_sample_4", originalDesc = "Netflix Monthly Subscription", payee = "Netflix", amount = -15.99, category = "Subscriptions", postedEpochSeconds = now - 259200)
        )
    }
}
