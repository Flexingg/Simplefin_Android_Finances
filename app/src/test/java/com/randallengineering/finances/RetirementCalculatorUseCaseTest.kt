package com.randallengineering.finances

import com.randallengineering.finances.domain.model.RetirementInputs
import com.randallengineering.finances.domain.usecase.RetirementCalculatorUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RetirementCalculatorUseCaseTest {

    private val useCase = RetirementCalculatorUseCase()

    @Test
    fun `test target FIRE calculation with 4 percent rule`() {
        val inputs = RetirementInputs(
            desiredAnnualRetirementSpend = 40000.0,
            safeWithdrawalRatePercent = 4.0
        )
        val result = useCase.calculate(inputs)
        // $40,000 / 0.04 = $1,000,000
        assertEquals(1000000.0, result.targetFireNumber, 0.01)
    }

    @Test
    fun `test compound growth and future value calculation`() {
        val inputs = RetirementInputs(
            currentAge = 30,
            retirementAge = 60, // 30 years
            currentSavings = 10000.0,
            monthlyContribution = 1000.0,
            expectedAnnualReturnPercent = 8.0,
            expectedAnnualInflationPercent = 3.0, // Real return ~4.854%
            desiredAnnualRetirementSpend = 50000.0,
            safeWithdrawalRatePercent = 4.0
        )
        val result = useCase.calculate(inputs)

        assertTrue("Projected real nest egg should exceed initial savings + sum of contributions",
            result.projectedNestEggAtRetirementReal > (10000.0 + (1000.0 * 360))
        )
        assertTrue("Nominal nest egg must exceed real nest egg due to positive inflation",
            result.projectedNestEggAtRetirementNominal > result.projectedNestEggAtRetirementReal
        )
        assertEquals(inputs.lifeExpectancyAge - inputs.currentAge + 1, result.yearlyProjections.size)
    }

    @Test
    fun `test Coast FIRE threshold detection`() {
        val inputsAchieved = RetirementInputs(
            currentAge = 25,
            retirementAge = 65,
            currentSavings = 500000.0,
            monthlyContribution = 0.0,
            expectedAnnualReturnPercent = 7.0,
            expectedAnnualInflationPercent = 2.5,
            desiredAnnualRetirementSpend = 40000.0,
            safeWithdrawalRatePercent = 4.0
        )
        val resultAchieved = useCase.calculate(inputsAchieved)
        assertTrue(resultAchieved.isCoastFireAchieved)

        val inputsNotAchieved = inputsAchieved.copy(currentSavings = 5000.0)
        val resultNotAchieved = useCase.calculate(inputsNotAchieved)
        assertFalse(resultNotAchieved.isCoastFireAchieved)
    }
}
