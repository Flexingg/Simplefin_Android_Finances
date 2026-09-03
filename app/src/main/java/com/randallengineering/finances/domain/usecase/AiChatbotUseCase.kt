package com.randallengineering.finances.domain.usecase

import com.randallengineering.finances.core.ai.FinancialMcpTools
import com.randallengineering.finances.core.ai.GeminiApiClient
import com.randallengineering.finances.core.ai.ProposedCategorizationDto
import com.randallengineering.finances.core.ai.ToolExecutionResult
import com.randallengineering.finances.core.network.Resource
import com.randallengineering.finances.data.repository.AiConfigRepository
import com.randallengineering.finances.data.repository.AiProviderMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

@Serializable
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestampEpoch: Long = System.currentTimeMillis(),
    val toolResult: ToolExecutionResult? = null,
    val executedTools: List<ToolExecutionResult> = emptyList(),
    val proposedCategorizations: List<ProposedCategorizationDto> = emptyList(),
    val suggestedActions: List<String> = emptyList()
)

enum class MessageSender {
    USER,
    ASSISTANT
}

class AiChatbotUseCase(
    private val mcpTools: FinancialMcpTools,
    private val aiConfigRepository: AiConfigRepository? = null,
    private val geminiApiClient: GeminiApiClient? = null
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val systemPrompt = """
        You are the Randall Finances AI Financial Controller, a friendly, authoritative Duolingo-style Certified Financial Planner (CFP) and autonomous financial agent.
        You have direct real-time access to the user's SimpleFIN bank accounts, transaction history, envelope budgets, auto-rules, savings goals, and retirement projections via Model Context Protocol (MCP) tools.
        
        When the user asks questions or issues financial requests, choose the appropriate tool:
        - For allowance / cash flow: call get_financial_summary
        - For categorizing / classifying: call categorize_transaction or propose_categorization_review
        - For budgeting: call set_budget or list_budgets
        - For auto-rules: call create_rule or list_rules
        - For retirement, FIRE numbers, Coast FIRE, or retirement readiness: call simulate_retirement_projection
        - For debt strategies (Snowball vs. Avalanche): call simulate_debt_payoff
        - For splitting transactions: call split_transaction
        - For bank sync: call sync_simplefin_accounts
        
        Synthesize your final response using clear Markdown, bullet points, emoji icons, and currency formatting. Keep your tone motivational, clear, and proactive.
    """.trimIndent()

    suspend fun processUserMessage(userText: String, conversationHistory: List<ChatMessage> = emptyList()): ChatMessage = withContext(Dispatchers.IO) {
        val trimmed = userText.trim()
        if (trimmed.isBlank()) {
            return@withContext ChatMessage(
                sender = MessageSender.ASSISTANT,
                text = "How can I help with your finances today? You can ask me to categorize transactions, check budgets, calculate your daily allowance, or simulate your retirement!"
            )
        }

        // 1. Check if approving recent proposed categorizations
        val lower = trimmed.lowercase()
        if (lower.contains("apply") || lower.contains("approve") || lower.contains("looks good") || lower.contains("confirm")) {
            val lastProposalMsg = conversationHistory.lastOrNull { it.proposedCategorizations.isNotEmpty() }
            if (lastProposalMsg != null) {
                val batchJson = json.encodeToString(lastProposalMsg.proposedCategorizations)
                val execResult = mcpTools.executeTool("apply_proposed_categorizations", """{"batchJson":${json.encodeToString(batchJson)}}""")
                return@withContext ChatMessage(
                    sender = MessageSender.ASSISTANT,
                    text = execResult.message,
                    toolResult = execResult
                )
            }
        }

        // 2. Try Gemini API Client if configured
        val apiKey = aiConfigRepository?.getApiKey().orEmpty()
        val providerMode = aiConfigRepository?.getProviderMode() ?: AiProviderMode.CUSTOM_KEY
        val model = aiConfigRepository?.getSelectedModel() ?: "gemini-2.5-flash"

        if (geminiApiClient != null && apiKey.isNotBlank() && providerMode == AiProviderMode.CUSTOM_KEY) {
            val historyPairs = conversationHistory.takeLast(10).map { msg ->
                val role = if (msg.sender == MessageSender.USER) "user" else "model"
                Pair(role, msg.text)
            }

            when (val geminiRes = geminiApiClient.generateChatResponse(
                apiKey = apiKey,
                modelName = model,
                systemPrompt = systemPrompt,
                userMessage = trimmed,
                history = historyPairs
            )) {
                is Resource.Success -> {
                    val out = geminiRes.data!!
                    return@withContext ChatMessage(
                        sender = MessageSender.ASSISTANT,
                        text = out.responseText,
                        executedTools = out.executedTools,
                        toolResult = out.executedTools.lastOrNull(),
                        proposedCategorizations = out.executedTools.flatMap { it.proposedCategorizations }
                    )
                }
                is Resource.Error -> {
                    // If Gemini returned an error, check if we can fall back to local intent parser
                    val localFallback = tryParseLocalIntent(trimmed)
                    if (localFallback != null) {
                        val execResult = mcpTools.executeTool(localFallback.toolName, localFallback.argsJson)
                        return@withContext ChatMessage(
                            sender = MessageSender.ASSISTANT,
                            text = "(Offline / Local Fallback: ${geminiRes.message})\n\n" + execResult.message,
                            toolResult = execResult,
                            executedTools = listOf(execResult),
                            proposedCategorizations = execResult.proposedCategorizations
                        )
                    }
                    return@withContext ChatMessage(
                        sender = MessageSender.ASSISTANT,
                        text = "⚠️ **Gemini Advisor Error**: ${geminiRes.message}\n\nPlease check your Gemini API key in Settings, or try a simpler command like *'What is my daily allowance?'* or *'Simulate retirement'*."
                    )
                }
                else -> Unit
            }
        }

        // 3. Deterministic Local Intent Engine (runs when no key or as instant local executor)
        val localIntentResult = tryParseLocalIntent(trimmed)
        if (localIntentResult != null) {
            val execResult = mcpTools.executeTool(localIntentResult.toolName, localIntentResult.argsJson)
            return@withContext ChatMessage(
                sender = MessageSender.ASSISTANT,
                text = execResult.message,
                toolResult = execResult,
                executedTools = listOf(execResult),
                proposedCategorizations = execResult.proposedCategorizations
            )
        }

        // 4. General fallback with suggestions
        handleGeneralQuery(trimmed)
    }

    private data class ParsedIntent(val toolName: String, val argsJson: String)

    private fun tryParseLocalIntent(text: String): ParsedIntent? {
        val lower = text.lowercase()

        // 1. Retirement simulation: "simulate retirement", "can i retire", "fire number", "retirement projection"
        if (lower.contains("retire") || lower.contains("retirement") || lower.contains("fire") || lower.contains("nest egg") || lower.contains("coast fire")) {
            val ageMatch = Regex("(?:at|age)\\s+([0-9]{2})", RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1)?.toIntOrNull()
            val saveMatch = Regex("(?:save|saving|contribute)\\s+\\$?([0-9]+)", RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1)?.toDoubleOrNull()
            val args = buildString {
                append("{")
                var first = true
                if (ageMatch != null) {
                    append("\"retirementAge\":$ageMatch")
                    first = false
                }
                if (saveMatch != null) {
                    if (!first) append(",")
                    append("\"monthlyContribution\":$saveMatch")
                }
                append("}")
            }
            return ParsedIntent("simulate_retirement_projection", args)
        }

        // 2. Debt payoff simulation: "snowball", "avalanche", "debt payoff", "pay off debt"
        if (lower.contains("debt") || lower.contains("snowball") || lower.contains("avalanche") || lower.contains("pay off")) {
            val strategy = if (lower.contains("avalanche")) "Avalanche" else "Snowball"
            val paymentMatch = Regex("\\$?([0-9]+(?:\\.[0-9]{2})?)\\s*(?:per month|/mo|a month)?", RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1)?.toDoubleOrNull() ?: 350.0
            return ParsedIntent("simulate_debt_payoff", """{"strategy":"$strategy","monthlyPaymentAmount":$paymentMatch}""")
        }

        // 3. Bank sync: "sync banks", "sync accounts", "pull transactions"
        if (lower.contains("sync") && (lower.contains("bank") || lower.contains("account") || lower.contains("simplefin") || lower.contains("now"))) {
            return ParsedIntent("sync_simplefin_accounts", """{"daysBack":90}""")
        }

        // 4. Transaction review / auto-categorize: "pull last 10 transactions...", "categorize my transactions"
        if ((lower.contains("transaction") || lower.contains("transactions")) &&
            (lower.contains("review") || lower.contains("pull") || lower.contains("categorize") || lower.contains("suggest") || lower.contains("auto"))
        ) {
            val limitMatch = Regex("([0-9]+)\\s+transactions?", RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1)
            val limit = limitMatch?.toIntOrNull() ?: 10
            return ParsedIntent("propose_categorization_review", """{"limit":$limit}""")
        }

        // 5. Categorize specific: "categorize Shell as Gas"
        if (lower.contains("categorize") || lower.contains("tag as") || lower.contains("classify")) {
            val keywordMatch = Regex("(?:categorize|tag|classify)\\s+(?:all\\s+)?(?:transactions?\\s+)?(?:from\\s+|with\\s+|for\\s+)?['\"]?([a-zA-Z0-9\\s&]+?)['\"]?\\s+(?:as|to|under|into)", RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1)
            val catMatch = Regex("(?:as|to|under|into)\\s+['\"]?([a-zA-Z0-9\\s&]+)(?:>|->|/)?([a-zA-Z0-9\\s&]*)?['\"]?", RegexOption.IGNORE_CASE).find(text)

            if (keywordMatch != null && catMatch != null) {
                val keyword = keywordMatch.trim()
                val mainCat = catMatch.groupValues.getOrNull(1)?.trim() ?: "General"
                val subCat = catMatch.groupValues.getOrNull(2)?.trim().orEmpty()
                val args = """{"keyword":"$keyword","mainCategory":"$mainCat","subCategory":"$subCat"}"""
                return ParsedIntent("categorize_transaction", args)
            }
        }

        // 6. Rule creation
        if (lower.contains("rule") && (lower.contains("create") || lower.contains("make") || lower.contains("add") || lower.contains("set up"))) {
            val nameMatch = Regex("(?:rule(?:\\s+called|\\s+for|\\s+named)?)\\s+['\"]?([a-zA-Z0-9\\s&]+?)['\"]?\\s+(?:to|for|category|categorize|->)", RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1)
            val catMatch = Regex("(?:category|as|to|into|->)\\s+['\"]?([a-zA-Z0-9\\s&]+)(?:>|->|/|sub)?([a-zA-Z0-9\\s&]*)?['\"]?", RegexOption.IGNORE_CASE).find(text)

            val ruleName = nameMatch?.trim() ?: "Auto-Rule"
            val mainCat = catMatch?.groupValues?.getOrNull(1)?.trim()?.ifBlank { "General" } ?: "General"
            val subCat = catMatch?.groupValues?.getOrNull(2)?.trim().orEmpty()

            val args = """{"name":"$ruleName","pattern":"(?i).*${ruleName.lowercase().trim()}.*","category":"$mainCat","subCategory":"$subCat"}"""
            return ParsedIntent("create_rule", args)
        }

        // 7. Budget creation
        if (lower.contains("budget") && (lower.contains("set") || lower.contains("create") || lower.contains("make") || lower.contains("$"))) {
            val amountMatch = Regex("\\$?([0-9]+(?:\\.[0-9]{2})?)", RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1)
            val catMatch = Regex("(?:for|on|in)\\s+['\"]?([a-zA-Z0-9\\s&]+)['\"]?", RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1)

            if (amountMatch != null && catMatch != null) {
                val amount = amountMatch.toDoubleOrNull() ?: 0.0
                val category = catMatch.trim()
                val args = """{"category":"$category","categoryType":"VARIABLE","targetAmount":$amount}"""
                return ParsedIntent("set_budget", args)
            }
        }

        // 8. Allowance / Summary
        if (lower.contains("allowance") || lower.contains("daily target") || lower.contains("how much can i spend") || lower.contains("summary") || lower.contains("cash flow")) {
            return ParsedIntent("get_financial_summary", "{}")
        }

        // 9. List queries
        if (lower.contains("list categories") || lower.contains("show categories") || lower.contains("what categories")) {
            return ParsedIntent("list_categories", "{}")
        }
        if (lower.contains("list rules") || lower.contains("show rules") || lower.contains("what rules")) {
            return ParsedIntent("list_rules", "{}")
        }
        if (lower.contains("list budgets") || lower.contains("show budgets") || lower.contains("my budgets")) {
            return ParsedIntent("list_budgets", "{}")
        }
        if (lower.contains("list transactions") || lower.contains("recent transactions") || lower.contains("show transactions")) {
            return ParsedIntent("list_transactions", """{"limit":10}""")
        }

        return null
    }

    private fun handleGeneralQuery(text: String): ChatMessage {
        return ChatMessage(
            sender = MessageSender.ASSISTANT,
            text = "I'm ready to manage your finances! You can ask me questions or command actions:\n\n" +
                    "• **\"What is my target daily allowance?\"**\n" +
                    "• **\"Simulate my retirement if I retire at 62\"**\n" +
                    "• **\"Compare Snowball vs Avalanche debt payoff\"**\n" +
                    "• **\"Review last 10 transactions for categorization\"**\n" +
                    "• **\"Create a rule for Shell -> Transportation > Gas\"**\n" +
                    "• **\"Set a budget of $500 for Food & Dining\"**\n" +
                    "• **\"Sync my bank accounts\"**",
            suggestedActions = listOf(
                "Simulate my retirement at age 62",
                "What is my daily allowance?",
                "Review transactions for categorization",
                "Compare Snowball vs Avalanche debt payoff",
                "Show my active budgets"
            )
        )
    }
}
