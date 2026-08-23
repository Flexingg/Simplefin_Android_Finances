package com.randallengineering.finances

import com.randallengineering.finances.core.util.AiSnapshotFormatter
import com.randallengineering.finances.domain.model.Budget
import com.randallengineering.finances.domain.model.BudgetCategoryType
import com.randallengineering.finances.domain.model.Goal
import com.randallengineering.finances.domain.model.SimpleFinAccount
import com.randallengineering.finances.domain.model.Transaction
import com.randallengineering.finances.domain.usecase.AiAdvisorUseCase
import com.randallengineering.finances.domain.usecase.BudgetCalculatorUseCase
import com.randallengineering.finances.domain.usecase.GoalPacingUseCase
import org.junit.Assert.assertTrue
import org.junit.Test

class AiAdvisorUseCaseTest {

    private val budgetCalculator = BudgetCalculatorUseCase()
    private val goalPacing = GoalPacingUseCase()
    private val aiAdvisor = AiAdvisorUseCase(budgetCalculator, goalPacing)

    @Test
    fun `test compile snapshot and serialize to markdown and json`() {
        val accounts = listOf(
            SimpleFinAccount(id = "acc1", name = "Primary Checking", balance = 3200.0, orgName = "Chase")
        )

        val budgets = listOf(
            Budget(id = "b1", category = "Dining", categoryType = BudgetCategoryType.VARIABLE, targetAmount = 300.0)
        )

        val goals = listOf(
            Goal(id = "g1", title = "Emergency Fund", targetAmount = 5000.0, currentAmount = 2500.0, targetEpochSeconds = 1750000000L)
        )

        val transactions = listOf(
            Transaction(id = "tx1", postedEpochSeconds = 1740000000L, amount = -150.0, originalDesc = "LOCAL BISTRO", category = "Dining")
        )

        val snapshot = aiAdvisor.compileAiSnapshot(
            accounts = accounts,
            budgets = budgets,
            goals = goals,
            transactions = transactions
        )

        val markdown = AiSnapshotFormatter.toMarkdown(snapshot)
        val json = AiSnapshotFormatter.toJson(snapshot)

        assertTrue(markdown.contains("Financial Health & Context Snapshot"))
        assertTrue(markdown.contains("Primary Checking"))
        assertTrue(markdown.contains("Emergency Fund"))
        assertTrue(markdown.contains("LOCAL BISTRO"))

        assertTrue(json.contains("\"totalCashBalance\": 3200.0"))
        assertTrue(json.contains("\"Emergency Fund\""))
    }
}
