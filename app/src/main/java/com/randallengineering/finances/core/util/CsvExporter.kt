package com.randallengineering.finances.core.util

import com.randallengineering.finances.domain.model.Transaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Builds a CSV export of the user's real transactions (no demo data). Columns
 * mirror the transaction model so the export round-trips into any spreadsheet.
 */
object CsvExporter {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    private val amountFormat = "%.2f"

    /** Escape a field for CSV: quote if it contains a comma, quote, or newline. */
    private fun escape(field: String): String {
        if (field.contains(',') || field.contains('"') || field.contains('\n')) {
            return "\"" + field.replace("\"", "\"\"") + "\""
        }
        return field
    }

    fun toCsv(transactions: List<Transaction>): String {
        val sb = StringBuilder()
        sb.append("Date,Description,Payee,Amount,Category,Subcategory,Notes,Pending,Split,TransactionID\n")
        for (t in transactions) {
            val date = dateFormat.format(Date(t.postedEpochSeconds * 1000L))
            val amount = String.format(Locale.US, amountFormat, t.amount)
            val split = if (t.isSplit) {
                t.splits.joinToString(";") { s ->
                    "${s.category}:${String.format(Locale.US, amountFormat, s.amount)}"
                }
            } else {
                ""
            }
            sb.append(escape(date)).append(',')
                .append(escape(t.originalDesc)).append(',')
                .append(escape(t.payee)).append(',')
                .append(escape(amount)).append(',')
                .append(escape(t.category)).append(',')
                .append(escape(t.subCategory)).append(',')
                .append(escape(t.notes)).append(',')
                .append(if (t.pending) "1" else "0").append(',')
                .append(escape(split)).append(',')
                .append(escape(t.id)).append('\n')
        }
        return sb.toString()
    }
}
