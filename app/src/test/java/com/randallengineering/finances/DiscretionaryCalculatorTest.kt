package com.randallengineering.finances

import com.randallengineering.finances.core.finance.DiscretionaryCalculator
import com.randallengineering.finances.domain.model.Transaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscretionaryCalculatorTest {

    private fun tx(id: String, amount: Double, category: String, epoch: Long, account: String = "a") = Transaction(
        id = id, amount = amount, category = category, postedEpochSeconds = epoch,
        originalDesc = "", payee = "", accountId = account
    )

    private val t0 = 1_000_000_000L // fixed start-of-month (well in the past but stable for the test)
    private val later = t0 + 86_400L

    @Test
    fun `default all spend is discretionary`() {
        val txs = listOf(tx("1", -20.0, "Dining", t0), tx("2", -5.0, "Shopping", t0))
        val s = DiscretionaryCalculator.summary(txs, emptySet(), 100.0, t0)
        assertEquals(25.0, s.monthlySpend, 0.001)
        assertEquals(75.0, s.remaining, 0.001)
        assertTrue(s.categories.all { it.discretionary })
    }

    @Test
    fun `necessary categories are excluded`() {
        val txs = listOf(
            tx("1", -1200.0, "Rent", t0),
            tx("2", -30.0, "Dining", t0),
            tx("3", -60.0, "Utilities", t0)
        )
        val s = DiscretionaryCalculator.summary(txs, setOf("Rent", "Utilities"), 300.0, t0)
        assertEquals(30.0, s.monthlySpend, 0.001)
        assertEquals(270.0, s.remaining, 0.001)
    }

    @Test
    fun `internal transfers and income do not count`() {
        // Transfer: -500 out of checking -> +500 into savings (different accounts)
        val transferOut = tx("o", -500.0, "Checking", t0, "chk")
        val transferIn = tx("i", 500.0, "Savings", t0, "sav")
        val salary = tx("s", 3000.0, "Income", t0)
        val coffee = tx("c", -4.0, "Dining", t0)
        val s = DiscretionaryCalculator.summary(listOf(transferOut, transferIn, salary, coffee), emptySet(), 50.0, t0)
        assertEquals(4.0, s.monthlySpend, 0.001)
        assertEquals(46.0, s.remaining, 0.001)
    }

    @Test
    fun `only current-month transactions count`() {
        val lastMonth = tx("1", -100.0, "Dining", t0 - 3_000_000L)
        val thisMonth = tx("2", -10.0, "Dining", later)
        val s = DiscretionaryCalculator.summary(listOf(lastMonth, thisMonth), emptySet(), 50.0, t0)
        assertEquals(10.0, s.monthlySpend, 0.001)
    }
}
