package com.randallengineering.finances.domain.usecase

import com.randallengineering.finances.domain.model.RetirementInputs
import com.randallengineering.finances.domain.model.RetirementProjectionResult
import com.randallengineering.finances.domain.model.YearlyProjectionPoint
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.pow

class RetirementCalculatorUseCase {

    fun calculate(inputs: RetirementInputs): RetirementProjectionResult {
        val currentAge = inputs.currentAge
        val retirementAge = max(currentAge, inputs.retirementAge)
        val lifeExpectancyAge = max(retirementAge + 1, inputs.lifeExpectancyAge)
        val yearsToRetire = retirementAge - currentAge

        val nominalReturn = inputs.expectedAnnualReturnPercent / 100.0
        val inflation = inputs.expectedAnnualInflationPercent / 100.0

        // Exact real rate of return (Fisher formula)
        val realAnnualReturn = if (1.0 + inflation > 0.0) {
            ((1.0 + nominalReturn) / (1.0 + inflation)) - 1.0
        } else {
            nominalReturn - inflation
        }

        // Monthly rates
        val monthlyNominalRate = if (nominalReturn > -1.0) {
            (1.0 + nominalReturn).pow(1.0 / 12.0) - 1.0
        } else 0.0

        val monthlyRealRate = if (realAnnualReturn > -1.0) {
            (1.0 + realAnnualReturn).pow(1.0 / 12.0) - 1.0
        } else 0.0

        // Target FIRE number in today's dollars (Desired Annual Spend / SWR)
        val swr = max(0.005, inputs.safeWithdrawalRatePercent / 100.0)
        val targetFireNumber = inputs.desiredAnnualRetirementSpend / swr

        // Coast FIRE: The amount required today to reach targetFireNumber without further additions
        val coastFireNumber = if (yearsToRetire > 0 && realAnnualReturn > -1.0) {
            targetFireNumber / (1.0 + realAnnualReturn).pow(yearsToRetire.toDouble())
        } else {
            targetFireNumber
        }

        val totalMonths = yearsToRetire * 12
        val currentPv = max(0.0, inputs.currentSavings)
        val pmt = max(0.0, inputs.monthlyContribution)

        // Calculate Future Value at Retirement
        val projectedNestEggReal: Double
        val projectedNestEggNominal: Double

        if (totalMonths == 0) {
            projectedNestEggReal = currentPv
            projectedNestEggNominal = currentPv
        } else {
            // Real FV
            projectedNestEggReal = if (monthlyRealRate == 0.0) {
                currentPv + (pmt * totalMonths)
            } else {
                val compFactor = (1.0 + monthlyRealRate).pow(totalMonths.toDouble())
                (currentPv * compFactor) + (pmt * (compFactor - 1.0) / monthlyRealRate)
            }

            // Nominal FV
            projectedNestEggNominal = if (monthlyNominalRate == 0.0) {
                currentPv + (pmt * totalMonths)
            } else {
                val compFactor = (1.0 + monthlyNominalRate).pow(totalMonths.toDouble())
                (currentPv * compFactor) + (pmt * (compFactor - 1.0) / monthlyNominalRate)
            }
        }

        // Required monthly contribution to reach targetFireNumber exactly
        val requiredMonthlyContribution: Double = if (totalMonths <= 0) {
            0.0
        } else {
            val compFactor = (1.0 + monthlyRealRate).pow(totalMonths.toDouble())
            val futureFromCurrentPv = currentPv * compFactor
            val neededFromPmt = targetFireNumber - futureFromCurrentPv
            if (neededFromPmt <= 0.0) {
                0.0
            } else if (monthlyRealRate == 0.0) {
                neededFromPmt / totalMonths
            } else {
                neededFromPmt * monthlyRealRate / (compFactor - 1.0)
            }
        }

        val surplusOrShortfallReal = projectedNestEggReal - targetFireNumber
        val safeMonthlyRetirementIncomeReal = (projectedNestEggReal * swr) / 12.0
        val desiredMonthlyRetirementSpend = inputs.desiredAnnualRetirementSpend / 12.0
        val isCoastFireAchieved = currentPv >= coastFireNumber
        val isOnTrackForRetirement = surplusOrShortfallReal >= 0.0
        val monthlySavingsGap = requiredMonthlyContribution - pmt

        // Generate Year-by-Year Projection Series
        val projectionPoints = mutableListOf<YearlyProjectionPoint>()
        val startYear = LocalDate.now().year

        var runningRealBalance = currentPv
        var runningNominalBalance = currentPv
        var runningCumulativeContributions = currentPv

        for (age in currentAge..lifeExpectancyAge) {
            val yearOffset = age - currentAge
            val currentYear = startYear + yearOffset
            val isRetirement = age >= retirementAge

            if (yearOffset > 0) {
                if (!isRetirement) {
                    // Accumulation phase: 12 monthly contributions + compounding
                    val annualContributions = pmt * 12.0
                    runningCumulativeContributions += annualContributions

                    // Compound real
                    val compFactorReal = (1.0 + monthlyRealRate).pow(12.0)
                    runningRealBalance = if (monthlyRealRate == 0.0) {
                        runningRealBalance + annualContributions
                    } else {
                        (runningRealBalance * compFactorReal) + (pmt * (compFactorReal - 1.0) / monthlyRealRate)
                    }

                    // Compound nominal
                    val compFactorNominal = (1.0 + monthlyNominalRate).pow(12.0)
                    runningNominalBalance = if (monthlyNominalRate == 0.0) {
                        runningNominalBalance + annualContributions
                    } else {
                        (runningNominalBalance * compFactorNominal) + (pmt * (compFactorNominal - 1.0) / monthlyNominalRate)
                    }
                } else {
                    // Decumulation / Retirement phase: Withdraw desired annual spend in real terms
                    runningRealBalance = max(0.0, (runningRealBalance * (1.0 + realAnnualReturn)) - inputs.desiredAnnualRetirementSpend)
                    runningNominalBalance = max(0.0, (runningNominalBalance * (1.0 + nominalReturn)) - (inputs.desiredAnnualRetirementSpend * (1.0 + inflation).pow(yearOffset.toDouble())))
                }
            }

            val investmentGrowth = max(0.0, runningRealBalance - runningCumulativeContributions)

            projectionPoints.add(
                YearlyProjectionPoint(
                    age = age,
                    year = currentYear,
                    portfolioNominal = runningNominalBalance,
                    portfolioReal = runningRealBalance,
                    totalContributions = runningCumulativeContributions,
                    totalInvestmentGrowth = investmentGrowth,
                    isRetirementPhase = isRetirement
                )
            )
        }

        return RetirementProjectionResult(
            inputs = inputs,
            targetFireNumber = targetFireNumber,
            projectedNestEggAtRetirementReal = projectedNestEggReal,
            projectedNestEggAtRetirementNominal = projectedNestEggNominal,
            safeMonthlyRetirementIncomeReal = safeMonthlyRetirementIncomeReal,
            desiredMonthlyRetirementSpend = desiredMonthlyRetirementSpend,
            coastFireNumber = coastFireNumber,
            isCoastFireAchieved = isCoastFireAchieved,
            isOnTrackForRetirement = isOnTrackForRetirement,
            surplusOrShortfallReal = surplusOrShortfallReal,
            requiredMonthlyContribution = requiredMonthlyContribution,
            monthlySavingsGap = monthlySavingsGap,
            yearlyProjections = projectionPoints
        )
    }
}
