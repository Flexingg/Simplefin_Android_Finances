package com.randallengineering.finances.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class CategorySpendSnapshot(
    val category: String,
    val spentAmount: Double,
    val budgetAmount: Double,
    val pacingPercent: Double,
    val isAnomaly: Boolean
)

@Serializable
data class GoalSnapshot(
    val title: String,
    val currentAmount: Double,
    val targetAmount: Double,
    val progressPercent: Double,
    val targetDate: String,
    val requiredDailySaving: Double,
    val requiredMonthlySaving: Double
)

@Serializable
data class TransactionSummarySnapshot(
    val date: String,
    val description: String,
    val amount: Double,
    val category: String
)

@Serializable
data class AiSnapshot(
    val generatedTimestamp: String,
    val totalCashBalance: Double,
    val accounts: List<SimpleFinAccount>,
    val mtdTotalIncome: Double,
    val mtdTotalExpense: Double,
    val mtdNetSavings: Double,
    val targetDailyAllowance: Double,
    val categorySpends: List<CategorySpendSnapshot>,
    val activeGoals: List<GoalSnapshot>,
    val recentTransactions: List<TransactionSummarySnapshot>
)
