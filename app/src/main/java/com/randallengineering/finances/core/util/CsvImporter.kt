package com.randallengineering.finances.core.util

import com.randallengineering.finances.domain.model.Transaction
import com.randallengineering.finances.domain.model.TransactionSplit
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

/**
 * Imports real transactions from CSV. Supports the app's own export format
 * (Date,Description,Payee,Amount,Category,Subcategory,Notes,Pending,Split,
 * TransactionID) and common bank exports via header-row column auto-detection.
 * Never fabricates data — only rows it can parse become transactions.
 */
object CsvImporter {

    data class ImportResult(
        val imported: Int = 0,
        val skipped: Int = 0,
        val problems: List<String> = emptyList(),
        val transactions: List<Transaction> = emptyList()
    )

    private val formats = listOf(
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd",
        "MM/dd/yyyy",
        "MM/dd/yyyy HH:mm:ss",
        "M/d/yyyy"
    )

    /** RFC-4180-style parser: handles quoted fields, escaped quotes, commas, newlines. */
    fun parseCsv(text: String): List<List<String>> {
        val rows = mutableListOf<MutableList<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0
        val n = text.length
        while (i < n) {
            val c = text[i]
            when {
                inQuotes -> {
                    if (c == '"') {
                        if (i + 1 < n && text[i + 1] == '"') { field.append('"'); i++ }
                        else inQuotes = false
                    } else {
                        field.append(c)
                    }
                }
                c == '"' -> inQuotes = true
                c == ',' -> { row.add(field.toString()); field.setLength(0) }
                c == '\n' -> {
                    row.add(field.toString()); field.setLength(0)
                    rows.add(row); row = mutableListOf()
                }
                c == '\r' -> { /* ignore */ }
                else -> field.append(c)
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) {
            row.add(field.toString())
            rows.add(row)
        }
        return rows
    }

    private fun indexOf(header: List<String>, vararg names: String): Int? {
        for (name in names) {
            val idx = header.indexOfFirst { it.trim().equals(name, ignoreCase = true) }
            if (idx >= 0) return idx
        }
        return null
    }

    fun importTransactions(csvText: String, nowEpoch: Long = System.currentTimeMillis() / 1000): ImportResult {
        if (csvText.isBlank()) return ImportResult(problems = listOf("File is empty"))
        val rows = parseCsv(csvText).filter { r -> r.any { it.isNotBlank() } }
        if (rows.isEmpty()) return ImportResult(problems = listOf("No rows found"))

        val header = rows.first()
        // Column indices by app-export names (case-insensitive).
        val idxDate = indexOf(header, "date", "posted", "transaction date")
        val idxDesc = indexOf(header, "description", "memo", "payee", "merchant", "name")
        val idxPayee = indexOf(header, "payee")
        val idxAmount = indexOf(header, "amount")
        val idxCategory = indexOf(header, "category")
        val idxSub = indexOf(header, "subcategory", "sub-category", "sub category")
        val idxNotes = indexOf(header, "notes", "note")
        val idxPending = indexOf(header, "pending")
        val idxSplit = indexOf(header, "split")
        val idxId = indexOf(header, "transactionid", "transaction id", "id")

        val hasHeader = header.any { it.trim().equals("date", true) } || idxAmount != null
        val dataRows = if (hasHeader) rows.drop(1) else rows

        val imported = mutableListOf<Transaction>()
        val problems = mutableListOf<String>()
        dataRows.forEachIndexed { idx, r ->
            val lineNo = idx + (if (hasHeader) 2 else 1)
            try {
                val dateRaw = r.getOrNull(idxDate ?: 0)?.trim().orEmpty()
                val amountRaw = r.getOrNull(idxAmount ?: 3)?.trim().orEmpty()
                if (dateRaw.isBlank() && amountRaw.isBlank()) return@forEachIndexed
                if (amountRaw.isBlank()) { problems.add("Line $lineNo: missing amount"); return@forEachIndexed }

                val amount = amountRaw.replace("$", "").replace(",", "").trim().toDoubleOrNull()
                if (amount == null) { problems.add("Line $lineNo: bad amount '$amountRaw'"); return@forEachIndexed }

                val epoch = parseEpoch(dateRaw)
                if (epoch == null) { problems.add("Line $lineNo: bad date '$dateRaw'"); return@forEachIndexed }

                val desc = r.getOrNull(idxDesc ?: 1)?.trim().orEmpty()
                val payee = r.getOrNull(idxPayee ?: -1)?.trim().orEmpty()
                val cat = r.getOrNull(idxCategory ?: 4)?.trim()?.takeIf { it.isNotBlank() } ?: "Uncategorized"
                val sub = r.getOrNull(idxSub ?: -1)?.trim().orEmpty()
                val notes = r.getOrNull(idxNotes ?: -1)?.trim().orEmpty()
                val pending = r.getOrNull(idxPending ?: -1)?.trim().equals("1", ignoreCase = true) ||
                    r.getOrNull(idxPending ?: -1)?.trim().equals("true", ignoreCase = true)
                val id = r.getOrNull(idxId ?: -1)?.trim()?.takeIf { it.isNotBlank() }
                    ?: UUID.randomUUID().toString()
                val splitRaw = r.getOrNull(idxSplit ?: -1)?.trim()

                val splits = if (!splitRaw.isNullOrBlank()) {
                    splitRaw.split(';').mapNotNull { part ->
                        val kv = part.trim().split(':')
                        if (kv.size >= 2) {
                            val amt = kv.drop(1).joinToString(":").replace(",", "").trim().toDoubleOrNull()
                            if (amt != null) TransactionSplit(
                                id = UUID.randomUUID().toString(),
                                category = kv[0].trim(),
                                amount = amt
                            ) else null
                        } else null
                    }
                } else emptyList()

                imported.add(
                    Transaction(
                        id = id,
                        postedEpochSeconds = epoch,
                        amount = amount,
                        originalDesc = desc.ifBlank { payee },
                        payee = payee,
                        category = if (splits.isNotEmpty()) "Split" else cat,
                        subCategory = sub,
                        notes = notes,
                        pending = pending,
                        splits = splits
                    )
                )
            } catch (e: Exception) {
                problems.add("Line $lineNo: ${e.localizedMessage ?: "parse error"}")
            }
        }

        return ImportResult(
            imported = imported.size,
            skipped = dataRows.size - imported.size,
            problems = problems,
            transactions = imported
        )
    }

    private fun parseEpoch(raw: String): Long? {
        val v = raw.trim()
        if (v.isEmpty()) return null
        // Plain number -> assume epoch seconds (or milliseconds if huge).
        val asNumber = v.toLongOrNull()
        if (asNumber != null) {
            return if (asNumber > 10_000_000_000L) asNumber / 1000 else asNumber
        }
        for (fmt in formats) {
            try {
                val d = SimpleDateFormat(fmt, Locale.US).parse(v)
                if (d != null) return d.time / 1000
            } catch (_: Exception) { }
        }
        return null
    }
}
