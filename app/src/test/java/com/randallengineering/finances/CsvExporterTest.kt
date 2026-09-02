package com.randallengineering.finances

import com.randallengineering.finances.core.util.CsvExporter
import com.randallengineering.finances.domain.model.Transaction
import com.randallengineering.finances.domain.model.TransactionSplit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvExporterTest {

    private fun tx(
        id: String = "t1",
        posted: Long = 1_700_000_000,
        amount: Double = -45.32,
        desc: String = "WALMART STORE",
        payee: String = "",
        category: String = "Groceries",
        splits: List<TransactionSplit> = emptyList()
    ) = Transaction(
        id = id, postedEpochSeconds = posted, amount = amount,
        originalDesc = desc, payee = payee, category = category, splits = splits
    )

    @Test
    fun `exports header and one row`() {
        val csv = CsvExporter.toCsv(listOf(tx()))
        val lines = csv.trim().lines()
        assertEquals("Date,Description,Payee,Amount,Category,Subcategory,Notes,Pending,Split,TransactionID", lines[0])
        assertTrue("row contains category", lines[1].contains("Groceries"))
        assertTrue("row contains amount", lines[1].contains("-45.32"))
        assertTrue("row contains id", lines[1].endsWith("t1"))
    }

    @Test
    fun `escapes commas quotes and newlines in fields`() {
        val tricky = tx(desc = "COFFEE, \"LATTE\"\nSHOP", category = "Dining")
        val csv = CsvExporter.toCsv(listOf(tricky))
        assertTrue("quoted and doubled quotes", csv.contains("\"COFFEE, \"\"LATTE\"\"\nSHOP\""))
    }

    @Test
    fun `renders split allocations in split column`() {
        val t = tx(splits = listOf(
            TransactionSplit(id = "s1", category = "Groceries", amount = -20.0),
            TransactionSplit(id = "s2", category = "Dining", amount = -25.32)
        ))
        val csv = CsvExporter.toCsv(listOf(t))
        assertTrue(csv.contains("Groceries:-20.00"))
        assertTrue(csv.contains("Dining:-25.32"))
    }

    @Test
    fun `no crash on blank or long descriptions`() {
        val blank = tx(desc = "", payee = "", amount = 0.0)
        val csv = CsvExporter.toCsv(listOf(blank))
        assertTrue(csv.isNotBlank())
    }
}
