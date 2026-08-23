package com.randallengineering.finances.domain.usecase

import com.randallengineering.finances.core.ai.FinancialMcpTools
import com.randallengineering.finances.core.ai.ProposedCategorizationDto
import com.randallengineering.finances.core.ai.ToolExecutionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestampEpoch: Long = System.currentTimeMillis(),
    val toolResult: ToolExecutionResult? = null,
    val proposedCategorizations: List<ProposedCategorizationDto> = emptyList(),
    val suggestedActions: List<String> = emptyList()
)

enum class MessageSender {
    USER,
    ASSISTANT
}

class AiChatbotUseCase(
    private val mcpTools: FinancialMcpTools
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun processUserMessage(userText: String, conversationHistory: List<ChatMessage> = emptyList()): ChatMessage = withContext(Dispatchers.IO) {
        val trimmed = userText.trim()
        if (trimmed.isBlank()) {
            return@withContext ChatMessage(
                sender = MessageSender.ASSISTANT,
                text = "How can I help with your finances? You can ask me to categorize transactions, create rules, add categories, or calculate your daily allowance."
            )
        }

        // Check if approving recent proposed categorizations
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

        // 1. Deterministic high-level intent engine
        val localIntentResult = tryParseLocalIntent(trimmed)
        if (localIntentResult != null) {
            val execResult = mcpTools.executeTool(localIntentResult.toolName, localIntentResult.argsJson)
            return@withContext ChatMessage(
                sender = MessageSender.ASSISTANT,
                text = execResult.message,
                toolResult = execResult,
                proposedCategorizations = execResult.proposedCategorizations
            )
        }

        // General fallback with helpful options
        handleGeneralQuery(trimmed)
    }

    private data class ParsedIntent(val toolName: String, val argsJson: String)

    private fun tryParseLocalIntent(text: String): ParsedIntent? {
        val lower = text.lowercase()

        // 1. Transaction review / auto-categorize with review: "pull last 10 transactions...", "categorize my transactions", "review transactions"
        if ((lower.contains("transaction") || lower.contains("transactions")) &&
            (lower.contains("review") || lower.contains("pull") || lower.contains("categorize") || lower.contains("suggest") || lower.contains("auto"))
        ) {
            val limitMatch = Regex("([0-9]+)\\s+transactions?", RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1)
            val limit = limitMatch?.toIntOrNull() ?: 10
            return ParsedIntent("propose_categorization_review", """{"limit":$limit}""")
        }

        // 2. Specific single/bulk categorization: "categorize Shell as Gas"
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

        // 3. Rule creation: "create rule ...", "make a rule ...", "rule for ..."
        if (lower.contains("rule") && (lower.contains("create") || lower.contains("make") || lower.contains("add") || lower.contains("set up"))) {
            val nameMatch = Regex("(?:rule(?:\\s+called|\\s+for|\\s+named)?)\\s+['\"]?([a-zA-Z0-9\\s&]+?)['\"]?\\s+(?:to|for|category|categorize|->)", RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1)
            val catMatch = Regex("(?:category|as|to|into|->)\\s+['\"]?([a-zA-Z0-9\\s&]+)(?:>|->|/|sub)?([a-zA-Z0-9\\s&]*)?['\"]?", RegexOption.IGNORE_CASE).find(text)

            val ruleName = nameMatch?.trim() ?: "Auto-Rule"
            val mainCat = catMatch?.groupValues?.getOrNull(1)?.trim()?.ifBlank { "General" } ?: "General"
            val subCat = catMatch?.groupValues?.getOrNull(2)?.trim().orEmpty()

            val args = """{"name":"$ruleName","pattern":"(?i).*${ruleName.lowercase().trim()}.*","category":"$mainCat","subCategory":"$subCat"}"""
            return ParsedIntent("create_rule", args)
        }

        // 4. Category creation: "create category ...", "add category ..."
        if (lower.contains("category") && (lower.contains("create") || lower.contains("add") || lower.contains("make") || lower.contains("new"))) {
            val mainMatch = Regex("(?:category|categories|new category)(?:\\s+called|\\s+named)?\\s+['\"]?([a-zA-Z0-9\\s&]+?)['\"]?(?:\\s+with|\\s+and|\\s+sub|$)", RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1)
            val subMatch = Regex("(?:with subcategory|with subcategories|subcategories|subcategory|sub)\\s+['\"]?([a-zA-Z0-9\\s&,]+)['\"]?", RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1)

            val mainCat = mainMatch?.trim() ?: "Custom Category"
            val subCat = subMatch?.split(",")?.firstOrNull()?.trim().orEmpty()

            val args = """{"mainCategory":"$mainCat","subCategory":"$subCat"}"""
            return ParsedIntent("create_category", args)
        }

        // 5. Budget creation: "set budget ...", "budget of $X for ..."
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

        // 6. Allowance / Financial Summary: "what is my daily allowance", "how much can I spend"
        if (lower.contains("allowance") || lower.contains("daily target") || lower.contains("how much can i spend") || lower.contains("summary") || lower.contains("cash flow")) {
            return ParsedIntent("get_financial_summary", "{}")
        }

        // 7. List queries
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
            text = "I'm ready to manage your finances! Here are things you can ask me to do:\n\n" +
                    "• **\"Pull my last 10 transactions and suggest categories for review\"**\n" +
                    "• **\"What is my target daily allowance?\"**\n" +
                    "• **\"Create a category called Home with subcategory Utilities\"**\n" +
                    "• **\"Create a rule for Shell -> Transportation > Gas\"**\n" +
                    "• **\"Set a budget of $500 for Food & Dining\"**\n" +
                    "• **\"Show my active auto-rules\"**",
            suggestedActions = listOf(
                "Review last 10 transactions for categorization",
                "What is my daily allowance?",
                "List my categories",
                "List my auto-rules"
            )
        )
    }
}
