package com.randallengineering.finances.domain.usecase

import com.randallengineering.finances.core.util.DateUtils
import com.randallengineering.finances.domain.model.Goal

data class GoalPacingResult(
    val goal: Goal,
    val daysRemaining: Long,
    val monthsRemaining: Double,
    val requiredDailySaving: Double,
    val requiredMonthlySaving: Double,
    val progressPercent: Double,
    val isPastDeadline: Boolean
)

class GoalPacingUseCase {

    fun calculatePacing(
        goal: Goal,
        currentEpochSeconds: Long = System.currentTimeMillis() / 1000
    ): GoalPacingResult {
        val days = DateUtils.getDaysBetween(currentEpochSeconds, goal.targetEpochSeconds)
        val months = DateUtils.getMonthsBetween(currentEpochSeconds, goal.targetEpochSeconds)
        val isPastDeadline = currentEpochSeconds > goal.targetEpochSeconds

        val remaining = (goal.targetAmount - goal.currentAmount).coerceAtLeast(0.0)
        val daily = if (days > 0 && !isPastDeadline) remaining / days else remaining
        val monthly = if (months > 0 && !isPastDeadline) remaining / months else remaining
        val progress = if (goal.targetAmount > 0) {
            ((goal.currentAmount / goal.targetAmount) * 100.0).coerceIn(0.0, 100.0)
        } else {
            0.0
        }

        return GoalPacingResult(
            goal = goal,
            daysRemaining = days,
            monthsRemaining = months,
            requiredDailySaving = daily,
            requiredMonthlySaving = monthly,
            progressPercent = progress,
            isPastDeadline = isPastDeadline
        )
    }

    fun calculateBatch(
        goals: List<Goal>,
        currentEpochSeconds: Long = System.currentTimeMillis() / 1000
    ): List<GoalPacingResult> {
        return goals.map { calculatePacing(it, currentEpochSeconds) }
    }
}
