package com.randallengineering.finances

import com.randallengineering.finances.domain.model.Budget
import com.randallengineering.finances.domain.model.BudgetCategoryType
import com.randallengineering.finances.domain.model.Transaction
import com.randallengineering.finances.domain.usecase.BudgetCalculatorUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BudgetCalculatorUseCaseTest {

    private val budgetCalculatorUseCase = BudgetCalculatorUseCase()

    @Test
    fun `test formula budget dynamically calculates from monthly income`() {
        val budgets = listOf(
            Budget(
                id = "b1",
                category = "Tithe",
                categoryType = BudgetCategoryType.PERCENT_INCOME,
                incomePercentage = 10.0 // 10%
            ),
            Budget(
                id = "b2",
                category = "Groceries",
                categoryType = BudgetCategoryType.VARIABLE,
                targetAmount = 600.0
            )
        )

        val transactions = listOf(
            // Income transaction of $5,000
            Transaction(
                id = "tx1",
                postedEpochSeconds = 1000L,
                amount = 5000.0,
                originalDesc = "EMPLOYER PAYROLL DIRECT DEP",
                category = "Income"
            ),
            // Groceries expense of $300
            Transaction(
                id = "tx2",
                postedEpochSeconds = 1010L,
                amount = -300.0,
                originalDesc = "SAFEWAY #1234",
                category = "Groceries"
            )
        )

        val result = budgetCalculatorUseCase.calculate(
            budgets = budgets,
            transactions = transactions,
            startOfMonthEpoch = 0L,
            endOfMonthEpoch = 5000L,
            daysRemaining = 15
        )

        assertEquals(5000.0, result.totalMtdIncome, 0.001)

        val titheBudget = result.calculatedBudgets.find { it.category == "Tithe" }!!
        // 10% of 5000.0 = 500.0
        assertEquals(500.0, titheBudget.targetAmount, 0.001)

        val groceriesBudget = result.calculatedBudgets.find { it.category == "Groceries" }!!
        assertEquals(300.0, groceriesBudget.spentAmount, 0.001)
        assertEquals(50.0, groceriesBudget.pacingPercent, 0.001) // 300 / 600 = 50%
    }

    @Test
    fun `test target daily allowance calculation`() {
        val budgets = listOf(
            Budget(
                id = "b1",
                category = "Groceries",
                categoryType = BudgetCategoryType.VARIABLE,
                targetAmount = 600.0
            ),
            Budget(
                id = "b2",
                category = "Entertainment",
                categoryType = BudgetCategoryType.VARIABLE,
                targetAmount = 200.0
            ),
            Budget(
                id = "b3",
                category = "Rent",
                categoryType = BudgetCategoryType.FIXED,
                targetAmount = 1500.0
            )
        )

        val transactions = listOf(
            Transaction(
                id = "tx1",
                postedEpochSeconds = 1000L,
                amount = -350.0,
                originalDesc = "GROCERY STORE",
                category = "Groceries"
            )
        )

        // Monthly variable target = 600 + 200 = 800. MTD variable spent = 350.
        // Remaining variable budget = 800 - 350 = 450.
        // Days remaining = 10.
        // Target Daily Allowance = 450 / 10 = 45.0/day.

        val result = budgetCalculatorUseCase.calculate(
            budgets = budgets,
            transactions = transactions,
            startOfMonthEpoch = 0L,
            endOfMonthEpoch = 5000L,
            daysRemaining = 10
        )

        assertEquals(800.0, result.monthlyVariableTarget, 0.001)
        assertEquals(350.0, result.mtdVariableSpent, 0.001)
        assertEquals(45.0, result.targetDailyAllowance, 0.001)
    }

    @Test
    fun `test anomaly overpacing flag when category exceeds 120 percent`() {
        val budgets = listOf(
            Budget(
                id = "b1",
                category = "Dining",
                categoryType = BudgetCategoryType.VARIABLE,
                targetAmount = 200.0
            )
        )

        val transactions = listOf(
            Transaction(
                id = "tx1",
                postedEpochSeconds = 1000L,
                amount = -260.0, // 260 / 200 = 130%
                originalDesc = "FANCY RESTAURANT",
                category = "Dining"
            )
        )

        val result = budgetCalculatorUseCase.calculate(
            budgets = budgets,
            transactions = transactions,
            startOfMonthEpoch = 0L,
            endOfMonthEpoch = 5000L,
            daysRemaining = 10
        )

        val diningBudget = result.calculatedBudgets.find { it.category == "Dining" }!!
        assertEquals(130.0, diningBudget.pacingPercent, 0.001)
        assertTrue(diningBudget.isAnomalyOverpacing)
        assertEquals(1, result.anomalies.size)
    }
}
