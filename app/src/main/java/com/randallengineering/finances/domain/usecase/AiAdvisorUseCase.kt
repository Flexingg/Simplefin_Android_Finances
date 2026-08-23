package com.randallengineering.finances.domain.usecase

import com.google.firebase.Firebase
import com.google.firebase.vertexai.GenerativeModel
import com.google.firebase.vertexai.vertexAI
import com.google.firebase.vertexai.type.content
import com.randallengineering.finances.core.network.Resource
import com.randallengineering.finances.core.util.AiSnapshotFormatter
import com.randallengineering.finances.core.util.DateUtils
import com.randallengineering.finances.domain.model.AiInsight
import com.randallengineering.finances.domain.model.AiSnapshot
import com.randallengineering.finances.domain.model.Budget
import com.randallengineering.finances.domain.model.CategorySpendSnapshot
import com.randallengineering.finances.domain.model.Goal
import com.randallengineering.finances.domain.model.GoalSnapshot
import com.randallengineering.finances.domain.model.InsightSeverity
import com.randallengineering.finances.domain.model.InsightType
import com.randallengineering.finances.domain.model.SimpleFinAccount
import com.randallengineering.finances.domain.model.Transaction
import com.randallengineering.finances.domain.model.TransactionSummarySnapshot
import java.time.Instant
import java.util.UUID

class AiAdvisorUseCase(
    private val budgetCalculatorUseCase: BudgetCalculatorUseCase,
    private val goalPacingUseCase: GoalPacingUseCase
) {
    // Model identifier for Gemini 3.7 Flash
    private val modelName = "gemini-3.7-flash"

    private val generativeModel: GenerativeModel by lazy {
        Firebase.vertexAI.generativeModel(
            modelName = modelName
        )
    }

    /**
     * Builds a comprehensive financial snapshot object.
     */
    fun compileAiSnapshot(
        accounts: List<SimpleFinAccount>,
        budgets: List<Budget>,
        goals: List<Goal>,
        transactions: List<Transaction>
    ): AiSnapshot {
        val totalCashBalance = accounts.sumOf { it.balance }

        val budgetResult = budgetCalculatorUseCase.calculate(budgets, transactions)

        val categorySpends = budgetResult.calculatedBudgets.map {
            CategorySpendSnapshot(
                category = it.category,
                spentAmount = it.spentAmount,
                budgetAmount = it.targetAmount,
                pacingPercent = it.pacingPercent,
                isAnomaly = it.isAnomalyOverpacing
            )
        }

        val goalSnapshots = goals.map { goal ->
            val pacing = goalPacingUseCase.calculatePacing(goal)
            GoalSnapshot(
                title = goal.title,
                currentAmount = goal.currentAmount,
                targetAmount = goal.targetAmount,
                progressPercent = pacing.progressPercent,
                targetDate = DateUtils.formatDate(goal.targetEpochSeconds),
                requiredDailySaving = pacing.requiredDailySaving,
                requiredMonthlySaving = pacing.requiredMonthlySaving
            )
        }

        val recentTransactions = transactions
            .sortedByDescending { it.postedEpochSeconds }
            .take(30)
            .map {
                TransactionSummarySnapshot(
                    date = DateUtils.formatShortDate(it.postedEpochSeconds),
                    description = it.originalDesc,
                    amount = it.amount,
                    category = it.displayCategory
                )
            }

        return AiSnapshot(
            generatedTimestamp = Instant.now().toString(),
            totalCashBalance = totalCashBalance,
            accounts = accounts,
            mtdTotalIncome = budgetResult.totalMtdIncome,
            mtdTotalExpense = budgetResult.totalMtdExpense,
            mtdNetSavings = budgetResult.totalMtdIncome - budgetResult.totalMtdExpense,
            targetDailyAllowance = budgetResult.targetDailyAllowance,
            categorySpends = categorySpends,
            activeGoals = goalSnapshots,
            recentTransactions = recentTransactions
        )
    }

    /**
     * Identifies immediate local rule-based insights including >120% pacing anomaly alerts.
     */
    fun extractRuleBasedInsights(
        snapshot: AiSnapshot
    ): List<AiInsight> {
        val insights = mutableListOf<AiInsight>()

        // 1. Spending anomalies (>120% pacing)
        val overpacingCategories = snapshot.categorySpends.filter { it.pacingPercent >= 120.0 }
        for (anomaly in overpacingCategories) {
            insights.add(
                AiInsight(
                    id = UUID.randomUUID().toString(),
                    title = "Overpacing Alert: ${anomaly.category}",
                    summary = "${anomaly.category} is pacing at ${String.format("%.1f", anomaly.pacingPercent)}% of its monthly target ($${String.format("%.2f", anomaly.spentAmount)} spent of $${String.format("%.2f", anomaly.budgetAmount)}).",
                    type = InsightType.ANOMALY_OVERPACING,
                    severity = InsightSeverity.HIGH,
                    actionableTip = "Consider shifting non-essential purchases or adjusting sub-category allocations for the remaining days of the month."
                )
            )
        }

        // 2. Cash flow projection
        val projectedSavings = snapshot.mtdNetSavings
        if (projectedSavings < 0) {
            insights.add(
                AiInsight(
                    id = UUID.randomUUID().toString(),
                    title = "Negative Net Cash Flow Warning",
                    summary = "Your MTD expenses ($${String.format("%.2f", snapshot.mtdTotalExpense)}) currently exceed your income ($${String.format("%.2f", snapshot.mtdTotalIncome)}) by $${String.format("%.2f", -projectedSavings)}.",
                    type = InsightType.CASH_FLOW_FORECAST,
                    severity = InsightSeverity.HIGH,
                    actionableTip = "Keep daily variable spending under your target allowance of $${String.format("%.2f", snapshot.targetDailyAllowance)}/day."
                )
            )
        } else {
            insights.add(
                AiInsight(
                    id = UUID.randomUUID().toString(),
                    title = "Positive Cash Flow Forecast",
                    summary = "You have accumulated $${String.format("%.2f", snapshot.mtdNetSavings)} in net savings this month.",
                    type = InsightType.CASH_FLOW_FORECAST,
                    severity = InsightSeverity.LOW,
                    actionableTip = "You could allocate surplus funds towards your active savings goals."
                )
            )
        }

        return insights
    }

    /**
     * Executes deep financial analysis prompt with Gemini 3.7 Flash.
     */
    suspend fun generateGeminiInsights(snapshot: AiSnapshot): Resource<String> {
        return try {
            val markdownContext = AiSnapshotFormatter.toMarkdown(snapshot)
            val prompt = """
                You are an expert Certified Financial Planner (CFP) analyzing the user's financial snapshot.
                
                $markdownContext
                
                Please provide:
                1. A concise Executive Summary of their financial health.
                2. Key Anomalies & Red Flags (highlighting categories over 120% pacing).
                3. Cash Flow & Runway Forecast for the rest of the month.
                4. Three high-impact, actionable recommendations to improve savings and achieve their goals.
                
                Format with clear Markdown headers, bullet points, and an encouraging, professional tone.
            """.trimIndent()

            val response = generativeModel.generateContent(prompt)

            val text = response.text ?: "No insight response received from Gemini."
            Resource.Success(text)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Gemini analysis encountered an error", e)
        }
    }
}
