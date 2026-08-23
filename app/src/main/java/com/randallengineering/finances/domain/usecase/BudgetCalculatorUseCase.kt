package com.randallengineering.finances.domain.usecase

import com.randallengineering.finances.core.util.DateUtils
import com.randallengineering.finances.domain.model.Budget
import com.randallengineering.finances.domain.model.BudgetCategoryType
import com.randallengineering.finances.domain.model.Transaction
import kotlin.math.abs

data class BudgetCalculationResult(
    val calculatedBudgets: List<Budget>,
    val totalMtdIncome: Double,
    val totalMtdExpense: Double,
    val mtdVariableSpent: Double,
    val monthlyVariableTarget: Double,
    val targetDailyAllowance: Double,
    val daysRemaining: Int,
    val anomalies: List<Budget>
)

class BudgetCalculatorUseCase {

    fun calculate(
        budgets: List<Budget>,
        transactions: List<Transaction>,
        startOfMonthEpoch: Long = DateUtils.getStartOfCurrentMonthEpochSeconds(),
        endOfMonthEpoch: Long = DateUtils.getEndOfCurrentMonthEpochSeconds(),
        daysRemaining: Int = DateUtils.getDaysRemainingInCurrentMonth()
    ): BudgetCalculationResult {
        // Filter transactions for current calendar month
        val mtdTransactions = transactions.filter {
            it.postedEpochSeconds in startOfMonthEpoch..endOfMonthEpoch
        }

        // Calculate total MTD Income (transactions tagged 'Income' or with positive amount)
        val totalMtdIncome = mtdTransactions
            .filter { it.category.equals("Income", ignoreCase = true) || it.amount > 0 }
            .sumOf { abs(it.amount) }

        // Calculate spend per category (handling split allocations as well)
        val spendByCategory = mutableMapOf<String, Double>()
        var totalMtdExpense = 0.0

        for (tx in mtdTransactions) {
            if (tx.isSplit) {
                for (split in tx.splits) {
                    if (split.amount < 0 || !split.category.equals("Income", ignoreCase = true)) {
                        val amount = abs(split.amount)
                        spendByCategory[split.category] = (spendByCategory[split.category] ?: 0.0) + amount
                        totalMtdExpense += amount
                    }
                }
            } else if (tx.amount < 0 || !tx.category.equals("Income", ignoreCase = true)) {
                val amount = abs(tx.amount)
                spendByCategory[tx.category] = (spendByCategory[tx.category] ?: 0.0) + amount
                totalMtdExpense += amount
            }
        }

        // Calculate each budget target and pacing
        val calculatedBudgets = budgets.map { budget ->
            val targetAmount = when (budget.categoryType) {
                BudgetCategoryType.PERCENT_INCOME -> {
                    val percent = budget.incomePercentage ?: 0.0
                    (percent / 100.0) * totalMtdIncome
                }
                else -> budget.targetAmount
            }

            val spent = spendByCategory[budget.category] ?: 0.0
            val pacing = if (targetAmount > 0) {
                (spent / targetAmount) * 100.0
            } else if (spent > 0) {
                100.0 // Spent money with 0 budget
            } else {
                0.0
            }

            budget.copy(
                targetAmount = targetAmount,
                spentAmount = spent,
                pacingPercent = pacing
            )
        }

        // Calculate Variable Target & Spent for Daily Allowance
        val variableBudgets = calculatedBudgets.filter { it.categoryType == BudgetCategoryType.VARIABLE }
        val monthlyVariableTarget = variableBudgets.sumOf { it.targetAmount }
        val mtdVariableSpent = variableBudgets.sumOf { it.spentAmount }

        val safeDays = daysRemaining.coerceAtLeast(1)
        val remainingVariableBudget = (monthlyVariableTarget - mtdVariableSpent).coerceAtLeast(0.0)
        val targetDailyAllowance = remainingVariableBudget / safeDays

        // Anomaly alerts (over 120% pacing)
        val anomalies = calculatedBudgets.filter { it.isAnomalyOverpacing }

        return BudgetCalculationResult(
            calculatedBudgets = calculatedBudgets,
            totalMtdIncome = totalMtdIncome,
            totalMtdExpense = totalMtdExpense,
            mtdVariableSpent = mtdVariableSpent,
            monthlyVariableTarget = monthlyVariableTarget,
            targetDailyAllowance = targetDailyAllowance,
            daysRemaining = safeDays,
            anomalies = anomalies
        )
    }
}
