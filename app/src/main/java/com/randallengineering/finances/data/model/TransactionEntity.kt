package com.randallengineering.finances.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.randallengineering.finances.domain.model.Transaction
import com.randallengineering.finances.domain.model.TransactionSplit

@IgnoreExtraProperties
data class TransactionSplitEntity(
    var id: String = "",
    var category: String = "",
    var subCategory: String = "",
    var amount: Double = 0.0,
    var notes: String = ""
) {
    fun toDomain(): TransactionSplit = TransactionSplit(
        id = id,
        category = category,
        subCategory = subCategory,
        amount = amount,
        notes = notes
    )

    companion object {
        fun fromDomain(domain: TransactionSplit): TransactionSplitEntity = TransactionSplitEntity(
            id = domain.id,
            category = domain.category,
            subCategory = domain.subCategory,
            amount = domain.amount,
            notes = domain.notes
        )
    }
}

@IgnoreExtraProperties
data class TransactionEntity(
    @DocumentId
    var id: String = "",
    var accountId: String = "",
    var postedEpochSeconds: Long = 0L,
    var amount: Double = 0.0,
    var originalDesc: String = "",
    var payee: String = "",
    var category: String = "Uncategorized",
    var subCategory: String = "",
    var notes: String = "",
    var pending: Boolean = false,
    var splits: List<TransactionSplitEntity> = emptyList(),
    var receiptUrls: List<String> = emptyList(),
    var matchedRuleId: String? = null
) {
    fun toDomain(): Transaction = Transaction(
        id = id,
        accountId = accountId,
        postedEpochSeconds = postedEpochSeconds,
        amount = amount,
        originalDesc = originalDesc,
        payee = payee,
        category = category,
        subCategory = subCategory,
        notes = notes,
        pending = pending,
        splits = splits.map { it.toDomain() },
        receiptUrls = receiptUrls,
        matchedRuleId = matchedRuleId
    )

    companion object {
        fun fromDomain(domain: Transaction): TransactionEntity = TransactionEntity(
            id = domain.id,
            accountId = domain.accountId,
            postedEpochSeconds = domain.postedEpochSeconds,
            amount = domain.amount,
            originalDesc = domain.originalDesc,
            payee = domain.payee,
            category = domain.category,
            subCategory = domain.subCategory,
            notes = domain.notes,
            pending = domain.pending,
            splits = domain.splits.map { TransactionSplitEntity.fromDomain(it) },
            receiptUrls = domain.receiptUrls,
            matchedRuleId = domain.matchedRuleId
        )
    }
}
