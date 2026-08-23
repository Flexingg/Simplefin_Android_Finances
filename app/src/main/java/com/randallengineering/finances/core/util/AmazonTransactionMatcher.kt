package com.randallengineering.finances.core.util

import com.randallengineering.finances.data.repository.TransactionRepository
import com.randallengineering.finances.domain.model.AmazonOrderItem
import com.randallengineering.finances.domain.model.MatchedAmazonOrder
import com.randallengineering.finances.domain.model.Transaction
import com.randallengineering.finances.domain.model.TransactionSplit
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import kotlin.math.abs

data class AmazonTransactionMatchCandidate(
    val transaction: Transaction,
    val order: MatchedAmazonOrder,
    val items: List<AmazonOrderItem>,
    val suggestedSplits: List<TransactionSplit>,
    val formattedDate: String
)

object AmazonTransactionMatcher {

    /**
     * Finds bank transactions matching the scanned Amazon order dates.
     */
    fun findMatches(
        orders: List<MatchedAmazonOrder>,
        transactions: List<Transaction>
    ): List<AmazonTransactionMatchCandidate> {
        val candidates = mutableListOf<AmazonTransactionMatchCandidate>()
        val amazonTxs = transactions.filter { tx ->
            val desc = tx.originalDesc.lowercase()
            val payee = tx.payee.lowercase()
            desc.contains("amazon") || desc.contains("amzn") || payee.contains("amazon") || payee.contains("amzn")
        }

        for (order in orders) {
            val orderDate = parseOrderDate(order.purchaseDate)
            val matchedTx = amazonTxs.find { tx ->
                val txDate = Instant.ofEpochSecond(tx.postedEpochSeconds)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                
                // Match within 3 days window
                if (orderDate != null) {
                    val daysDiff = abs(java.time.temporal.ChronoUnit.DAYS.between(orderDate, txDate))
                    daysDiff <= 3
                } else {
                    true
                }
            } ?: amazonTxs.firstOrNull()

            if (matchedTx != null) {
                val splits = generateSuggestedSplits(matchedTx.amount, order.items)
                candidates.add(
                    AmazonTransactionMatchCandidate(
                        transaction = matchedTx,
                        order = order,
                        items = order.items,
                        suggestedSplits = splits,
                        formattedDate = order.purchaseDate.ifBlank { DateUtils.formatDate(matchedTx.postedEpochSeconds) }
                    )
                )
            }
        }

        return candidates.distinctBy { it.transaction.id }
    }

    private fun generateSuggestedSplits(
        totalAmount: Double,
        items: List<AmazonOrderItem>
    ): List<TransactionSplit> {
        val count = items.size.coerceAtLeast(1)
        val targetTotal = abs(totalAmount)
        val perItemAmount = if (items.any { it.totalPrice > 0 }) {
            0.0 // Use item prices
        } else {
            targetTotal / count
        }

        return items.mapIndexed { index, item ->
            val suggested = AmazonCategorySuggester.suggestCategory(item.title)
            val itemAmount = if (item.totalPrice > 0) item.totalPrice else perItemAmount
            TransactionSplit(
                id = UUID.randomUUID().toString(),
                category = suggested.category,
                subCategory = suggested.subCategory,
                amount = itemAmount,
                notes = item.title
            )
        }
    }

    private fun parseOrderDate(raw: String): LocalDate? {
        val clean = raw.trim()
        val currentYear = LocalDate.now().year

        val patterns = listOf(
            DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.US),
            DateTimeFormatter.ofPattern("MMMM d", Locale.US),
            DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US),
            DateTimeFormatter.ofPattern("MMM d", Locale.US),
            DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
        )

        for (fmt in patterns) {
            try {
                if (fmt.toString().contains("yyyy")) {
                    return LocalDate.parse(clean, fmt)
                } else {
                    val temporal = fmt.parse(clean)
                    val month = temporal.get(java.time.temporal.ChronoField.MONTH_OF_YEAR)
                    val day = temporal.get(java.time.temporal.ChronoField.DAY_OF_MONTH)
                    return LocalDate.of(currentYear, month, day)
                }
            } catch (e: Exception) {
                // Try next pattern
            }
        }
        return null
    }

    /**
     * Applies the matched items, splits, and categories to the transaction and saves it.
     */
    suspend fun applyMatchToTransaction(
        candidate: AmazonTransactionMatchCandidate,
        transactionRepository: TransactionRepository
    ) {
        val tx = candidate.transaction
        val items = candidate.items
        val splits = candidate.suggestedSplits

        val itemSummary = items.joinToString(", ") { it.title }
        val primaryCategory = splits.firstOrNull()?.category ?: "Shopping"
        val primarySubCategory = splits.firstOrNull()?.subCategory ?: "Amazon Retail"

        val updatedTx = tx.copy(
            category = primaryCategory,
            subCategory = primarySubCategory,
            notes = if (tx.notes.isNotBlank()) "${tx.notes} | Items: $itemSummary" else "Amazon Items: $itemSummary",
            splits = if (splits.size > 1) splits else emptyList()
        )

        transactionRepository.saveTransaction(updatedTx)
    }
}
