package com.randallengineering.finances.ui.screens.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.randallengineering.finances.core.network.Resource
import com.randallengineering.finances.data.repository.GoalRepository
import com.randallengineering.finances.domain.model.Goal
import com.randallengineering.finances.domain.usecase.GoalPacingResult
import com.randallengineering.finances.domain.usecase.GoalPacingUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GoalsUiState(
    val goals: List<GoalPacingResult> = emptyList(),
    val isLoading: Boolean = false,
    val isCreatingGoal: Boolean = false,
    val editingGoal: Goal? = null,
    val contributingGoal: Goal? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class GoalsViewModel(
    private val goalRepository: GoalRepository,
    private val goalPacingUseCase: GoalPacingUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoalsUiState())
    val uiState: StateFlow<GoalsUiState> = _uiState.asStateFlow()

    init {
        observeGoals()
    }

    private fun observeGoals() {
        viewModelScope.launch {
            goalRepository.getGoalsFlow().collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        val goalsList = resource.data
                        val pacingResults = goalPacingUseCase.calculateBatch(goalsList)
                        _uiState.update {
                            it.copy(
                                goals = pacingResults,
                                isLoading = false
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = resource.message
                            )
                        }
                    }
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                }
            }
        }
    }

    fun openCreateGoalDialog() {
        _uiState.update { it.copy(isCreatingGoal = true, editingGoal = null) }
    }

    fun openEditGoalDialog(goal: Goal) {
        _uiState.update { it.copy(editingGoal = goal, isCreatingGoal = false) }
    }

    fun openContributeDialog(goal: Goal) {
        _uiState.update { it.copy(contributingGoal = goal) }
    }

    fun closeDialog() {
        _uiState.update { it.copy(editingGoal = null, isCreatingGoal = false, contributingGoal = null) }
    }

    fun saveGoal(goal: Goal) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = goalRepository.saveGoal(goal)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            editingGoal = null,
                            isCreatingGoal = false,
                            successMessage = "Goal saved successfully"
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

    fun contributeToGoal(goal: Goal, addAmount: Double) {
        val updated = goal.copy(
            currentAmount = goal.currentAmount + addAmount,
            isCompleted = (goal.currentAmount + addAmount) >= goal.targetAmount
        )
        saveGoal(updated)
    }

    fun deleteGoal(goalId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = goalRepository.deleteGoal(goalId)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isLoading = false, successMessage = "Goal deleted") }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
                is Resource.Loading -> Unit
            }
        }
    }
}
