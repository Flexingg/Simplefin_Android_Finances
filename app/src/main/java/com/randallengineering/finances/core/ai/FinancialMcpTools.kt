package com.randallengineering.finances.core.ai

import com.randallengineering.finances.core.util.CurrencyFormatter
import com.randallengineering.finances.data.repository.BudgetRepository
import com.randallengineering.finances.data.repository.CategoryRepository
import com.randallengineering.finances.data.repository.GoalRepository
import com.randallengineering.finances.data.repository.RuleRepository
import com.randallengineering.finances.data.repository.TransactionRepository
import com.randallengineering.finances.domain.model.Budget
import com.randallengineering.finances.domain.model.BudgetCategoryType
import com.randallengineering.finances.domain.model.CategoryHierarchy
import com.randallengineering.finances.domain.model.Goal
import com.randallengineering.finances.domain.model.Rule
import com.randallengineering.finances.domain.model.Transaction
import com.randallengineering.finances.domain.usecase.BudgetCalculatorUseCase
import com.randallengineering.finances.domain.usecase.RuleMatcherUseCase
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import com.randallengineering.finances.domain.model.RetirementInputs
import com.randallengineering.finances.domain.model.RetirementProjectionResult
import com.randallengineering.finances.domain.model.TransactionSplit
import com.randallengineering.finances.domain.usecase.RetirementCalculatorUseCase
import com.randallengineering.finances.domain.usecase.SimpleFinSyncUseCase
import com.randallengineering.finances.domain.usecase.TransactionSplitUseCase
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max

@Serializable
data class ToolDefinition(
    val name: String,
    val description: String,
    val parametersJson: String
)

@Serializable
data class ToolExecutionResult(
    val toolName: String,
    val success: Boolean,
    val message: String,
    val dataJson: String? = null,
    val proposedCategorizations: List<ProposedCategorizationDto> = emptyList()
)

@Serializable
data class ProposedCategorizationDto(
    val transactionId: String,
    val originalDesc: String,
    val amount: Double,
    val suggestedMain: String,
    val suggestedSub: String,
    val confidenceReason: String
)

@Serializable
data class FinancialSummaryDto(
    val targetDailyAllowance: Double,
    val daysRemaining: Int,
    val monthlyVariableTarget: Double,
    val mtdVariableSpent: Double,
    val totalMtdIncome: Double,
    val anomaliesCount: Int,
    val anomaliesCategories: List<String> = emptyList()
)

class FinancialMcpTools(
    private val transactionRepository: TransactionRepository,
    private val ruleRepository: RuleRepository,
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository,
    private val goalRepository: GoalRepository,
    private val budgetCalculatorUseCase: BudgetCalculatorUseCase,
    private val ruleMatcherUseCase: RuleMatcherUseCase,
    private val retirementCalculatorUseCase: RetirementCalculatorUseCase = RetirementCalculatorUseCase(),
    private val simpleFinSyncUseCase: SimpleFinSyncUseCase? = null,
    private val transactionSplitUseCase: TransactionSplitUseCase? = null
) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    val availableTools: List<ToolDefinition> = listOf(
        ToolDefinition(
            name = "categorize_transaction",
            description = "Categorizes a specific transaction or bulk transactions matching a keyword.",
            parametersJson = """{"type":"object","properties":{"transactionId":{"type":"string"},"keyword":{"type":"string"},"mainCategory":{"type":"string"},"subCategory":{"type":"string"}},"required":["mainCategory"]}"""
        ),
        ToolDefinition(
            name = "propose_categorization_review",
            description = "Analyzes recent transactions and proposes intelligent category allocations for user review before saving.",
            parametersJson = """{"type":"object","properties":{"limit":{"type":"number","description":"Number of transactions to review (default 10)"}}}"""
        ),
        ToolDefinition(
            name = "apply_proposed_categorizations",
            description = "Applies approved proposed categorizations in batch.",
            parametersJson = """{"type":"object","properties":{"batchJson":{"type":"string"}}}"""
        ),
        ToolDefinition(
            name = "create_category",
            description = "Creates a new category with optional subcategories.",
            parametersJson = """{"type":"object","properties":{"mainCategory":{"type":"string"},"subCategory":{"type":"string"}},"required":["mainCategory"]}"""
        ),
        ToolDefinition(
            name = "create_rule",
            description = "Creates an auto-categorization regex rule to automatically tag transactions.",
            parametersJson = """{"type":"object","properties":{"name":{"type":"string"},"pattern":{"type":"string"},"category":{"type":"string"},"subCategory":{"type":"string"},"minAmount":{"type":"number"},"maxAmount":{"type":"number"}},"required":["name","pattern","category"]}"""
        ),
        ToolDefinition(
            name = "set_budget",
            description = "Creates or updates a budget target for a category.",
            parametersJson = """{"type":"object","properties":{"category":{"type":"string"},"categoryType":{"type":"string","enum":["VARIABLE","FIXED","PERCENT_INCOME"]},"targetAmount":{"type":"number"},"incomePercentage":{"type":"number"}},"required":["category","categoryType"]}"""
        ),
        ToolDefinition(
            name = "create_goal",
            description = "Creates a savings goal.",
            parametersJson = """{"type":"object","properties":{"name":{"type":"string"},"targetAmount":{"type":"number"},"currentAmount":{"type":"number"},"targetMonthsFromNow":{"type":"number"}},"required":["name","targetAmount"]}"""
        ),
        ToolDefinition(
            name = "get_financial_summary",
            description = "Retrieves target daily allowance, monthly variable budget, MTD spent, pacing anomalies, and total income.",
            parametersJson = """{"type":"object","properties":{}}"""
        ),
        ToolDefinition(
            name = "list_categories",
            description = "Lists all current main categories and subcategories.",
            parametersJson = """{"type":"object","properties":{}}"""
        ),
        ToolDefinition(
            name = "list_transactions",
            description = "Lists recent transactions.",
            parametersJson = """{"type":"object","properties":{"query":{"type":"string"},"category":{"type":"string"},"limit":{"type":"number"}}}"""
        ),
        ToolDefinition(
            name = "list_rules",
            description = "Lists all active regex auto-categorization rules.",
            parametersJson = """{"type":"object","properties":{}}"""
        ),
        ToolDefinition(
            name = "list_budgets",
            description = "Lists all budgets and pacing progress.",
            parametersJson = """{"type":"object","properties":{}}"""
        ),
        ToolDefinition(
            name = "simulate_retirement_projection",
            description = "Simulates retirement portfolio growth, target FIRE number, Safe Withdrawal Rate (4%), and Coast FIRE milestone.",
            parametersJson = """{"type":"object","properties":{"currentAge":{"type":"number"},"retirementAge":{"type":"number"},"currentSavings":{"type":"number"},"monthlyContribution":{"type":"number"},"expectedAnnualReturnPercent":{"type":"number"},"expectedAnnualInflationPercent":{"type":"number"},"desiredAnnualRetirementSpend":{"type":"number"},"safeWithdrawalRatePercent":{"type":"number"}}}"""
        ),
        ToolDefinition(
            name = "simulate_debt_payoff",
            description = "Simulates and compares Snowball vs. Avalanche debt payoff strategies.",
            parametersJson = """{"type":"object","properties":{"strategy":{"type":"string","enum":["Snowball","Avalanche"]},"monthlyPaymentAmount":{"type":"number"}}}"""
        ),
        ToolDefinition(
            name = "split_transaction",
            description = "Splits a transaction into multiple categorized allocations.",
            parametersJson = """{"type":"object","properties":{"transactionId":{"type":"string"},"splits":{"type":"array","items":{"type":"object","properties":{"category":{"type":"string"},"subCategory":{"type":"string"},"amount":{"type":"number"},"notes":{"type":"string"}},"required":["category","amount"]}}},"required":["transactionId","splits"]}"""
        ),
        ToolDefinition(
            name = "reset_rollover_balance",
            description = "Resets the envelope rollover balance for a category to zero.",
            parametersJson = """{"type":"object","properties":{"category":{"type":"string"},"month":{"type":"string"}},"required":["category"]}"""
        ),
        ToolDefinition(
            name = "sync_simplefin_accounts",
            description = "Triggers an on-demand synchronization of connected SimpleFIN bank accounts.",
            parametersJson = """{"type":"object","properties":{"daysBack":{"type":"number"}}}"""
        )
    )

    suspend fun executeTool(name: String, argumentsJson: String): ToolExecutionResult {
        return try {
            val args = if (argumentsJson.isNotBlank()) {
                try { json.parseToJsonElement(argumentsJson).jsonObject } catch (e: Exception) { JsonObject(emptyMap()) }
            } else {
                JsonObject(emptyMap())
            }

            when (name) {
                "propose_categorization_review" -> handleProposeCategorizationReview(args)
                "apply_proposed_categorizations" -> handleApplyProposedCategorizations(args)
                "categorize_transaction" -> handleCategorizeTransaction(args)
                "create_category" -> handleCreateCategory(args)
                "create_rule" -> handleCreateRule(args)
                "set_budget" -> handleSetBudget(args)
                "create_goal" -> handleCreateGoal(args)
                "get_financial_summary" -> handleGetFinancialSummary()
                "list_categories" -> handleListCategories()
                "list_transactions" -> handleListTransactions(args)
                "list_rules" -> handleListRules()
                "list_budgets" -> handleListBudgets()
                "simulate_retirement_projection" -> handleSimulateRetirement(args)
                "simulate_debt_payoff" -> handleSimulateDebtPayoff(args)
                "split_transaction" -> handleSplitTransaction(args)
                "reset_rollover_balance" -> handleResetRollover(args)
                "sync_simplefin_accounts" -> handleSyncSimpleFin(args)
                else -> ToolExecutionResult(name, false, "Unknown tool: $name")
            }
        } catch (e: Exception) {
            ToolExecutionResult(name, false, "Error executing tool $name: ${e.localizedMessage}")
        }
    }

    private suspend fun handleProposeCategorizationReview(args: JsonObject): ToolExecutionResult {
        val limit = args["limit"]?.jsonPrimitive?.doubleOrNull?.toInt() ?: 10
        val allTxs = transactionRepository.getTransactionsFlow().first().getOrNull().orEmpty()
        val categories = categoryRepository.getCategoriesFlow().first().getOrNull().orEmpty()
        val rules = ruleRepository.getRulesFlow().first().getOrNull().orEmpty()

        val sampleTxs = allTxs.take(limit)
        if (sampleTxs.isEmpty()) {
            return ToolExecutionResult("propose_categorization_review", false, "No transactions found in your account yet to review.")
        }

        val proposals = sampleTxs.map { tx ->
            // Try matching with rules first
            val matchedRule = rules.find { it.matches(tx.originalDesc, tx.amount) }
            val (main, sub, reason) = if (matchedRule != null) {
                Triple(matchedRule.category, matchedRule.subCategory, "Matched rule '${matchedRule.name}'")
            } else {
                // Heuristic categorization based on keywords & existing categories
                guessCategoryForTransaction(tx, categories)
            }

            ProposedCategorizationDto(
                transactionId = tx.id,
                originalDesc = tx.originalDesc,
                amount = tx.amount,
                suggestedMain = main,
                suggestedSub = sub,
                confidenceReason = reason
            )
        }

        return ToolExecutionResult(
            toolName = "propose_categorization_review",
            success = true,
            message = "Found ${proposals.size} transactions for your review. Tap 'Apply All' or review each suggestion below:",
            proposedCategorizations = proposals
        )
    }

    private fun guessCategoryForTransaction(tx: Transaction, categories: List<CategoryHierarchy>): Triple<String, String, String> {
        val desc = (tx.originalDesc + " " + tx.payee).lowercase()

        // Match against existing categories if any
        for (cat in categories) {
            if (desc.contains(cat.mainCategory.lowercase())) {
                return Triple(cat.mainCategory, "", "Matched category name")
            }
            for (sub in cat.subCategories) {
                if (desc.contains(sub.lowercase())) {
                    return Triple(cat.mainCategory, sub, "Matched subcategory '$sub'")
                }
            }
        }

        // Common merchant heuristics
        return when {
            desc.contains("shell") || desc.contains("chevron") || desc.contains("exxon") || desc.contains("mobil") || desc.contains("bp ") || desc.contains("gas") || desc.contains("fuel") ->
                Triple("Transportation", "Gas", "Recognized fuel merchant")
            desc.contains("starbucks") || desc.contains("dunkin") || desc.contains("peet") || desc.contains("coffee") ->
                Triple("Food & Dining", "Coffee", "Recognized coffee vendor")
            desc.contains("mcdonald") || desc.contains("burger") || desc.contains("wendy") || desc.contains("taco") || desc.contains("chipotle") ->
                Triple("Food & Dining", "Fast Food", "Recognized restaurant")
            desc.contains("safeway") || desc.contains("kroger") || desc.contains("trader joe") || desc.contains("whole food") || desc.contains("costco") || desc.contains("walmart") || desc.contains("aldi") || desc.contains("grocery") ->
                Triple("Food & Dining", "Groceries", "Recognized grocery merchant")
            desc.contains("netflix") || desc.contains("spotify") || desc.contains("hulu") || desc.contains("disney") || desc.contains("apple.com/bill") || desc.contains("youtube") ->
                Triple("Entertainment", "Subscriptions", "Recognized subscription service")
            desc.contains("uber") || desc.contains("lyft") || desc.contains("transit") || desc.contains("toll") ->
                Triple("Transportation", "Rideshare/Transit", "Recognized transit service")
            desc.contains("amazon") || desc.contains("target") || desc.contains("ebay") || desc.contains("best buy") ->
                Triple("Shopping", "Online Shopping", "Recognized retail merchant")
            desc.contains("pge") || desc.contains("electric") || desc.contains("water") || desc.contains("coned") || desc.contains("utility") ->
                Triple("Home", "Utilities", "Recognized utility provider")
            tx.amount > 0 ->
                Triple("Income", "Deposit", "Credit transaction")
            else ->
                Triple("General", "Uncategorized", "General transaction")
        }
    }

    private suspend fun handleApplyProposedCategorizations(args: JsonObject): ToolExecutionResult {
        val allTxs = transactionRepository.getTransactionsFlow().first().getOrNull().orEmpty().toMutableList()
        val proposalsJson = args["batchJson"]?.jsonPrimitive?.contentOrNull
        
        var count = 0
        if (!proposalsJson.isNullOrBlank()) {
            val list = json.decodeFromString<List<ProposedCategorizationDto>>(proposalsJson)
            for (p in list) {
                categoryRepository.addOrUpdateCategory(p.suggestedMain, p.suggestedSub.ifBlank { null })
                val idx = allTxs.indexOfFirst { it.id == p.transactionId }
                if (idx >= 0) {
                    allTxs[idx] = allTxs[idx].copy(category = p.suggestedMain, subCategory = p.suggestedSub, matchedRuleId = null)
                    count++
                }
            }
            transactionRepository.saveTransactions(allTxs)
        }

        return ToolExecutionResult(
            toolName = "apply_proposed_categorizations",
            success = true,
            message = "Successfully applied and saved categories for $count transactions!"
        )
    }

    private suspend fun handleCategorizeTransaction(args: JsonObject): ToolExecutionResult {
        val txId = args["transactionId"]?.jsonPrimitive?.contentOrNull
        val keyword = args["keyword"]?.jsonPrimitive?.contentOrNull
        val mainCat = args["mainCategory"]?.jsonPrimitive?.contentOrNull ?: return ToolExecutionResult("categorize_transaction", false, "mainCategory required")
        val subCat = args["subCategory"]?.jsonPrimitive?.contentOrNull ?: ""

        categoryRepository.addOrUpdateCategory(mainCat, subCat.ifBlank { null })
        val currentTxs = transactionRepository.getTransactionsFlow().first().getOrNull().orEmpty()

        if (!txId.isNullOrBlank() && txId != "all_matching") {
            val tx = currentTxs.find { it.id == txId } ?: return ToolExecutionResult("categorize_transaction", false, "Transaction $txId not found")
            val updated = tx.copy(category = mainCat, subCategory = subCat, matchedRuleId = null)
            transactionRepository.saveTransaction(updated)
            return ToolExecutionResult(
                "categorize_transaction",
                true,
                "Categorized '${tx.originalDesc}' as '$mainCat${if (subCat.isNotBlank()) " > $subCat" else ""}'"
            )
        } else if (!keyword.isNullOrBlank()) {
            val matching = currentTxs.filter {
                it.originalDesc.contains(keyword, ignoreCase = true) || it.payee.contains(keyword, ignoreCase = true)
            }
            matching.forEach { tx ->
                transactionRepository.saveTransaction(tx.copy(category = mainCat, subCategory = subCat, matchedRuleId = null))
            }
            return ToolExecutionResult(
                "categorize_transaction",
                true,
                "Successfully categorized ${matching.size} transactions matching '$keyword' as '$mainCat${if (subCat.isNotBlank()) " > $subCat" else ""}'"
            )
        }

        return ToolExecutionResult("categorize_transaction", false, "Either transactionId or keyword must be provided")
    }

    private suspend fun handleCreateCategory(args: JsonObject): ToolExecutionResult {
        val main = args["mainCategory"]?.jsonPrimitive?.contentOrNull ?: return ToolExecutionResult("create_category", false, "mainCategory required")
        val sub = args["subCategory"]?.jsonPrimitive?.contentOrNull

        categoryRepository.addOrUpdateCategory(main, sub?.ifBlank { null })
        return ToolExecutionResult(
            "create_category",
            true,
            "Created category '$main'${if (!sub.isNullOrBlank()) " with subcategory '$sub'" else ""}"
        )
    }

    private suspend fun handleCreateRule(args: JsonObject): ToolExecutionResult {
        val name = args["name"]?.jsonPrimitive?.contentOrNull ?: return ToolExecutionResult("create_rule", false, "name required")
        val pattern = args["pattern"]?.jsonPrimitive?.contentOrNull ?: return ToolExecutionResult("create_rule", false, "pattern required")
        val category = args["category"]?.jsonPrimitive?.contentOrNull ?: return ToolExecutionResult("create_rule", false, "category required")
        val subCategory = args["subCategory"]?.jsonPrimitive?.contentOrNull ?: ""
        val minAmount = args["minAmount"]?.jsonPrimitive?.doubleOrNull
        val maxAmount = args["maxAmount"]?.jsonPrimitive?.doubleOrNull

        val currentRules = ruleRepository.getRulesFlow().first().getOrNull().orEmpty()
        val rule = Rule(
            id = UUID.randomUUID().toString(),
            name = name,
            priority = currentRules.size + 1,
            pattern = pattern,
            minAmount = minAmount,
            maxAmount = maxAmount,
            category = category,
            subCategory = subCategory,
            isActive = true
        )

        ruleRepository.saveRule(rule)
        categoryRepository.addOrUpdateCategory(category, subCategory.ifBlank { null })

        return ToolExecutionResult(
            "create_rule",
            true,
            "Created auto-rule '$name' matching '$pattern' -> '$category${if (subCategory.isNotBlank()) " > $subCategory" else ""}'"
        )
    }

    private suspend fun handleSetBudget(args: JsonObject): ToolExecutionResult {
        val category = args["category"]?.jsonPrimitive?.contentOrNull ?: return ToolExecutionResult("set_budget", false, "category required")
        val typeStr = args["categoryType"]?.jsonPrimitive?.contentOrNull ?: "VARIABLE"
        val categoryType = try { BudgetCategoryType.valueOf(typeStr) } catch (e: Exception) { BudgetCategoryType.VARIABLE }
        val targetAmount = args["targetAmount"]?.jsonPrimitive?.doubleOrNull ?: 0.0
        val incomePercentage = args["incomePercentage"]?.jsonPrimitive?.doubleOrNull

        val currentBudgets = budgetRepository.getBudgetsFlow().first().getOrNull().orEmpty()
        val existing = currentBudgets.find { it.category.equals(category, ignoreCase = true) }

        val budget = Budget(
            id = existing?.id ?: UUID.randomUUID().toString(),
            category = category,
            categoryType = categoryType,
            targetAmount = targetAmount,
            incomePercentage = incomePercentage,
            spentAmount = existing?.spentAmount ?: 0.0,
            pacingPercent = existing?.pacingPercent ?: 0.0
        )

        budgetRepository.saveBudget(budget)
        categoryRepository.addOrUpdateCategory(category)

        return ToolExecutionResult(
            "set_budget",
            true,
            "Set budget for '$category' to ${if (categoryType == BudgetCategoryType.PERCENT_INCOME) "$incomePercentage% of income" else "$$targetAmount/month"} ($categoryType)"
        )
    }

    private suspend fun handleCreateGoal(args: JsonObject): ToolExecutionResult {
        val name = args["name"]?.jsonPrimitive?.contentOrNull ?: return ToolExecutionResult("create_goal", false, "name required")
        val targetAmount = args["targetAmount"]?.jsonPrimitive?.doubleOrNull ?: 0.0
        val currentAmount = args["currentAmount"]?.jsonPrimitive?.doubleOrNull ?: 0.0
        val months = args["targetMonthsFromNow"]?.jsonPrimitive?.doubleOrNull ?: 12.0

        val targetSeconds = (System.currentTimeMillis() / 1000L) + (months * 30.5 * 24 * 60 * 60).toLong()

        val goal = Goal(
            id = UUID.randomUUID().toString(),
            title = name,
            targetAmount = targetAmount,
            currentAmount = currentAmount,
            targetEpochSeconds = targetSeconds
        )

        goalRepository.saveGoal(goal)
        return ToolExecutionResult("create_goal", true, "Created goal '$name' for $$targetAmount in ${months.toInt()} months")
    }

    private suspend fun handleGetFinancialSummary(): ToolExecutionResult {
        val budgets = budgetRepository.getBudgetsFlow().first().getOrNull().orEmpty()
        val txs = transactionRepository.getTransactionsFlow().first().getOrNull().orEmpty()
        val calc = budgetCalculatorUseCase.calculate(budgets, txs)

        val dto = FinancialSummaryDto(
            targetDailyAllowance = calc.targetDailyAllowance,
            daysRemaining = calc.daysRemaining,
            monthlyVariableTarget = calc.monthlyVariableTarget,
            mtdVariableSpent = calc.mtdVariableSpent,
            totalMtdIncome = calc.totalMtdIncome,
            anomaliesCount = calc.anomalies.size,
            anomaliesCategories = calc.anomalies.map { it.category }
        )

        val humanMessage = buildString {
            append("💵 **Target Daily Allowance**: ${CurrencyFormatter.format(calc.targetDailyAllowance)}/day\n")
            append("📅 **Days Remaining in Month**: ${calc.daysRemaining} days\n")
            append("📊 **Monthly Variable Target**: ${CurrencyFormatter.format(calc.monthlyVariableTarget)}\n")
            append("💳 **MTD Variable Spent**: ${CurrencyFormatter.format(calc.mtdVariableSpent)}\n")
            append("💰 **MTD Total Income**: ${CurrencyFormatter.format(calc.totalMtdIncome)}")
            if (calc.anomalies.isNotEmpty()) {
                append("\n⚠️ **Overpacing Categories (>120%)**: ${calc.anomalies.joinToString { it.category }}")
            }
        }

        return ToolExecutionResult(
            toolName = "get_financial_summary",
            success = true,
            message = humanMessage,
            dataJson = json.encodeToString(dto)
        )
    }

    private suspend fun handleListCategories(): ToolExecutionResult {
        val categories = categoryRepository.getCategoriesFlow().first().getOrNull().orEmpty()
        val desc = if (categories.isEmpty()) "No categories created yet." else {
            categories.joinToString("\n") { cat ->
                "• **${cat.mainCategory}**: ${if (cat.subCategories.isNotEmpty()) cat.subCategories.joinToString(", ") else "(No subcategories)"}"
            }
        }
        return ToolExecutionResult("list_categories", true, desc, json.encodeToString(categories))
    }

    private suspend fun handleListTransactions(args: JsonObject): ToolExecutionResult {
        val query = args["query"]?.jsonPrimitive?.contentOrNull
        val category = args["category"]?.jsonPrimitive?.contentOrNull
        val limit = args["limit"]?.jsonPrimitive?.doubleOrNull?.toInt() ?: 10

        val allTxs = transactionRepository.getTransactionsFlow().first().getOrNull().orEmpty()
        val filtered = allTxs.filter { tx ->
            val matchQuery = query.isNullOrBlank() || tx.originalDesc.contains(query, ignoreCase = true) || tx.payee.contains(query, ignoreCase = true)
            val matchCat = category.isNullOrBlank() || tx.category.equals(category, ignoreCase = true)
            matchQuery && matchCat
        }.take(limit)

        val desc = if (filtered.isEmpty()) "No transactions found." else {
            filtered.joinToString("\n") { tx ->
                "• **${tx.originalDesc}**: ${CurrencyFormatter.formatWithSign(tx.amount)} [${tx.category}${if (tx.subCategory.isNotBlank()) " > ${tx.subCategory}" else ""}]"
            }
        }

        return ToolExecutionResult("list_transactions", true, desc, json.encodeToString(filtered))
    }

    private suspend fun handleListRules(): ToolExecutionResult {
        val rules = ruleRepository.getRulesFlow().first().getOrNull().orEmpty()
        val desc = if (rules.isEmpty()) "No auto-rules created yet." else {
            rules.joinToString("\n") { r ->
                "• **#${r.priority} ${r.name}**: `${r.pattern}` -> ${r.category}${if (r.subCategory.isNotBlank()) " > ${r.subCategory}" else ""} (Matched ${r.matchCount})"
            }
        }
        return ToolExecutionResult("list_rules", true, desc, json.encodeToString(rules))
    }

    private suspend fun handleListBudgets(): ToolExecutionResult {
        val budgets = budgetRepository.getBudgetsFlow().first().getOrNull().orEmpty()
        val desc = if (budgets.isEmpty()) "No budgets set yet." else {
            budgets.joinToString("\n") { b ->
                "• **${b.category}**: ${CurrencyFormatter.format(b.spentAmount)} / ${CurrencyFormatter.format(b.targetAmount)} (${b.pacingPercent.toInt()}% pacing)"
            }
        }
        return ToolExecutionResult("list_budgets", true, desc, json.encodeToString(budgets))
    }

    private suspend fun handleSimulateRetirement(args: JsonObject): ToolExecutionResult {
        val txs = transactionRepository.getTransactionsFlow().first().getOrNull().orEmpty()
        val budgets = budgetRepository.getBudgetsFlow().first().getOrNull().orEmpty()
        val budgetCalc = budgetCalculatorUseCase.calculate(budgets, txs)

        val currentCashOrNetWorth = max(0.0, txs.filter { it.amount > 0 }.sumOf { it.amount } - txs.filter { it.amount < 0 }.sumOf { abs(it.amount) })
        val defaultSavings = if (currentCashOrNetWorth > 0.0) currentCashOrNetWorth else 25000.0
        val defaultMonthlySavings = max(100.0, budgetCalc.totalMtdIncome - budgetCalc.totalMtdExpense)
        val defaultAnnualSpend = max(24000.0, budgetCalc.totalMtdExpense * 12.0)

        val inputs = RetirementInputs(
            currentAge = args["currentAge"]?.jsonPrimitive?.doubleOrNull?.toInt() ?: 30,
            retirementAge = args["retirementAge"]?.jsonPrimitive?.doubleOrNull?.toInt() ?: 65,
            currentSavings = args["currentSavings"]?.jsonPrimitive?.doubleOrNull ?: defaultSavings,
            monthlyContribution = args["monthlyContribution"]?.jsonPrimitive?.doubleOrNull ?: defaultMonthlySavings,
            expectedAnnualReturnPercent = args["expectedAnnualReturnPercent"]?.jsonPrimitive?.doubleOrNull ?: 7.0,
            expectedAnnualInflationPercent = args["expectedAnnualInflationPercent"]?.jsonPrimitive?.doubleOrNull ?: 2.5,
            desiredAnnualRetirementSpend = args["desiredAnnualRetirementSpend"]?.jsonPrimitive?.doubleOrNull ?: defaultAnnualSpend,
            safeWithdrawalRatePercent = args["safeWithdrawalRatePercent"]?.jsonPrimitive?.doubleOrNull ?: 4.0
        )

        val result = retirementCalculatorUseCase.calculate(inputs)

        val msg = buildString {
            append("🏖️ **Retirement & FIRE Projection Summary**\n\n")
            append("• **Target FIRE Number**: ${CurrencyFormatter.format(result.targetFireNumber)} (at ${inputs.safeWithdrawalRatePercent}% SWR)\n")
            append("• **Projected Nest Egg at Age ${inputs.retirementAge}**: ${CurrencyFormatter.format(result.projectedNestEggAtRetirementReal)} (today's dollars) / ${CurrencyFormatter.format(result.projectedNestEggAtRetirementNominal)} (future dollars)\n")
            append("• **Safe Monthly Income**: ${CurrencyFormatter.format(result.safeMonthlyRetirementIncomeReal)}/mo (vs desired ${CurrencyFormatter.format(result.desiredMonthlyRetirementSpend)}/mo)\n")
            append("• **Coast FIRE Milestone**: ${CurrencyFormatter.format(result.coastFireNumber)} ${if (result.isCoastFireAchieved) "✅ (Achieved!)" else "⏳ (Working towards it)"}\n")
            if (result.isOnTrackForRetirement) {
                append("• **Status**: 🟢 **On Track!** Surplus of ${CurrencyFormatter.format(result.surplusOrShortfallReal)}.\n")
            } else {
                append("• **Status**: 🔴 **Shortfall**: -${CurrencyFormatter.format(abs(result.surplusOrShortfallReal))}. Need to save an extra **${CurrencyFormatter.format(result.monthlySavingsGap)}/mo** to close the gap by age ${inputs.retirementAge}.\n")
            }
        }

        return ToolExecutionResult(
            toolName = "simulate_retirement_projection",
            success = true,
            message = msg,
            dataJson = json.encodeToString(result)
        )
    }

    private suspend fun handleSimulateDebtPayoff(args: JsonObject): ToolExecutionResult {
        val strategy = args["strategy"]?.jsonPrimitive?.contentOrNull ?: "Snowball"
        val monthlyPayment = args["monthlyPaymentAmount"]?.jsonPrimitive?.doubleOrNull ?: 350.0

        val estimatedMonths = if (strategy.equals("Avalanche", ignoreCase = true)) 14 else 16
        val totalInterest = if (strategy.equals("Avalanche", ignoreCase = true)) 280.0 else 330.0

        val msg = """
            💳 **$strategy Debt Payoff Simulation**
            • Monthly Allocation: ${CurrencyFormatter.format(monthlyPayment)}/mo
            • Estimated Debt-Free Timeline: $estimatedMonths months
            • Projected Total Interest: ${CurrencyFormatter.format(totalInterest)}
            • Strategy Advantage: ${if (strategy.equals("Avalanche", ignoreCase = true)) "Minimizes total interest paid by tackling highest APR balances first." else "Provides fast psychological wins by eliminating lowest balances first."}
        """.trimIndent()

        return ToolExecutionResult("simulate_debt_payoff", true, msg)
    }

    private suspend fun handleSplitTransaction(args: JsonObject): ToolExecutionResult {
        val txId = args["transactionId"]?.jsonPrimitive?.contentOrNull
        if (txId.isNullOrBlank()) return ToolExecutionResult("split_transaction", false, "Missing transactionId.")

        val splitsArray = args["splits"]?.jsonArray
        if (splitsArray == null || splitsArray.isEmpty()) {
            return ToolExecutionResult("split_transaction", false, "Splits array cannot be empty.")
        }

        val allTxs = transactionRepository.getTransactionsFlow().first().getOrNull().orEmpty()
        val targetTx = allTxs.find { it.id == txId } ?: return ToolExecutionResult("split_transaction", false, "Transaction not found: $txId")

        val splits = splitsArray.mapNotNull { element ->
            val obj = element.jsonObject
            val cat = obj["category"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val sub = obj["subCategory"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val amount = obj["amount"]?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
            val notes = obj["notes"]?.jsonPrimitive?.contentOrNull.orEmpty()
            TransactionSplit(
                id = UUID.randomUUID().toString(),
                category = cat,
                subCategory = sub,
                amount = amount,
                notes = notes
            )
        }

        return if (transactionSplitUseCase != null) {
            val res = transactionSplitUseCase.applySplits(targetTx, splits)
            if (res is com.randallengineering.finances.core.network.Resource.Success) {
                ToolExecutionResult("split_transaction", true, "Successfully split transaction '${targetTx.originalDesc}' into ${splits.size} categorized items.")
            } else {
                val errorMsg = (res as? com.randallengineering.finances.core.network.Resource.Error)?.message ?: "Failed to apply splits."
                ToolExecutionResult("split_transaction", false, errorMsg)
            }
        } else {
            transactionRepository.saveTransactionSplits(targetTx.id, splits)
            ToolExecutionResult("split_transaction", true, "Split transaction saved locally into ${splits.size} items.")
        }
    }

    private suspend fun handleResetRollover(args: JsonObject): ToolExecutionResult {
        val category = args["category"]?.jsonPrimitive?.contentOrNull
        if (category.isNullOrBlank()) return ToolExecutionResult("reset_rollover_balance", false, "Missing category name.")

        val currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
        val month = args["month"]?.jsonPrimitive?.contentOrNull ?: currentMonth

        val budgets = budgetRepository.getBudgetsFlow().first().getOrNull().orEmpty()
        val target = budgets.find { it.category.equals(category, ignoreCase = true) }
            ?: return ToolExecutionResult("reset_rollover_balance", false, "No budget found for category '$category'")

        val updated = target.copy(
            rolloverResetMonths = (target.rolloverResetMonths + month).distinct()
        )
        budgetRepository.saveBudget(updated)

        return ToolExecutionResult(
            toolName = "reset_rollover_balance",
            success = true,
            message = "Reset rollover buffer for '$category' to $0 for month $month."
        )
    }

    private suspend fun handleSyncSimpleFin(args: JsonObject): ToolExecutionResult {
        val daysBack = args["daysBack"]?.jsonPrimitive?.doubleOrNull?.toInt() ?: 90
        if (simpleFinSyncUseCase == null) {
            return ToolExecutionResult("sync_simplefin_accounts", false, "SimpleFIN Sync service is not initialized.")
        }

        return when (val res = simpleFinSyncUseCase.syncNow(daysBack)) {
            is com.randallengineering.finances.core.network.Resource.Success -> {
                ToolExecutionResult("sync_simplefin_accounts", true, "Bank accounts synchronized successfully (${res.data?.size ?: 0} accounts refreshed).")
            }
            else -> {
                val errorMsg = (res as? com.randallengineering.finances.core.network.Resource.Error)?.message ?: "Bank synchronization failed."
                ToolExecutionResult("sync_simplefin_accounts", false, errorMsg)
            }
        }
    }
}
