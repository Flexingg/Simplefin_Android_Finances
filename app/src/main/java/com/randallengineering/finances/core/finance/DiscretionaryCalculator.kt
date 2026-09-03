package com.randallengineering.finances.core.finance

import com.randallengineering.finances.domain.model.Transaction
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * Pure, testable logic for the discretionary-spending setpoint tracker.
 *
 * Monthly "fun money" allowance: discretionary spend for the current month is the
 * sum of every expense (amount < 0) whose category is NOT flagged "Necessary",
 * with internal transfers excluded. Everything else — income and necessary spend —
 * doesn't count. Remaining = setpoint - spend.
 */
object DiscretionaryCalculator {

    data class CategoryRow(
        val name: String,
        val discretionary: Boolean,
        val spent: Double
    )

    data class Summary(
        val setpoint: Double,
        val monthlySpend: Double,
        val remaining: Double,
        val categories: List<CategoryRow>
    )

    fun monthStartEpoch(now: LocalDate = LocalDate.now()): Long =
        now.with(TemporalAdjusters.firstDayOfMonth())
            .atStartOfDay(ZoneId.systemDefault())
            .toEpochSecond()

    fun summary(
        transactions: List<Transaction>,
        necessaryCategories: Set<String>,
        setpoint: Double,
        monthStart: Long = monthStartEpoch()
    ): Summary {
        val transferIds = TransferDetection.detectTransferIds(transactions)
        val necessary = necessaryCategories.filter { it.isNotBlank() }.toSet()

        val monthSpend = transactions.filter { tx ->
            tx.amount < 0 &&
                tx.postedEpochSeconds >= monthStart &&
                tx.id !in transferIds &&
                tx.category !in necessary
        }

        val spendByCategory = monthSpend
            .groupBy { tx -> tx.category.ifBlank { "Uncategorized" } }
            .map { (cat, txs) -> CategoryRow(cat, cat !in necessary, txs.sumOf { -it.amount }) }
            .sortedByDescending { it.spent }

        val total = spendByCategory.sumOf { it.spent }
        return Summary(setpoint = setpoint, monthlySpend = total, remaining = setpoint - total, categories = spendByCategory)
    }
}
