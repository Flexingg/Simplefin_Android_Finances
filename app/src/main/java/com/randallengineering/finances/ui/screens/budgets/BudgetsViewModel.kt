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
    val selectedTab: Int = 0, // 0 = Budgets & Pacing, 1 = Categories, 2 = Auto-Rules
    val isLoading: Boolean = false,
    val isCreatingBudget: Boolean = false,
    val editingBudget: Budget? = null,
    val isCreatingCategory: Boolean = false,
    val selectedMainCategoryForSub: String? = null,
    val isCreatingRule: Boolean = false,
    val editingRule: Rule? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class BudgetsViewModel(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val ruleRepository: RuleRepository,
    private val budgetCalculatorUseCase: BudgetCalculatorUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetsUiState())
    val uiState: StateFlow<BudgetsUiState> = _uiState.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                budgetRepository.getBudgetsFlow(),
                transactionRepository.getTransactionsFlow(),
                categoryRepository.getCategoriesFlow(),
                ruleRepository.getRulesFlow()
            ) { budgetsRes, txRes, catRes, rulesRes ->
                listOf(budgetsRes, txRes, catRes, rulesRes)
            }.collect { resources ->
                val budgetsRes = resources[0] as Resource<List<Budget>>
                val txRes = resources[1] as Resource<List<Transaction>>
                val catRes = resources[2] as Resource<List<CategoryHierarchy>>
                val rulesRes = resources[3] as Resource<List<Rule>>

                val isLoading = budgetsRes.isLoading || txRes.isLoading || catRes.isLoading || rulesRes.isLoading
                val budgets = budgetsRes.getOrNull().orEmpty()
                val transactions = txRes.getOrNull().orEmpty()
                val categories = catRes.getOrNull().orEmpty()
                val rawRules = rulesRes.getOrNull().orEmpty()

                // Calculate rule matches reactively from transactions
                val rulesWithMatchCount = rawRules.map { rule ->
                    val count = transactions.count { tx ->
                        rule.matches(tx.originalDesc, tx.amount)
                    }.toLong()
                    rule.copy(matchCount = count)
                }

                val result = budgetCalculatorUseCase.calculate(budgets, transactions)

                _uiState.update {
                    it.copy(
                        calculationResult = result,
                        categories = categories,
                        rules = rulesWithMatchCount,
                        isLoading = isLoading
                    )
                }
            }
        }
    }

    fun onTabSelect(tabIndex: Int) {
        _uiState.update { it.copy(selectedTab = tabIndex) }
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
                editingBudget = null,
                isCreatingBudget = false,
                isCreatingCategory = false,
                selectedMainCategoryForSub = null,
                isCreatingRule = false,
                editingRule = null
            )
        }
    }

    fun saveBudget(budget: Budget) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            categoryRepository.addOrUpdateCategory(budget.category)

            when (val result = budgetRepository.saveBudget(budget)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            editingBudget = null,
                            isCreatingBudget = false,
                            successMessage = "Budget for '${budget.category}' saved successfully"
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun deleteBudget(budgetId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = budgetRepository.deleteBudget(budgetId)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isLoading = false, successMessage = "Budget removed") }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun addCategory(mainCategory: String, subCategory: String? = null) {
        categoryRepository.addOrUpdateCategory(mainCategory, subCategory)
        closeDialogs()
        _uiState.update { it.copy(successMessage = "Category '$mainCategory' added successfully") }
    }

    fun addSubCategory(mainCategory: String, subCategory: String) {
        categoryRepository.addSubCategory(mainCategory, subCategory)
        closeDialogs()
        _uiState.update { it.copy(successMessage = "Added '$subCategory' to $mainCategory") }
    }

    fun deleteCategory(mainCategory: String) {
        categoryRepository.deleteMainCategory(mainCategory)
        _uiState.update { it.copy(successMessage = "Category '$mainCategory' deleted") }
    }

    fun deleteSubCategory(mainCategory: String, subCategory: String) {
        categoryRepository.deleteSubCategory(mainCategory, subCategory)
    }

    fun saveRule(rule: Rule) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            categoryRepository.addOrUpdateCategory(rule.category, rule.subCategory.ifBlank { null })

            when (val result = ruleRepository.saveRule(rule)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            editingRule = null,
                            isCreatingRule = false,
                            successMessage = "Rule '${rule.name}' saved"
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun deleteRule(ruleId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = ruleRepository.deleteRule(ruleId)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isLoading = false, successMessage = "Rule deleted") }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun moveRuleUp(index: Int) {
        if (index <= 0) return
        val list = _uiState.value.rules.toMutableList()
        val temp = list[index]
        list[index] = list[index - 1]
        list[index - 1] = temp
        updatePriorities(list)
    }

    fun moveRuleDown(index: Int) {
        val list = _uiState.value.rules.toMutableList()
        if (index >= list.size - 1) return
        val temp = list[index]
        list[index] = list[index + 1]
        list[index + 1] = temp
        updatePriorities(list)
    }

    private fun updatePriorities(reordered: List<Rule>) {
        val updated = reordered.mapIndexed { idx, rule -> rule.copy(priority = idx + 1) }
        _uiState.update { it.copy(rules = updated) }
        viewModelScope.launch {
            ruleRepository.updateRulesPriority(updated)
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
