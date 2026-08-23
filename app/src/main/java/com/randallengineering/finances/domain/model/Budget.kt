package com.randallengineering.finances.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class BudgetCategoryType {
    FIXED,
    VARIABLE,
    PERCENT_INCOME
}

@Serializable
data class Budget(
    val id: String,
    val category: String,
    val categoryType: BudgetCategoryType = BudgetCategoryType.VARIABLE,
    val targetAmount: Double = 0.0,
    val incomePercentage: Double? = null, // e.g. 10.0 for 10%
    val spentAmount: Double = 0.0,
    val pacingPercent: Double = 0.0
) {
    val remainingAmount: Double
        get() = (targetAmount - spentAmount).coerceAtLeast(0.0)

    val isOverBudget: Boolean
        get() = spentAmount > targetAmount && targetAmount > 0

    val isAnomalyOverpacing: Boolean
        get() = pacingPercent >= 120.0
}
