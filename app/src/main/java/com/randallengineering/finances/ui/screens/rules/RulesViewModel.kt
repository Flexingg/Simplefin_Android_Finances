package com.randallengineering.finances.ui.screens.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.randallengineering.finances.core.network.Resource
import com.randallengineering.finances.data.repository.RuleRepository
import com.randallengineering.finances.data.repository.TransactionRepository
import com.randallengineering.finances.domain.model.Rule
import com.randallengineering.finances.domain.model.Transaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RulesUiState(
    val rules: List<Rule> = emptyList(),
    val ruleMatches: Map<String, List<Transaction>> = emptyMap(),
    val isLoading: Boolean = false,
    val editingRule: Rule? = null,
    val isCreatingNewRule: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class RulesViewModel(
    private val ruleRepository: RuleRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RulesUiState())
    val uiState: StateFlow<RulesUiState> = _uiState.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                ruleRepository.getRulesFlow(),
                transactionRepository.getTransactionsFlow()
            ) { rulesResource, txResource ->
                Pair(rulesResource, txResource)
            }.collect { (rulesResource, txResource) ->
                val isLoading = rulesResource.isLoading || txResource.isLoading
                val rawRules = rulesResource.getOrNull().orEmpty()
                val transactions = txResource.getOrNull().orEmpty()

                // Calculate match count accurately and dynamically from actual transactions
                val rulesWithMatchCount = rawRules.map { rule ->
                    val count = transactions.count { tx ->
                        rule.matches(tx.originalDesc, tx.amount)
                    }.toLong()
                    rule.copy(matchCount = count)
                }

                // Drill-down: which real transactions each rule would match / already applied to.
                val ruleMatches = rawRules.associate { rule ->
                    rule.id to transactions
                        .filter { tx -> tx.matchedRuleId == rule.id || rule.matches(tx.originalDesc, tx.amount) }
                        .sortedByDescending { it.postedEpochSeconds }
                        .take(50)
                }

                _uiState.update {
                    it.copy(
                        rules = rulesWithMatchCount,
                        ruleMatches = ruleMatches,
                        isLoading = isLoading
                    )
                }
            }
        }
    }

    fun openCreateRuleDialog() {
        _uiState.update { it.copy(isCreatingNewRule = true, editingRule = null) }
    }

    fun openEditRuleDialog(rule: Rule) {
        _uiState.update { it.copy(editingRule = rule, isCreatingNewRule = false) }
    }

    fun closeDialog() {
        _uiState.update { it.copy(editingRule = null, isCreatingNewRule = false) }
    }

    fun saveRule(rule: Rule) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = ruleRepository.saveRule(rule)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            editingRule = null,
                            isCreatingNewRule = false,
                            successMessage = "Rule saved successfully"
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
