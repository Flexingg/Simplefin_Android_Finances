package com.randallengineering.finances.core.util

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

object CurrencyFormatter {
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

    fun format(amount: Double): String {
        return currencyFormat.format(amount)
    }

    fun formatWithSign(amount: Double): String {
        val formatted = currencyFormat.format(abs(amount))
        return if (amount < 0) {
            "-$formatted"
        } else if (amount > 0) {
            "+$formatted"
        } else {
            formatted
        }
    }

    fun formatPacing(pacingPercent: Double): String {
        return String.format(Locale.US, "%.1f%%", pacingPercent)
    }
}
