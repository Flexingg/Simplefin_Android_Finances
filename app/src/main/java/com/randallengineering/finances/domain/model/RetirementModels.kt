package com.randallengineering.finances.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class RetirementInputs(
    val currentAge: Int = 30,
    val retirementAge: Int = 65,
    val lifeExpectancyAge: Int = 85,
    val currentSavings: Double = 25000.0,
    val monthlyContribution: Double = 500.0,
    val expectedAnnualReturnPercent: Double = 7.0,
    val expectedAnnualInflationPercent: Double = 2.5,
    val desiredAnnualRetirementSpend: Double = 48000.0, // in today's dollars
    val safeWithdrawalRatePercent: Double = 4.0
) {
    val yearsToRetirement: Int
        get() = (retirementAge - currentAge).coerceAtLeast(0)

    val yearsInRetirement: Int
        get() = (lifeExpectancyAge - retirementAge).coerceAtLeast(1)
}

@Serializable
data class YearlyProjectionPoint(
    val age: Int,
    val year: Int,
    val portfolioNominal: Double,
    val portfolioReal: Double,             // In today's dollars (inflation-adjusted)
    val totalContributions: Double,
    val totalInvestmentGrowth: Double,
    val isRetirementPhase: Boolean
)

@Serializable
data class RetirementProjectionResult(
    val inputs: RetirementInputs,
    val targetFireNumber: Double,             // Target portfolio needed in today's dollars (Desired Spend / SWR)
    val projectedNestEggAtRetirementReal: Double, // Projected nest egg in today's dollars
    val projectedNestEggAtRetirementNominal: Double, // Projected nest egg in future nominal dollars
    val safeMonthlyRetirementIncomeReal: Double,    // Safe monthly withdrawal in today's dollars
    val desiredMonthlyRetirementSpend: Double,       // Monthly equivalent of desired annual spend
    val coastFireNumber: Double,              // Present value needed today so compound growth hits FIRE number at retirement with $0 added
    val isCoastFireAchieved: Boolean,         // Current savings >= coastFireNumber
    val isOnTrackForRetirement: Boolean,      // Projected nest egg >= target FIRE number
    val surplusOrShortfallReal: Double,       // Positive = surplus, Negative = shortfall in today's dollars
    val requiredMonthlyContribution: Double,  // Monthly saving required from today to hit the exact target FIRE number
    val monthlySavingsGap: Double,            // Difference: requiredMonthlyContribution - inputs.monthlyContribution
    val yearlyProjections: List<YearlyProjectionPoint> = emptyList()
)
