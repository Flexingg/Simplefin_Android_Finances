package com.randallengineering.finances.ui.screens.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.randallengineering.finances.core.network.Resource
import com.randallengineering.finances.data.repository.BudgetRepository
import com.randallengineering.finances.data.repository.GoalRepository
import com.randallengineering.finances.data.repository.TransactionRepository
import com.randallengineering.finances.domain.model.AiInsight
import com.randallengineering.finances.domain.model.AiSnapshot
import com.randallengineering.finances.domain.usecase.AiAdvisorUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AiAdvisorUiState(
    val snapshot: AiSnapshot? = null,
    val ruleBasedInsights: List<AiInsight> = emptyList(),
    val geminiAnalysisMarkdown: String? = null,
    val isGeneratingGemini: Boolean = false,
    val isSnapshotSheetOpen: Boolean = false,
    val isLoadingData: Boolean = false,
    val errorMessage: String? = null
)

class AiAdvisorViewModel(
    private val aiAdvisorUseCase: AiAdvisorUseCase,
    private val budgetRepository: BudgetRepository,
    private val goalRepository: GoalRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiAdvisorUiState())
    val uiState: StateFlow<AiAdvisorUiState> = _uiState.asStateFlow()

    init {
        observeDataAndCompileSnapshot()
    }

    private fun observeDataAndCompileSnapshot() {
        viewModelScope.launch {
            combine(
                budgetRepository.getBudgetsFlow(),
                goalRepository.getGoalsFlow(),
                transactionRepository.getTransactionsFlow()
            ) { budgetsRes, goalsRes, txRes ->
                Triple(budgetsRes, goalsRes, txRes)
            }.collect { (budgetsRes, goalsRes, txRes) ->
                val isLoading = budgetsRes.isLoading || goalsRes.isLoading || txRes.isLoading
                val budgets = budgetsRes.getOrNull().orEmpty()
                val goals = goalsRes.getOrNull().orEmpty()
                val txs = txRes.getOrNull().orEmpty()

                val snapshot = aiAdvisorUseCase.compileAiSnapshot(
                    accounts = emptyList(), // Accounts from simplefin or mock
                    budgets = budgets,
                    goals = goals,
                    transactions = txs
                )

                val ruleInsights = aiAdvisorUseCase.extractRuleBasedInsights(snapshot)

                _uiState.update {
                    it.copy(
                        snapshot = snapshot,
                        ruleBasedInsights = ruleInsights,
                        isLoadingData = isLoading
                    )
                }
            }
        }
    }

    fun generateGeminiAnalysis() {
        val snapshot = _uiState.value.snapshot ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingGemini = true, errorMessage = null) }
            when (val result = aiAdvisorUseCase.generateGeminiInsights(snapshot)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isGeneratingGemini = false,
                            geminiAnalysisMarkdown = result.data
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isGeneratingGemini = false,
                            errorMessage = result.message
                        )
                    }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun openSnapshotSheet() {
        _uiState.update { it.copy(isSnapshotSheetOpen = true) }
    }

    fun closeSnapshotSheet() {
        _uiState.update { it.copy(isSnapshotSheetOpen = false) }
    }
}
