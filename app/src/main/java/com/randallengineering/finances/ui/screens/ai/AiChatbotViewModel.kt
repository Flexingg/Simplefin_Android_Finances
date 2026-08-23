package com.randallengineering.finances.ui.screens.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.randallengineering.finances.domain.usecase.AiChatbotUseCase
import com.randallengineering.finances.domain.usecase.ChatMessage
import com.randallengineering.finances.domain.usecase.MessageSender
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatbotUiState(
    val messages: List<ChatMessage> = listOf(
        ChatMessage(
            sender = MessageSender.ASSISTANT,
            text = "👋 Hello! I am your AI Financial Copilot. You can tell me in plain English to:\n• \"Create a category called Home with subcategories Mortgage, Utilities, and Upkeep\"\n• \"Categorize all Shell transactions as Transportation > Gas\"\n• \"Create an auto-rule for Target -> Shopping\"\n• \"Set a budget of $500 for Food & Dining\"\n• \"What is my daily allowance this month?\"",
            suggestedActions = listOf(
                "What is my daily allowance?",
                "Create category 'Home' with subcategory 'Utilities'",
                "Set a budget of $500 for Food & Dining",
                "Create a rule for Starbucks -> Food & Dining > Coffee"
            )
        )
    ),
    val inputText: String = "",
    val isProcessing: Boolean = false,
    val isChatOpen: Boolean = false
)

class AiChatbotViewModel(
    private val aiChatbotUseCase: AiChatbotUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatbotUiState())
    val uiState: StateFlow<ChatbotUiState> = _uiState.asStateFlow()

    fun toggleChat(open: Boolean? = null) {
        _uiState.update { it.copy(isChatOpen = open ?: !it.isChatOpen) }
    }

    fun onInputTextChange(newText: String) {
        _uiState.update { it.copy(inputText = newText) }
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
                isProcessing = true
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
                        text = "Chat cleared. What would you like to do next?"
                    )
                )
            )
        }
    }
}
