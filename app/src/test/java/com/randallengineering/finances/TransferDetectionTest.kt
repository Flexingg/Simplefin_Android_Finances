package com.randallengineering.finances

import com.randallengineering.finances.core.finance.TransferDetection
import com.randallengineering.finances.domain.model.Transaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransferDetectionTest {

    private fun tx(id: String, acct: String, amount: Double, posted: Long = 1_700_000_000L, desc: String = "X")
        = Transaction(id = id, accountId = acct, postedEpochSeconds = posted, amount = amount, originalDesc = desc)

    @Test
    fun `flags an internal transfer pair across two accounts`() {
        val a = tx("t1", "acctA", -500.00, 1_700_000_000L, "TRANSFER")
        val b = tx("t2", "acctB", 500.00, 1_700_000_001L, "TRANSFER")
        val ids = TransferDetection.detectTransferIds(listOf(a, b))
        assertEquals(setOf("t1", "t2"), ids)
    }

    @Test
    fun `does not flag a lone withdrawal with no matching credit`() {
        val a = tx("t1", "acctA", -500.00)
        val ids = TransferDetection.detectTransferIds(listOf(a))
        assertTrue(ids.isEmpty())
    }

    @Test
    fun `does not flag when amounts differ materially`() {
        val a = tx("t1", "acctA", -500.00)
        val b = tx("t2", "acctB", 515.00, 1_700_000_001L)
        assertTrue(TransferDetection.detectTransferIds(listOf(a, b)).isEmpty())
    }

    @Test
    fun `does not flag same-account internal movement`() {
        val a = tx("t1", "acctA", -500.00)
        val b = tx("t2", "acctA", 500.00, 1_700_000_001L)
        assertTrue(TransferDetection.detectTransferIds(listOf(a, b)).isEmpty())
    }

    @Test
    fun `does not flag when posted too far apart`() {
        val a = tx("t1", "acctA", -500.00, 1_700_000_000L)
        val b = tx("t2", "acctB", 500.00, 1_700_000_000L + 10L * 86400)
        assertTrue(TransferDetection.detectTransferIds(listOf(a, b)).isEmpty())
    }

    @Test
    fun `tolerates a few cents difference`() {
        val a = tx("t1", "acctA", -500.00)
        val b = tx("t2", "acctB", 500.45, 1_700_000_001L)
        val ids = TransferDetection.detectTransferIds(listOf(a, b))
        assertEquals(setOf("t1", "t2"), ids)
    }

    @Test
    fun `empty or single-element list never flags`() {
        assertTrue(TransferDetection.detectTransferIds(emptyList()).isEmpty())
        assertTrue(TransferDetection.detectTransferIds(listOf(tx("t1", "a", -1.0))).isEmpty())
    }
}
