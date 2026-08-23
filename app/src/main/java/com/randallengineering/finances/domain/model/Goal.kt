package com.randallengineering.finances.domain.model

import com.randallengineering.finances.core.util.DateUtils
import kotlinx.serialization.Serializable

@Serializable
data class Goal(
    val id: String,
    val title: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val targetEpochSeconds: Long,
    val category: String = "Savings",
    val isCompleted: Boolean = false
) {
    val remainingAmount: Double
        get() = (targetAmount - currentAmount).coerceAtLeast(0.0)

    val progressPercent: Double
        get() = if (targetAmount > 0) ((currentAmount / targetAmount) * 100.0).coerceIn(0.0, 100.0) else 0.0

    fun calculateDailyTargetSaving(currentEpochSeconds: Long = System.currentTimeMillis() / 1000): Double {
        val days = DateUtils.getDaysBetween(currentEpochSeconds, targetEpochSeconds)
        return if (days > 0) remainingAmount / days else remainingAmount
    }

    fun calculateMonthlyTargetSaving(currentEpochSeconds: Long = System.currentTimeMillis() / 1000): Double {
        val months = DateUtils.getMonthsBetween(currentEpochSeconds, targetEpochSeconds)
        return if (months > 0) remainingAmount / months else remainingAmount
    }
}
