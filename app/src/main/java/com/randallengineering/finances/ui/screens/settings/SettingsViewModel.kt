package com.randallengineering.finances.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.randallengineering.finances.core.auth.SessionManager
import com.randallengineering.finances.core.network.Resource
import com.randallengineering.finances.core.security.BiometricAuthManager
import com.randallengineering.finances.core.util.CsvExporter
import com.randallengineering.finances.data.model.SimpleFinConfigEntity
import com.randallengineering.finances.data.repository.AmazonRepository
import com.randallengineering.finances.data.repository.SimpleFinRepository
import com.randallengineering.finances.data.repository.TransactionRepository
import com.randallengineering.finances.domain.usecase.SimpleFinSyncUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val tokenInput: String = "",
    val isConnected: Boolean = false,
    val config: SimpleFinConfigEntity? = null,
    val syncDaysBack: Int = 90,
    val isClaimingToken: Boolean = false,
    val isSyncing: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val isBiometricAvailable: Boolean = false,
    val accountEmail: String? = null,
    val accountDisplayName: String? = null,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val errList: List<String> = emptyList()
)

class SettingsViewModel(
    private val simpleFinSyncUseCase: SimpleFinSyncUseCase,
    private val simpleFinRepository: SimpleFinRepository,
    private val amazonRepository: AmazonRepository,
    private val sessionManager: SessionManager,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        observeConfig()
        _uiState.update {
            it.copy(
                accountEmail = sessionManager.email,
                accountDisplayName = sessionManager.displayName
            )
        }
    }

    fun signOut() {
        viewModelScope.launch {
            sessionManager.signOut()
            _uiState.update { it.copy(accountEmail = null, accountDisplayName = null) }
        }
    }

    /** Drive-scoped sign-in intent, launched only when a Drive backup needs consent. */
    fun driveSignInIntent(): android.content.Intent = sessionManager.driveSignInClient.signInIntent

    /** Build a CSV of the user's real transactions for export/backup. */
    suspend fun buildBackupCsv(): String {
        val txs = transactionRepository.getTransactionsFlow().first().getOrNull().orEmpty()
        return CsvExporter.toCsv(txs)
    }

    fun initSecurityState(context: Context) {
        val enabled = BiometricAuthManager.isBiometricEnabled(context)
        val available = BiometricAuthManager.canAuthenticate(context)
        _uiState.update { it.copy(isBiometricEnabled = enabled, isBiometricAvailable = available) }
    }

    fun setBiometricEnabled(context: Context, enabled: Boolean) {
        BiometricAuthManager.setBiometricEnabled(context, enabled)
        _uiState.update {
            it.copy(
                isBiometricEnabled = enabled,
                successMessage = if (enabled) "Biometric Lock enabled!" else "Biometric Lock disabled."
            )
        }
    }

    private fun observeConfig() {
        viewModelScope.launch {
            simpleFinRepository.getConfigFlow().collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        val config = resource.data
                        _uiState.update {
                            it.copy(
                                isConnected = config?.accessUrlConfigured == true,
                                config = config,
                                errList = config?.errorList.orEmpty()
                            )
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    fun onTokenInputChange(token: String) {
        _uiState.update { it.copy(tokenInput = token, errorMessage = null) }
    }

    fun onSyncDaysBackChange(days: Int) {
        _uiState.update { it.copy(syncDaysBack = days.coerceIn(1, 1000)) }
    }

    fun claimToken() {
        val token = _uiState.value.tokenInput.trim()
        if (token.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid SimpleFIN setup token.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isClaimingToken = true, errorMessage = null, successMessage = null) }
            when (val result = simpleFinRepository.claimSetupToken(token)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isClaimingToken = false,
                            tokenInput = "",
                            successMessage = result.data,
                            isConnected = true
                        )
                    }
                    triggerSync()
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isClaimingToken = false,
                            errorMessage = result.message
                        )
                    }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun triggerSync() {
        val days = _uiState.value.syncDaysBack
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, errorMessage = null, successMessage = null) }
            when (val result = simpleFinRepository.triggerSync(days)) {
                is Resource.Success -> {
                    val errors = result.data.orEmpty()
                    _uiState.update {
                        it.copy(
                            isSyncing = false,
                            errList = errors,
                            successMessage = if (errors.isEmpty()) {
                                "Synced transactions for past $days days in 89-day batch windows!"
                            } else {
                                "Synced transactions (with ${errors.size} notices from institution)."
                            }
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isSyncing = false,
                            errorMessage = result.message
                        )
                    }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun openAmazonOrderHistory(context: Context) {
        amazonRepository.openOrderHistory(context)
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
