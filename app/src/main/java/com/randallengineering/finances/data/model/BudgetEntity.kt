package com.randallengineering.finances.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.randallengineering.finances.domain.model.Budget
import com.randallengineering.finances.domain.model.BudgetCategoryType

@IgnoreExtraProperties
data class BudgetEntity(
    @DocumentId
    var id: String = "",
    var category: String = "",
    var subCategory: String = "",
    var categoryType: String = BudgetCategoryType.FIXED.name,
    var targetAmount: Double = 0.0,
    var incomePercentage: Double? = null,
    var spentAmount: Double = 0.0,
    var pacingPercent: Double = 0.0,
    var rolloverEnabled: Boolean = false,
    var rolloverResetMonths: List<String> = emptyList()
) {
    fun toDomain(): Budget = Budget(
        id = id,
        category = category,
        subCategory = subCategory,
        categoryType = try {
            BudgetCategoryType.valueOf(categoryType)
        } catch (e: Exception) {
            BudgetCategoryType.FIXED
        },
        targetAmount = targetAmount,
        incomePercentage = incomePercentage,
        spentAmount = spentAmount,
        pacingPercent = pacingPercent,
        rolloverEnabled = rolloverEnabled,
        rolloverResetMonths = rolloverResetMonths
    )

    companion object {
        fun fromDomain(domain: Budget): BudgetEntity = BudgetEntity(
            id = domain.id,
            category = domain.category,
            subCategory = domain.subCategory,
            categoryType = domain.categoryType.name,
            targetAmount = domain.targetAmount,
            incomePercentage = domain.incomePercentage,
            spentAmount = domain.spentAmount,
            pacingPercent = domain.pacingPercent,
            rolloverEnabled = domain.rolloverEnabled,
            rolloverResetMonths = domain.rolloverResetMonths
        )
    }
}
