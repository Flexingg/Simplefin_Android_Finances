package com.randallengineering.finances.core.finance

import com.randallengineering.finances.domain.model.Transaction
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Detects internal transfers between the user's own accounts so they can be
 * excluded from income/spending. Otherwise a transfer from checking to savings
 * shows up as an expense on one side and (wrongly) income on the other,
 * inflating both.
 *
 * Detection pairs a debit in one account with a credit of a near-equal amount
 * in a *different* own account within a short window. A leg is only excluded
 * when a matching counterpart exists — external transactions are never flagged.
 */
object TransferDetection {

    private const val AMOUNT_TOLERANCE = 2.0        // dollars
    private const val WINDOW_SECONDS = 4L * 24 * 3600  // 4 days

    fun detectTransferIds(transactions: List<Transaction>): Set<String> {
        if (transactions.size < 2) return emptySet()
        val flagged = mutableSetOf<String>()

        val credits = transactions.filter { it.amount > 0 && it.accountId.isNotBlank() }
        // Bucket credits by rounded dollar amount for O(n)-ish lookups.
        val creditByAmount = credits.groupBy { abs(it.amount).roundToLong() }

        val debits = transactions.filter { it.amount < 0 && it.accountId.isNotBlank() }
        for (debit in debits) {
            if (debit.id in flagged) continue
            val target = abs(debit.amount).roundToLong()
            // Look in the exact dollar bucket and adjacent (to absorb a few cents).
            val candidates = mutableListOf<Transaction>()
            for (bucket in listOf(target - 1, target, target + 1)) {
                creditByAmount[bucket]?.let { candidates.addAll(it) }
            }
            for (credit in candidates) {
                if (credit.id in flagged) continue
                if (credit.accountId == debit.accountId) continue
                if (abs(credit.amount) < 0.01 || abs(debit.amount) < 0.01) continue
                val amountDiff = abs(abs(credit.amount) - abs(debit.amount))
                val timeDiff = abs(credit.postedEpochSeconds - debit.postedEpochSeconds)
                if (amountDiff <= AMOUNT_TOLERANCE && timeDiff <= WINDOW_SECONDS) {
                    flagged.add(debit.id)
                    flagged.add(credit.id)
                    break
                }
            }
        }
        return flagged
    }
}
