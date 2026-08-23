package com.randallengineering.finances.ui.screens.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.randallengineering.finances.core.network.Resource
import com.randallengineering.finances.data.model.SimpleFinConfigEntity
import com.randallengineering.finances.data.repository.AmazonRepository
import com.randallengineering.finances.data.repository.SimpleFinRepository
import com.randallengineering.finances.domain.usecase.SimpleFinSyncUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val tokenInput: String = "",
    val isConnected: Boolean = false,
    val config: SimpleFinConfigEntity? = null,
    val syncDaysBack: Int = 90,
    val isClaimingToken: Boolean = false,
    val isSyncing: Boolean = false,
    val isAmazonConnected: Boolean = false,
    val amazonUserEmail: String = "",
    val amazonUserName: String = "",
    val amazonClientIdInput: String = "",
    val amazonClientSecretInput: String = "",
    val importedOrdersCount: Int = 0,
    val isImportingCsv: Boolean = false,
    val isFetchingAmazonAuthUrl: Boolean = false,
    val authUrlForSheet: String? = null,
    val isExchangingToken: Boolean = false,
    val rawApiDataToDisplay: String? = null,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val errList: List<String> = emptyList()
)

class SettingsViewModel(
    private val simpleFinSyncUseCase: SimpleFinSyncUseCase,
    private val simpleFinRepository: SimpleFinRepository,
    private val amazonRepository: AmazonRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        observeConfig()
        observeAmazonStatus()
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

    private fun observeAmazonStatus() {
        viewModelScope.launch {
            amazonRepository.isConnectedFlow().collect { connected ->
                _uiState.update { it.copy(isAmazonConnected = connected) }
            }
        }
        viewModelScope.launch {
            amazonRepository.getUserEmailFlow().collect { email ->
                _uiState.update { it.copy(amazonUserEmail = email) }
            }
        }
        viewModelScope.launch {
            amazonRepository.getUserNameFlow().collect { name ->
                _uiState.update { it.copy(amazonUserName = name) }
            }
        }
        viewModelScope.launch {
            amazonRepository.getClientIdFlow().collect { cId ->
                _uiState.update { it.copy(amazonClientIdInput = cId) }
            }
        }
        viewModelScope.launch {
            amazonRepository.getClientSecretFlow().collect { cSec ->
                _uiState.update { it.copy(amazonClientSecretInput = cSec) }
            }
        }
        viewModelScope.launch {
            amazonRepository.getImportedOrdersCountFlow().collect { count ->
                _uiState.update { it.copy(importedOrdersCount = count) }
            }
        }
    }

    fun onTokenInputChange(token: String) {
        _uiState.update { it.copy(tokenInput = token, errorMessage = null) }
    }

    fun onAmazonClientIdChange(clientId: String) {
        _uiState.update { it.copy(amazonClientIdInput = clientId, errorMessage = null) }
    }

    fun onAmazonClientSecretChange(secret: String) {
        _uiState.update { it.copy(amazonClientSecretInput = secret, errorMessage = null) }
    }

    fun saveAmazonCredentials() {
        amazonRepository.saveCredentials(
            clientId = _uiState.value.amazonClientIdInput,
            clientSecret = _uiState.value.amazonClientSecretInput
        )
        _uiState.update { it.copy(successMessage = "Amazon Client ID & Secret saved!") }
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

    // Amazon Login with Amazon (LWA) In-App OAuth Flow
    fun startAmazonConnect() {
        val clientId = _uiState.value.amazonClientIdInput.trim()
        val clientSecret = _uiState.value.amazonClientSecretInput.trim()

        if (clientId.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your Amazon LWA Client ID first.") }
            return
        }

        amazonRepository.saveCredentials(clientId, clientSecret)

        viewModelScope.launch {
            _uiState.update { it.copy(isFetchingAmazonAuthUrl = true, errorMessage = null) }
            when (val result = amazonRepository.getAmazonOAuthUrl()) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isFetchingAmazonAuthUrl = false,
                            authUrlForSheet = result.data
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isFetchingAmazonAuthUrl = false,
                            errorMessage = result.message
                        )
                    }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun onOAuthCodeReceived(authCode: String) {
        _uiState.update { it.copy(authUrlForSheet = null, isExchangingToken = true) }
        viewModelScope.launch {
            when (val result = amazonRepository.exchangeOAuthCode(authCode)) {
                is Resource.Success -> {
                    val email = amazonRepository.getUserEmail()
                    val msg = if (email.isNotBlank()) "✅ Linked Amazon: $email" else "✅ Amazon Account Connected Successfully!"
                    _uiState.update {
                        it.copy(
                            isExchangingToken = false,
                            isAmazonConnected = true,
                            amazonUserEmail = email,
                            amazonUserName = amazonRepository.getUserName(),
                            successMessage = msg
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isExchangingToken = false,
                            errorMessage = "Amazon Token Exchange: ${result.message}"
                        )
                    }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun viewRawApiData() {
        val data = amazonRepository.getRawApiData()
        _uiState.update { it.copy(rawApiDataToDisplay = data) }
    }

    fun closeRawApiDataDialog() {
        _uiState.update { it.copy(rawApiDataToDisplay = null) }
    }

    fun importAmazonCsv(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImportingCsv = true, errorMessage = null, successMessage = null) }
            when (val result = amazonRepository.importOrdersFromCsv(uri)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isImportingCsv = false,
                            successMessage = "Successfully imported ${result.data} real Amazon order items!"
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isImportingCsv = false,
                            errorMessage = result.message
                        )
                    }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun clearImportedOrders() {
        amazonRepository.clearImportedOrders()
        _uiState.update { it.copy(successMessage = "Imported orders cache cleared.") }
    }

    fun closeOAuthSheet() {
        _uiState.update { it.copy(authUrlForSheet = null) }
    }

    fun disconnectAmazon() {
        amazonRepository.setConnected(false)
        _uiState.update {
            it.copy(
                isAmazonConnected = false,
                amazonUserEmail = "",
                amazonUserName = "",
                successMessage = "Amazon account disconnected."
            )
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
