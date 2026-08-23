package com.randallengineering.finances

import com.randallengineering.finances.data.repository.TransactionRepository
import com.randallengineering.finances.domain.model.Transaction
import com.randallengineering.finances.domain.model.TransactionSplit
import com.randallengineering.finances.domain.usecase.TransactionSplitUseCase
import io.mockk.mockk
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionSplitUseCaseTest {

    private val transactionRepository: TransactionRepository = mockk(relaxed = true)
    private val splitUseCase = TransactionSplitUseCase(transactionRepository)

    @Test
    fun `test valid splits summing to transaction amount passes validation`() {
        val transaction = Transaction(
            id = "tx1",
            postedEpochSeconds = 1000L,
            amount = -100.0,
            originalDesc = "SUPERSTORE"
        )

        val splits = listOf(
            TransactionSplit(id = "s1", category = "Groceries", amount = -60.0),
            TransactionSplit(id = "s2", category = "Home Goods", amount = -40.0)
        )

        val validation = splitUseCase.validateSplits(transaction, splits)
        assertTrue(validation is TransactionSplitUseCase.SplitValidationResult.Valid)
    }

    @Test
    fun `test unbalanced splits are rejected`() {
        val transaction = Transaction(
            id = "tx1",
            postedEpochSeconds = 1000L,
            amount = -100.0,
            originalDesc = "SUPERSTORE"
        )

        val splits = listOf(
            TransactionSplit(id = "s1", category = "Groceries", amount = -60.0),
            TransactionSplit(id = "s2", category = "Home Goods", amount = -30.0) // Sum = -90.0 != -100.0
        )

        val validation = splitUseCase.validateSplits(transaction, splits)
        assertTrue(validation is TransactionSplitUseCase.SplitValidationResult.InvalidAmountSum)
    }

    @Test
    fun `test empty splits list is rejected`() {
        val transaction = Transaction(
            id = "tx1",
            postedEpochSeconds = 1000L,
            amount = -100.0,
            originalDesc = "SUPERSTORE"
        )

        val validation = splitUseCase.validateSplits(transaction, emptyList())
        assertTrue(validation is TransactionSplitUseCase.SplitValidationResult.EmptySplits)
    }
}
