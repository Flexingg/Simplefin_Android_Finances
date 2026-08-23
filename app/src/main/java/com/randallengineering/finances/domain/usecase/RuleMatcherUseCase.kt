package com.randallengineering.finances.domain.usecase

import com.randallengineering.finances.domain.model.Rule
import com.randallengineering.finances.domain.model.Transaction

class RuleMatcherUseCase {
    /**
     * Evaluates a transaction against rules sorted ascending by priority.
     * Applies the first matching rule and returns the updated transaction.
     */
    fun matchAndCategorize(
        transaction: Transaction,
        rules: List<Rule>
    ): Transaction {
        // If already split, do not override with single category rule
        if (transaction.isSplit) return transaction

        val sortedRules = rules
            .filter { it.isActive }
            .sortedBy { it.priority }

        for (rule in sortedRules) {
            if (rule.matches(transaction.originalDesc, transaction.amount)) {
                return transaction.copy(
                    category = rule.category,
                    subCategory = rule.subCategory,
                    matchedRuleId = rule.id
                )
            }
        }

        // Return unchanged if no rule matched
        return transaction
    }

    /**
     * Batch evaluation helper for incoming transactions list.
     * Pure evaluation that does not trigger state-mutation loops.
     */
    fun categorizeBatch(
        transactions: List<Transaction>,
        rules: List<Rule>
    ): List<Transaction> {
        val sortedRules = rules
            .filter { it.isActive }
            .sortedBy { it.priority }

        if (sortedRules.isEmpty()) return transactions

        return transactions.map { tx ->
            var matchedTx = tx
            if (!tx.isSplit) {
                for (rule in sortedRules) {
                    if (rule.matches(tx.originalDesc, tx.amount)) {
                        matchedTx = tx.copy(
                            category = rule.category,
                            subCategory = rule.subCategory,
                            matchedRuleId = rule.id
                        )
                        break
                    }
                }
            }
            matchedTx
        }
    }
}
