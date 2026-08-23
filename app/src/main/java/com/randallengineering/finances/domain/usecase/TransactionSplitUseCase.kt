package com.randallengineering.finances.domain.usecase

import com.randallengineering.finances.core.network.Resource
import com.randallengineering.finances.data.repository.TransactionRepository
import com.randallengineering.finances.domain.model.Transaction
import com.randallengineering.finances.domain.model.TransactionSplit
import kotlin.math.abs

class TransactionSplitUseCase(
    private val transactionRepository: TransactionRepository
) {
    sealed class SplitValidationResult {
        data object Valid : SplitValidationResult()
        data class InvalidAmountSum(
            val expectedSum: Double,
            val actualSum: Double,
            val difference: Double
        ) : SplitValidationResult()
        data class EmptySplits(val message: String) : SplitValidationResult()
    }

    fun validateSplits(
        transaction: Transaction,
        splits: List<TransactionSplit>
    ): SplitValidationResult {
        if (splits.isEmpty()) {
            return SplitValidationResult.EmptySplits("A split transaction must have at least one sub-allocation")
        }

        val totalSplitAmount = splits.sumOf { it.amount }
        val diff = abs(transaction.amount - totalSplitAmount)

        // Accept within 1 cent tolerance for floating point representations
        return if (diff < 0.01) {
            SplitValidationResult.Valid
        } else {
            SplitValidationResult.InvalidAmountSum(
                expectedSum = transaction.amount,
                actualSum = totalSplitAmount,
                difference = diff
            )
        }
    }

    suspend fun applySplits(
        transaction: Transaction,
        splits: List<TransactionSplit>
    ): Resource<Unit> {
        val validation = validateSplits(transaction, splits)
        if (validation !is SplitValidationResult.Valid) {
            val errorMsg = when (validation) {
                is SplitValidationResult.InvalidAmountSum ->
                    "Splits sum ($${String.format("%.2f", validation.actualSum)}) does not match transaction amount ($${String.format("%.2f", validation.expectedSum)})"
                is SplitValidationResult.EmptySplits -> validation.message
                else -> "Invalid split configuration"
            }
            return Resource.Error(errorMsg)
        }

        return transactionRepository.saveTransactionSplits(transaction.id, splits)
    }
}
