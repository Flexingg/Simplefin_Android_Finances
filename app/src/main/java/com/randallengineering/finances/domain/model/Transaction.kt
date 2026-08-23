package com.randallengineering.finances.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class TransactionSplit(
    val id: String,
    val category: String,
    val subCategory: String = "",
    val amount: Double,
    val notes: String = ""
)

@Serializable
data class Transaction(
    val id: String,
    val accountId: String = "",
    val postedEpochSeconds: Long,
    val amount: Double,
    val originalDesc: String,
    val payee: String = "",
    val category: String = "Uncategorized",
    val subCategory: String = "",
    val notes: String = "",
    val pending: Boolean = false,
    val splits: List<TransactionSplit> = emptyList(),
    val receiptUrls: List<String> = emptyList(),
    val matchedRuleId: String? = null
) {
    val isSplit: Boolean
        get() = splits.isNotEmpty()

    val displayCategory: String
        get() = if (isSplit) "Split (${splits.size})" else category

    val isExpense: Boolean
        get() = amount < 0

    val isIncome: Boolean
        get() = amount > 0
}
