package com.randallengineering.finances.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.randallengineering.finances.core.auth.SessionManager
import com.randallengineering.finances.core.network.Resource
import com.randallengineering.finances.core.security.BiometricAuthManager
import com.randallengineering.finances.core.util.CsvExporter
import com.randallengineering.finances.core.util.CsvImporter
import com.randallengineering.finances.data.model.SimpleFinConfigEntity
import com.randallengineering.finances.data.repository.AmazonRepository
import com.randallengineering.finances.data.repository.DiscretionaryRepository
import com.randallengineering.finances.data.repository.NotificationPrefsRepository
import com.randallengineering.finances.data.repository.SimpleFinRepository
import com.randallengineering.finances.data.repository.SyncStatusRepository
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
    val errList: List<String> = emptyList(),
    val lastSync: SyncStatusRepository.SyncStatus? = null,
    val budgetAlertsEnabled: Boolean = false,
    val reviewAlertsEnabled: Boolean = false,
    val isImporting: Boolean = false,
    val discretionarySetpoint: Double = 0.0,
    val discretionarySpent: Double = 0.0,
    val discretionaryRemaining: Double = 0.0,
    val discretionaryCategories: List<String> = emptyList(),
    val necessaryCategories: Set<String> = emptySet()
)

class SettingsViewModel(
    private val simpleFinSyncUseCase: SimpleFinSyncUseCase,
    private val simpleFinRepository: SimpleFinRepository,
    private val amazonRepository: AmazonRepository,
    private val sessionManager: SessionManager,
    private val transactionRepository: TransactionRepository,
    private val syncStatusRepository: SyncStatusRepository,
    private val notificationPrefsRepository: NotificationPrefsRepository,
    private val discretionaryRepository: DiscretionaryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        observeConfig()
        observeSyncStatus()
        observeNotificationPrefs()
        observeDiscretionary()
        _uiState.update {
            it.copy(
                accountEmail = sessionManager.email,
                accountDisplayName = sessionManager.displayName
            )
        }
    }

    private fun observeDiscretionary() {
        viewModelScope.launch {
            discretionaryRepository.state.collect { s ->
                _uiState.update {
                    it.copy(
                        discretionarySetpoint = s.config.setpoint,
                        discretionarySpent = s.monthlySpend,
                        discretionaryRemaining = s.remaining,
                        discretionaryCategories = s.toggleCategories,
                        necessaryCategories = s.config.necessaryCategories.toSet()
                    )
                }
            }
        }
    }

    fun setDiscretionarySetpoint(setpoint: Double) {
        discretionaryRepository.setSetpoint(setpoint)
    }

    fun setCategoryNecessary(category: String, necessary: Boolean) {
        discretionaryRepository.setCategoryNecessary(category, necessary)
    }

    private fun observeNotificationPrefs() {
        viewModelScope.launch {
            notificationPrefsRepository.budgetAlerts.collect { on ->
                _uiState.update { it.copy(budgetAlertsEnabled = on) }
            }
        }
        viewModelScope.launch {
            notificationPrefsRepository.reviewAlerts.collect { on ->
                _uiState.update { it.copy(reviewAlertsEnabled = on) }
            }
        }
    }

    /** Opt-in toggles (default off). Called only after the user confirms + grants permission. */
    fun setBudgetAlerts(enabled: Boolean) = notificationPrefsRepository.setBudgetAlerts(enabled)
    fun setReviewAlerts(enabled: Boolean) = notificationPrefsRepository.setReviewAlerts(enabled)

    private fun observeSyncStatus() {
        viewModelScope.launch {
            syncStatusRepository.flow.collect { status ->
                _uiState.update { it.copy(lastSync = status) }
            }
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

    /** Parse a CSV the user picked and import its real transactions (no fabrication). */
    fun importCsv(text: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, errorMessage = null, successMessage = null) }
            val result = CsvImporter.importTransactions(text)
            if (result.transactions.isNotEmpty()) {
                transactionRepository.saveTransactions(result.transactions)
            }
            _uiState.update {
                it.copy(
                    isImporting = false,
                    successMessage = if (result.imported > 0) {
                        val skipText = if (result.skipped > 0) " (skipped ${result.skipped} unparseable rows)" else ""
                        "Imported ${result.imported} transaction${if (result.imported == 1) "" else "s"}$skipText"
                    } else {
                        "No transactions imported" + (result.problems.firstOrNull()?.let { " — $it" } ?: "")
                    }
                )
            }
        }
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
                    syncStatusRepository.recordSuccess()
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
                    syncStatusRepository.recordFailure(result.message)
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
