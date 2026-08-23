package com.randallengineering.finances.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.randallengineering.finances.domain.model.Goal

@IgnoreExtraProperties
data class GoalEntity(
    @DocumentId
    var id: String = "",
    var title: String = "",
    var targetAmount: Double = 0.0,
    var currentAmount: Double = 0.0,
    var targetEpochSeconds: Long = 0L,
    var category: String = "Savings",
    var isCompleted: Boolean = false
) {
    fun toDomain(): Goal = Goal(
        id = id,
        title = title,
        targetAmount = targetAmount,
        currentAmount = currentAmount,
        targetEpochSeconds = targetEpochSeconds,
        category = category,
        isCompleted = isCompleted
    )

    companion object {
        fun fromDomain(domain: Goal): GoalEntity = GoalEntity(
            id = domain.id,
            title = domain.title,
            targetAmount = domain.targetAmount,
            currentAmount = domain.currentAmount,
            targetEpochSeconds = domain.targetEpochSeconds,
            category = domain.category,
            isCompleted = domain.isCompleted
        )
    }
}
