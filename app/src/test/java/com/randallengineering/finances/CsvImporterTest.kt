package com.randallengineering.finances

import com.randallengineering.finances.core.util.CsvExporter
import com.randallengineering.finances.core.util.CsvImporter
import com.randallengineering.finances.domain.model.Transaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvImporterTest {

    @Test
    fun `round-trips its own export`() {
        val txs = listOf(
            Transaction(id = "t1", postedEpochSeconds = 1_700_000_000, amount = -45.32, originalDesc = "WALMART", category = "Groceries"),
            Transaction(id = "t2", postedEpochSeconds = 1_700_010_000, amount = 2000.0, originalDesc = "ACME PAYROLL", payee = "ACME", category = "Income")
        )
        val csv = CsvExporter.toCsv(txs)
        val result = CsvImporter.importTransactions(csv)
        assertEquals(0, result.skipped)
        assertEquals(2, result.imported)
        assertTrue(result.problems.isEmpty())
    }

    @Test
    fun `parses quoted fields with commas and newlines`() {
        val csv = "Date,Description,Payee,Amount,Category,Subcategory,Notes,Pending,Split,TransactionID\n" +
            "\"2023-11-14 22:13:20\",\"COFFEE, \"\"LATTE\"\" SHOP\",Cafe,-6.50,Dining,,,0,,t9\n"
        val result = CsvImporter.importTransactions(csv)
        assertEquals(1, result.imported)
        assertEquals(0, result.skipped)
    }

    @Test
    fun `skips rows with bad amount or date and records a problem`() {
        val csv = "Date,Description,Payee,Amount,Category,Subcategory,Notes,Pending,Split,TransactionID\n" +
            "2023-11-14,GOOD,-,-6.50,Groceries,,,,,t1\n" +   // date ok, amount ok
            "2023-11-14,BADAMT,-,abc,Groceries,,,,,t2\n" +
            "not-a-date,BADDATE,-,-6.50,Groceries,,,,,t3\n"
        val result = CsvImporter.importTransactions(csv)
        assertEquals(1, result.imported)
        assertEquals(2, result.skipped)
        assertEquals(2, result.problems.size)
    }

    @Test
    fun `auto-detects common bank headers by name`() {
        val csv = "Date,Description,Amount\n" +
            "11/14/2023,\"STARBUCKS #123\",-4.75\n" +
            "11/15/2023,Direct Deposit,1200.00\n"
        val result = CsvImporter.importTransactions(csv)
        assertEquals(2, result.imported)
        assertEquals(0, result.skipped)
        assertTrue(result.problems.isEmpty())
    }

    @Test
    fun `parses millisecond epoch values`() {
        val csv = "Date,Description,Amount\n" +
            "1700000000000,EPOCH MS,10.00\n"
        val result = CsvImporter.importTransactions(csv)
        assertEquals(1, result.imported)
    }
}
