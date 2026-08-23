package com.randallengineering.finances.domain.usecase

import com.randallengineering.finances.core.util.DateUtils
import com.randallengineering.finances.domain.model.Budget
import com.randallengineering.finances.domain.model.BudgetCategoryType
import com.randallengineering.finances.domain.model.MainCategoryBudgetGroup
import com.randallengineering.finances.domain.model.Transaction
import kotlin.math.abs

data class BudgetCalculationResult(
    val calculatedBudgets: List<Budget>,
    val mainCategoryGroups: List<MainCategoryBudgetGroup>,
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
        incomeCategory: String = "Income",
        startOfMonthEpoch: Long = DateUtils.getStartOfCurrentMonthEpochSeconds(),
        endOfMonthEpoch: Long = DateUtils.getEndOfCurrentMonthEpochSeconds(),
        daysRemaining: Int = DateUtils.getDaysRemainingInCurrentMonth()
    ): BudgetCalculationResult {
        // Filter transactions for current calendar month
        val mtdTransactions = transactions.filter {
            it.postedEpochSeconds in startOfMonthEpoch..endOfMonthEpoch
        }

        // Calculate total MTD Income (transactions tagged with designated incomeCategory or positive amounts)
        val totalMtdIncome = mtdTransactions
            .filter { it.category.equals(incomeCategory, ignoreCase = true) || it.amount > 0 }
            .sumOf { abs(it.amount) }

        // Calculate spend per subcategory key ("Category|Subcategory") and per main category ("Category")
        val spendBySubKey = mutableMapOf<String, Double>()
        val spendByMainKey = mutableMapOf<String, Double>()
        var totalMtdExpense = 0.0

        for (tx in mtdTransactions) {
            val isIncomeTx = tx.category.equals(incomeCategory, ignoreCase = true) || tx.amount > 0
            if (isIncomeTx) continue

            if (tx.isSplit) {
                for (split in tx.splits) {
                    val isIncomeSplit = split.category.equals(incomeCategory, ignoreCase = true) || split.amount > 0
                    if (!isIncomeSplit) {
                        val amount = abs(split.amount)
                        val mainKey = split.category.trim().lowercase()
                        val subKey = "$mainKey|${split.subCategory.trim().lowercase()}"

                        spendBySubKey[subKey] = (spendBySubKey[subKey] ?: 0.0) + amount
                        spendByMainKey[mainKey] = (spendByMainKey[mainKey] ?: 0.0) + amount
                        totalMtdExpense += amount
                    }
                }
            } else {
                val amount = abs(tx.amount)
                val mainKey = tx.category.trim().lowercase()
                val subKey = "$mainKey|${tx.subCategory.trim().lowercase()}"

                spendBySubKey[subKey] = (spendBySubKey[subKey] ?: 0.0) + amount
                spendByMainKey[mainKey] = (spendByMainKey[mainKey] ?: 0.0) + amount
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

            val mainKey = budget.category.trim().lowercase()
            val subKey = "$mainKey|${budget.subCategory.trim().lowercase()}"

            val spent = if (budget.subCategory.isNotBlank()) {
                spendBySubKey[subKey] ?: 0.0
            } else {
                spendByMainKey[mainKey] ?: 0.0
            }

            val pacing = if (targetAmount > 0) {
                (spent / targetAmount) * 100.0
            } else if (spent > 0) {
                100.0
            } else {
                0.0
            }

            budget.copy(
                targetAmount = targetAmount,
                spentAmount = spent,
                pacingPercent = pacing
            )
        }

        // Group into MainCategoryBudgetGroup
        val grouped = calculatedBudgets.groupBy { it.category.trim() }
        val mainCategoryGroups = grouped.map { (mainCat, subList) ->
            val totalTarget = subList.sumOf { it.targetAmount }
            val totalSpent = subList.sumOf { it.spentAmount }
            MainCategoryBudgetGroup(
                mainCategory = mainCat,
                totalTargetAmount = totalTarget,
                totalSpentAmount = totalSpent,
                subBudgets = subList
            )
        }

        val monthlyVariableTarget = calculatedBudgets.sumOf { it.targetAmount }
        val mtdVariableSpent = calculatedBudgets.sumOf { it.spentAmount }

        val safeDays = daysRemaining.coerceAtLeast(1)
        val remainingVariableBudget = (monthlyVariableTarget - mtdVariableSpent).coerceAtLeast(0.0)
        val targetDailyAllowance = remainingVariableBudget / safeDays

        // Anomaly alerts (over 120% pacing)
        val anomalies = calculatedBudgets.filter { it.isAnomalyOverpacing }

        return BudgetCalculationResult(
            calculatedBudgets = calculatedBudgets,
            mainCategoryGroups = mainCategoryGroups,
            totalMtdIncome = totalMtdIncome,
            totalMtdExpense = totalMtdExpense,
            mtdVariableSpent = mtdVariableSpent,
            monthlyVariableTarget = monthlyVariableTarget,
            targetDailyAllowance = targetDailyAllowance,
            daysRemaining = daysRemaining,
            anomalies = anomalies
        )
    }
}
