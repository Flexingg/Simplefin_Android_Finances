package com.randallengineering.finances.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class BudgetCategoryType {
    FIXED,           // Plain monthly $ amount (Default)
    VARIABLE,        // Variable spending
    PERCENT_INCOME   // % of monthly income
}

@Serializable
data class Budget(
    val id: String,
    val category: String,                   // Main Category (e.g. Housing, Dining)
    val subCategory: String = "",           // Subcategory (e.g. Mortgage, Fast Food)
    val categoryType: BudgetCategoryType = BudgetCategoryType.FIXED,
    val targetAmount: Double = 0.0,         // Target $ limit per month
    val incomePercentage: Double? = null,   // e.g. 15.0 for 15% of monthly income
    val spentAmount: Double = 0.0,
    val pacingPercent: Double = 0.0
) {
    val displayName: String
        get() = if (subCategory.isNotBlank()) "$category > $subCategory" else category

    val remainingAmount: Double
        get() = (targetAmount - spentAmount).coerceAtLeast(0.0)

    val isOverBudget: Boolean
        get() = spentAmount > targetAmount && targetAmount > 0

    val isAnomalyOverpacing: Boolean
        get() = pacingPercent >= 120.0
}

data class MainCategoryBudgetGroup(
    val mainCategory: String,
    val totalTargetAmount: Double,
    val totalSpentAmount: Double,
    val subBudgets: List<Budget>
) {
    val remainingAmount: Double
        get() = (totalTargetAmount - totalSpentAmount).coerceAtLeast(0.0)

    val isOverBudget: Boolean
        get() = totalSpentAmount > totalTargetAmount && totalTargetAmount > 0

    val healthPercent: Float
        get() = if (totalTargetAmount > 0) {
            ((totalTargetAmount - totalSpentAmount) / totalTargetAmount).toFloat().coerceIn(0f, 1f)
        } else 1f
}
