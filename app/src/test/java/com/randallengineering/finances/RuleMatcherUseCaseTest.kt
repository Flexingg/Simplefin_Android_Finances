package com.randallengineering.finances

import com.randallengineering.finances.domain.model.Rule
import com.randallengineering.finances.domain.model.Transaction
import com.randallengineering.finances.domain.usecase.RuleMatcherUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RuleMatcherUseCaseTest {

    private val ruleMatcherUseCase = RuleMatcherUseCase()

    @Test
    fun `test case-insensitive regex matching on originalDesc`() = runTest {
        val rules = listOf(
            Rule(
                id = "rule1",
                name = "Target Rule",
                priority = 1,
                pattern = "(?i).*target.*",
                category = "Shopping",
                subCategory = "General"
            )
        )

        val tx1 = Transaction(
            id = "tx1",
            postedEpochSeconds = 1740000000L,
            amount = -45.50,
            originalDesc = "TARGET STORE #0842 SAN FRANCISCO"
        )

        val matched = ruleMatcherUseCase.matchAndCategorize(tx1, rules)

        assertEquals("Shopping", matched.category)
        assertEquals("General", matched.subCategory)
        assertEquals("rule1", matched.matchedRuleId)
    }

    @Test
    fun `test priority ordering picks lowest priority number first`() = runTest {
        val rules = listOf(
            Rule(
                id = "rule_low_prio",
                name = "General Food",
                priority = 10,
                pattern = "(?i).*grocery.*",
                category = "Food",
                subCategory = "General"
            ),
            Rule(
                id = "rule_high_prio",
                name = "Trader Joe's",
                priority = 1,
                pattern = "(?i).*trader joe.*",
                category = "Groceries",
                subCategory = "Trader Joe's"
            )
        )

        val tx = Transaction(
            id = "tx2",
            postedEpochSeconds = 1740000000L,
            amount = -82.10,
            originalDesc = "TRADER JOES GROCERY MARKET"
        )

        val matched = ruleMatcherUseCase.matchAndCategorize(tx, rules)

        assertEquals("Groceries", matched.category)
        assertEquals("Trader Joe's", matched.subCategory)
        assertEquals("rule_high_prio", matched.matchedRuleId)
    }

    @Test
    fun `test amount range boundary filters out non-matching amounts`() = runTest {
        val rules = listOf(
            Rule(
                id = "rule_small_coffee",
                name = "Small Coffee",
                priority = 1,
                pattern = "(?i).*starbucks.*",
                minAmount = -10.0,
                maxAmount = -1.0,
                category = "Dining",
                subCategory = "Coffee"
            )
        )

        // Expense of $50 (beyond max of -10.0 in absolute terms or within numerical order)
        // Amount is -50.0 (less than minAmount -10.0)
        val expensiveTx = Transaction(
            id = "tx3",
            postedEpochSeconds = 1740000000L,
            amount = -50.00,
            originalDesc = "STARBUCKS BULK MERCHANDISE"
        )

        val result = ruleMatcherUseCase.matchAndCategorize(expensiveTx, rules)
        assertEquals("Uncategorized", result.category)
        assertNull(result.matchedRuleId)
    }
}
