package com.randallengineering.finances.ui.screens.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.randallengineering.finances.core.network.Resource
import com.randallengineering.finances.data.repository.BudgetRepository
import com.randallengineering.finances.data.repository.CategoryRepository
import com.randallengineering.finances.data.repository.RuleRepository
import com.randallengineering.finances.data.repository.TransactionRepository
import com.randallengineering.finances.domain.model.Budget
import com.randallengineering.finances.domain.model.BudgetCategoryType
import com.randallengineering.finances.domain.model.CategoryHierarchy
import com.randallengineering.finances.domain.model.Rule
import com.randallengineering.finances.domain.model.Transaction
import com.randallengineering.finances.domain.usecase.BudgetCalculationResult
import com.randallengineering.finances.domain.usecase.BudgetCalculatorUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BudgetsUiState(
    val calculationResult: BudgetCalculationResult? = null,
    val categories: List<CategoryHierarchy> = emptyList(),
    val rules: List<Rule> = emptyList(),
    val incomeCategory: String = "Income",
    val isAutoRunRulesEnabled: Boolean = true,
    val selectedTab: Int = 0, // 0 = Budgets & Pacing, 1 = Categories, 2 = Auto-Rules
    val isLoading: Boolean = false,
    val isCreatingBudget: Boolean = false,
    val editingBudget: Budget? = null,
    val isCreatingCategory: Boolean = false,
    val editingMainCategory: String? = null,
    val editingSubCategory: Pair<String, String>? = null, // Main -> Sub
    val selectedMainCategoryForSub: String? = null,
    val isCreatingRule: Boolean = false,
    val editingRule: Rule? = null,
    val isChangingIncomeCategory: Boolean = false,
    val ruleExecutionMessage: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@Suppress("UNCHECKED_CAST")
class BudgetsViewModel(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val ruleRepository: RuleRepository,
    private val budgetCalculatorUseCase: BudgetCalculatorUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetsUiState())
    val uiState: StateFlow<BudgetsUiState> = _uiState.asStateFlow()

    private var currentTransactions: List<Transaction> = emptyList()
    private var hasAutoRunExecuted = false

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                listOf(
                    budgetRepository.getBudgetsFlow(),
                    transactionRepository.getTransactionsFlow(),
                    categoryRepository.getCategoriesFlow(),
                    ruleRepository.getRulesFlow(),
                    categoryRepository.getIncomeCategoryFlow(),
                    ruleRepository.getAutoRunEnabledFlow()
                )
            ) { resources ->
                val budgetsRes = resources[0] as Resource<List<Budget>>
                val txRes = resources[1] as Resource<List<Transaction>>
                val catRes = resources[2] as Resource<List<CategoryHierarchy>>
                val rulesRes = resources[3] as Resource<List<Rule>>
                val incomeCategoryName = (resources[4] as? String) ?: "Income"
                val autoRunEnabled = (resources[5] as? Boolean) ?: true

                val isLoading = budgetsRes.isLoading || txRes.isLoading || catRes.isLoading || rulesRes.isLoading
                val budgets = budgetsRes.getOrNull().orEmpty()
                val transactions = txRes.getOrNull().orEmpty()
                val categories = catRes.getOrNull().orEmpty()
                val rawRules = rulesRes.getOrNull().orEmpty()

                currentTransactions = transactions

                // Calculate rule matches reactively from transactions
                val rulesWithMatchCount = rawRules.map { rule ->
                    val count = transactions.count { tx ->
                        rule.matches(tx.originalDesc, tx.amount)
                    }.toLong()
                    rule.copy(matchCount = count)
                }

                val result = budgetCalculatorUseCase.calculate(budgets, transactions, incomeCategory = incomeCategoryName)

                _uiState.update {
                    it.copy(
                        calculationResult = result,
                        categories = categories,
                        rules = rulesWithMatchCount,
                        incomeCategory = incomeCategoryName,
                        isAutoRunRulesEnabled = autoRunEnabled,
                        isLoading = isLoading
                    )
                }

                // Run rules automatically on first app load if enabled
                if (!hasAutoRunExecuted && autoRunEnabled && transactions.isNotEmpty() && rawRules.isNotEmpty()) {
                    hasAutoRunExecuted = true
                    runAllRules()
                }
            }.collect {}
        }
    }

    fun onTabSelect(tabIndex: Int) {
        _uiState.update { it.copy(selectedTab = tabIndex) }
    }

    fun toggleAutoRunRules(enabled: Boolean) {
        ruleRepository.setAutoRunEnabled(enabled)
    }

    fun setIncomeCategory(name: String) {
        categoryRepository.setIncomeCategory(name)
        _uiState.update { it.copy(incomeCategory = name, isChangingIncomeCategory = false) }
    }

    fun openChangeIncomeCategoryDialog() {
        _uiState.update { it.copy(isChangingIncomeCategory = true) }
    }

    fun openCreateBudgetDialog() {
        _uiState.update { it.copy(isCreatingBudget = true, editingBudget = null) }
    }

    fun openEditBudgetDialog(budget: Budget) {
        _uiState.update { it.copy(editingBudget = budget, isCreatingBudget = false) }
    }

    fun openCreateCategoryDialog() {
        _uiState.update { it.copy(isCreatingCategory = true) }
    }

    fun openEditMainCategoryDialog(mainCategory: String) {
        _uiState.update { it.copy(editingMainCategory = mainCategory) }
    }

    fun openEditSubCategoryDialog(mainCategory: String, subCategory: String) {
        _uiState.update { it.copy(editingSubCategory = Pair(mainCategory, subCategory)) }
    }

    fun openAddSubCategoryDialog(mainCategory: String) {
        _uiState.update { it.copy(selectedMainCategoryForSub = mainCategory) }
    }

    fun openCreateRuleDialog() {
        _uiState.update { it.copy(isCreatingRule = true, editingRule = null) }
    }

    fun openEditRuleDialog(rule: Rule) {
        _uiState.update { it.copy(editingRule = rule, isCreatingRule = false) }
    }

    fun closeDialogs() {
        _uiState.update {
            it.copy(
                isCreatingBudget = false,
                editingBudget = null,
                isCreatingCategory = false,
                editingMainCategory = null,
                editingSubCategory = null,
                selectedMainCategoryForSub = null,
                isCreatingRule = false,
                editingRule = null,
                isChangingIncomeCategory = false,
                ruleExecutionMessage = null
            )
        }
    }

    fun saveBudget(budget: Budget) {
        viewModelScope.launch {
            budgetRepository.saveBudget(budget)
            closeDialogs()
        }
    }

    fun deleteBudget(budgetId: String) {
        viewModelScope.launch {
            budgetRepository.deleteBudget(budgetId)
        }
    }

    fun addCategory(mainCategory: String, subCategory: String?) {
        viewModelScope.launch {
            categoryRepository.addOrUpdateCategory(mainCategory, subCategory)
            closeDialogs()
        }
    }

    fun renameMainCategory(oldMain: String, newMain: String) {
        viewModelScope.launch {
            categoryRepository.renameMainCategory(oldMain, newMain)
            closeDialogs()
        }
    }

    fun addSubCategory(mainCategory: String, subCategory: String) {
        viewModelScope.launch {
            categoryRepository.addSubCategory(mainCategory, subCategory)
            closeDialogs()
        }
    }

    fun renameSubCategory(mainCategory: String, oldSub: String, newSub: String) {
        viewModelScope.launch {
            categoryRepository.renameSubCategory(mainCategory, oldSub, newSub)
            closeDialogs()
        }
    }

    fun deleteSubCategory(mainCategory: String, subCategory: String) {
        viewModelScope.launch {
            categoryRepository.deleteSubCategory(mainCategory, subCategory)
        }
    }

    fun deleteMainCategory(mainCategory: String) {
        viewModelScope.launch {
            categoryRepository.deleteMainCategory(mainCategory)
        }
    }

    fun saveRule(rule: Rule) {
        viewModelScope.launch {
            ruleRepository.saveRule(rule)
            
            // Automatically execute this rule upon save!
            val (updatedTxs, count) = ruleRepository.applySingleRule(rule, currentTransactions)
            if (updatedTxs.isNotEmpty()) {
                transactionRepository.saveTransactionsBatch(updatedTxs)
            }
            _uiState.update { it.copy(ruleExecutionMessage = "Rule saved & applied to $count transactions!") }
            closeDialogs()
        }
    }

    fun deleteRule(ruleId: String) {
        viewModelScope.launch {
            ruleRepository.deleteRule(ruleId)
        }
    }

    fun runAllRules() {
        viewModelScope.launch {
            val rules = _uiState.value.rules
            val (updatedTxs, count) = ruleRepository.applyAllRules(rules, currentTransactions)
            if (updatedTxs.isNotEmpty()) {
                transactionRepository.saveTransactionsBatch(updatedTxs)
            }
            _uiState.update { it.copy(ruleExecutionMessage = "Auto-Rules applied to $count transactions!") }
        }
    }

    fun calculateMatchesForPattern(pattern: String, minAmount: Double?, maxAmount: Double?): Int {
        if (pattern.isBlank()) return 0
        val testRule = Rule(
            id = "test",
            name = "test",
            priority = 1,
            pattern = pattern,
            minAmount = minAmount,
            maxAmount = maxAmount,
            category = "test"
        )
        return currentTransactions.count { testRule.matches(it.originalDesc, it.amount) }
    }
}
