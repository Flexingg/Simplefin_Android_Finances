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
    var categoryType: String = BudgetCategoryType.VARIABLE.name,
    var targetAmount: Double = 0.0,
    var incomePercentage: Double? = null,
    var spentAmount: Double = 0.0,
    var pacingPercent: Double = 0.0
) {
    fun toDomain(): Budget = Budget(
        id = id,
        category = category,
        categoryType = try {
            BudgetCategoryType.valueOf(categoryType)
        } catch (e: Exception) {
            BudgetCategoryType.VARIABLE
        },
        targetAmount = targetAmount,
        incomePercentage = incomePercentage,
        spentAmount = spentAmount,
        pacingPercent = pacingPercent
    )

    companion object {
        fun fromDomain(domain: Budget): BudgetEntity = BudgetEntity(
            id = domain.id,
            category = domain.category,
            categoryType = domain.categoryType.name,
            targetAmount = domain.targetAmount,
            incomePercentage = domain.incomePercentage,
            spentAmount = domain.spentAmount,
            pacingPercent = domain.pacingPercent
        )
    }
}
