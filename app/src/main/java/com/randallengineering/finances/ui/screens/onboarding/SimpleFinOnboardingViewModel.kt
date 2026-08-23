package com.randallengineering.finances.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.randallengineering.finances.core.network.Resource
import com.randallengineering.finances.data.repository.SimpleFinRepository
import com.randallengineering.finances.domain.usecase.SimpleFinSyncUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val tokenInput: String = "",
    val isLoading: Boolean = false,
    val isConnected: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val errList: List<String> = emptyList()
)

class SimpleFinOnboardingViewModel(
    private val simpleFinSyncUseCase: SimpleFinSyncUseCase,
    private val simpleFinRepository: SimpleFinRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        observeConnectionStatus()
    }

    private fun observeConnectionStatus() {
        viewModelScope.launch {
            simpleFinRepository.getConfigFlow().collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        val config = resource.data
                        _uiState.update {
                            it.copy(
                                isConnected = config?.accessUrlConfigured == true,
                                errList = config?.errorList.orEmpty()
                            )
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    fun onTokenChange(token: String) {
        _uiState.update { it.copy(tokenInput = token, errorMessage = null) }
    }

    fun claimToken() {
        val token = _uiState.value.tokenInput.trim()
        if (token.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid Base64 SimpleFIN Setup Token.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            
            when (val result = simpleFinSyncUseCase.claimToken(token)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isConnected = true,
                            successMessage = result.data,
                            errorMessage = null
                        )
                    }
                    // Trigger first sync
                    syncNow()
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message
                        )
                    }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = simpleFinSyncUseCase.syncNow()) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errList = result.data,
                            successMessage = "Accounts and transactions synchronized successfully!"
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message
                        )
                    }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null, errList = emptyList()) }
    }
}
