package com.randallengineering.finances.ui.screens.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.randallengineering.finances.data.repository.AiConfigRepository
import com.randallengineering.finances.data.repository.AiProviderMode
import com.randallengineering.finances.data.repository.BudgetRepository
import com.randallengineering.finances.data.repository.GoalRepository
import com.randallengineering.finances.data.repository.TransactionRepository
import com.randallengineering.finances.domain.model.AiInsight
import com.randallengineering.finances.domain.model.AiSnapshot
import com.randallengineering.finances.domain.usecase.AiAdvisorUseCase
import com.randallengineering.finances.domain.usecase.AiChatbotUseCase
import com.randallengineering.finances.domain.usecase.ChatMessage
import com.randallengineering.finances.domain.usecase.MessageSender
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AiAdvisorUiState(
    val messages: List<ChatMessage> = listOf(
        ChatMessage(
            sender = MessageSender.ASSISTANT,
            text = "👋 Greetings! I am your AI Financial Controller & CFP. I have full real-time access to your SimpleFIN accounts, transactions, envelope budgets, and retirement calculations via MCP tools.\n\nWhat can I calculate or manage for you today?",
            suggestedActions = listOf(
                "🏖️ Simulate retirement at age 62",
                "💵 What is my daily safe allowance?",
                "🏷️ Review last 10 transactions",
                "💳 Compare Snowball vs Avalanche debt payoff",
                "📊 Show my budgets & rollover"
            )
        )
    ),
    val inputText: String = "",
    val isProcessing: Boolean = false,
    val apiKey: String = "",
    val isApiKeyConfigured: Boolean = false,
    val providerMode: AiProviderMode = AiProviderMode.CUSTOM_KEY,
    val showApiKeyDialog: Boolean = false,
    val apiKeyInput: String = "",
    val snapshot: AiSnapshot? = null,
    val isSnapshotSheetOpen: Boolean = false,
    val isLoadingData: Boolean = false,
    val errorMessage: String? = null
)

class AiAdvisorViewModel(
    private val aiAdvisorUseCase: AiAdvisorUseCase,
    private val aiChatbotUseCase: AiChatbotUseCase,
    private val aiConfigRepository: AiConfigRepository,
    private val budgetRepository: BudgetRepository,
    private val goalRepository: GoalRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiAdvisorUiState())
    val uiState: StateFlow<AiAdvisorUiState> = _uiState.asStateFlow()

    init {
        observeAiConfig()
        observeDataAndCompileSnapshot()
    }

    private fun observeAiConfig() {
        viewModelScope.launch {
            aiConfigRepository.configFlow.collect { config ->
                _uiState.update {
                    it.copy(
                        apiKey = config.apiKey,
                        isApiKeyConfigured = config.isKeyConfigured,
                        providerMode = config.providerMode,
                        apiKeyInput = config.apiKey
                    )
                }
            }
        }
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
                    accounts = emptyList(),
                    budgets = budgets,
                    goals = goals,
                    transactions = txs
                )

                _uiState.update {
                    it.copy(
                        snapshot = snapshot,
                        isLoadingData = isLoading
                    )
                }
            }
        }
    }

    fun onInputTextChange(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun onApiKeyInputChange(key: String) {
        _uiState.update { it.copy(apiKeyInput = key) }
    }

    fun openApiKeyDialog() {
        _uiState.update { it.copy(showApiKeyDialog = true, apiKeyInput = it.apiKey) }
    }

    fun closeApiKeyDialog() {
        _uiState.update { it.copy(showApiKeyDialog = false) }
    }

    fun saveApiKey(key: String, mode: AiProviderMode) {
        aiConfigRepository.saveConfig(apiKey = key.trim(), providerMode = mode)
        _uiState.update { it.copy(showApiKeyDialog = false) }
    }

    fun sendMessage(customText: String? = null) {
        val textToSend = (customText ?: _uiState.value.inputText).trim()
        if (textToSend.isBlank() || _uiState.value.isProcessing) return

        val userMessage = ChatMessage(
            sender = MessageSender.USER,
            text = textToSend
        )

        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                inputText = "",
                isProcessing = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            val response = aiChatbotUseCase.processUserMessage(textToSend, _uiState.value.messages)
            _uiState.update {
                it.copy(
                    messages = it.messages + response,
                    isProcessing = false
                )
            }
        }
    }

    fun clearChat() {
        _uiState.update {
            it.copy(
                messages = listOf(
                    ChatMessage(
                        sender = MessageSender.ASSISTANT,
                        text = "Chat cleared! How can I assist with your finances?"
                    )
                )
            )
        }
    }

    fun openSnapshotSheet() {
        _uiState.update { it.copy(isSnapshotSheetOpen = true) }
    }

    fun closeSnapshotSheet() {
        _uiState.update { it.copy(isSnapshotSheetOpen = false) }
    }
}
