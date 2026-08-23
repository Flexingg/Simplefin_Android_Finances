package com.randallengineering.finances.core.ml

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern

data class ParsedReceipt(
    val merchantName: String = "",
    val totalAmount: Double = 0.0,
    val dateEpochSeconds: Long = System.currentTimeMillis() / 1000,
    val rawText: String = ""
)

object ReceiptOcrScanner {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private val priceRegex = Pattern.compile("(?i)(?:total|amount due|balance due|grand total|subtotal|charge|paid)?\\s*\\$?\\s*([0-9]+\\.[0-9]{2})")
    private val standalonePriceRegex = Pattern.compile("\\b([0-9]+\\.[0-9]{2})\\b")
    private val dateRegex = Pattern.compile("\\b(0?[1-9]|1[0-2])[/-](0?[1-9]|[12][0-9]|3[01])[/-](20\\d{2}|\\d{2})\\b")

    suspend fun scanReceipt(context: Context, imageUri: Uri): ParsedReceipt = withContext(Dispatchers.Default) {
        try {
            val inputImage = InputImage.fromFilePath(context, imageUri)
            val visionText = recognizer.process(inputImage).await()
            val fullText = visionText.text
            val lines = visionText.textBlocks.flatMap { it.lines }.map { it.text.trim() }.filter { it.isNotBlank() }

            if (lines.isEmpty()) {
                return@withContext ParsedReceipt(rawText = fullText)
            }

            // 1. Merchant Extraction: Top non-generic line
            val merchant = extractMerchant(lines)

            // 2. Total Amount Extraction
            val total = extractTotalAmount(lines)

            // 3. Date Extraction
            val dateSeconds = extractDate(lines)

            ParsedReceipt(
                merchantName = merchant,
                totalAmount = total,
                dateEpochSeconds = dateSeconds,
                rawText = fullText
            )
        } catch (e: Exception) {
            e.printStackTrace()
            ParsedReceipt(rawText = "OCR scan failed: ${e.localizedMessage}")
        }
    }

    private fun extractMerchant(lines: List<String>): String {
        val ignoreWords = setOf("receipt", "tax invoice", "welcome", "customer copy", "store #", "register #", "cashier", "order #", "sale", "merchant id", "terminal")
        for (line in lines.take(5)) {
            val lower = line.lowercase()
            if (ignoreWords.none { lower.contains(it) } && line.length in 3..40 && !line.matches(Regex("^[0-9\\W]+$"))) {
                return line.replace(Regex("[^A-Za-z0-9&'\\s-]"), "").trim()
            }
        }
        return lines.firstOrNull()?.take(30)?.trim().orEmpty()
    }

    private fun extractTotalAmount(lines: List<String>): Double {
        var foundTotal = 0.0
        val totalKeywords = listOf("total", "grand total", "amount due", "balance due", "charge", "debit", "visa", "mastercard")

        // First look for lines containing explicit "Total" keyword
        for (line in lines.reversed()) {
            val lower = line.lowercase()
            if (totalKeywords.any { lower.contains(it) } && !lower.contains("subtotal")) {
                val matcher = priceRegex.matcher(line)
                if (matcher.find()) {
                    val priceStr = matcher.group(1) ?: matcher.group(0)
                    val parsed = priceStr?.replace("$", "")?.toDoubleOrNull()
                    if (parsed != null && parsed > 0) {
                        return parsed
                    }
                }
            }
        }

        // Fallback: search all lines for max reasonable dollar value (avoiding phone numbers or years)
        val allPrices = mutableListOf<Double>()
        for (line in lines) {
            val matcher = standalonePriceRegex.matcher(line)
            while (matcher.find()) {
                val price = matcher.group(1)?.toDoubleOrNull()
                if (price != null && price in 0.50..5000.0) {
                    allPrices.add(price)
                }
            }
        }

        return allPrices.maxOrNull() ?: 0.0
    }

    private fun extractDate(lines: List<String>): Long {
        val now = LocalDate.now()
        for (line in lines) {
            val matcher = dateRegex.matcher(line)
            if (matcher.find()) {
                val matched = matcher.group(0) ?: continue
                try {
                    val clean = matched.replace("-", "/")
                    val parts = clean.split("/")
                    val month = parts[0].toInt()
                    val day = parts[1].toInt()
                    var year = parts[2].toInt()
                    if (year < 100) year += 2000
                    val parsedDate = LocalDate.of(year, month, day)
                    return parsedDate.atStartOfDay(ZoneId.systemDefault()).toEpochSecond()
                } catch (e: Exception) {
                    // Ignore parse error
                }
            }
        }
        return now.atStartOfDay(ZoneId.systemDefault()).toEpochSecond()
    }
}
