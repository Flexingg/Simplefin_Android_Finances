package com.randallengineering.finances.core.util

import android.content.Context
import android.net.Uri
import com.randallengineering.finances.domain.model.AmazonOrderItem
import com.randallengineering.finances.domain.model.MatchedAmazonOrder
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs

object AmazonCsvParser {

    /**
     * Parses an Amazon Order History CSV (downloaded from Amazon account reports or privacy data requests).
     * Maps Order ID -> MatchedAmazonOrder with full line item details.
     */
    fun parseAmazonOrdersCsv(context: Context, uri: Uri): List<MatchedAmazonOrder> {
        val ordersMap = mutableMapOf<String, MutableList<AmazonOrderItem>>()
        val orderDateMap = mutableMapOf<String, String>()
        val orderTotalMap = mutableMapOf<String, Double>()

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                val headerLine = reader.readLine() ?: return emptyList()
                val headers = parseCsvLine(headerLine).map { it.trim().lowercase() }

                val orderIdIdx = headers.indexOfFirst { it.contains("order id") || it == "order-id" }
                val titleIdx = headers.indexOfFirst { it.contains("title") || it.contains("product name") || it.contains("item name") || it.contains("description") }
                val dateIdx = headers.indexOfFirst { it.contains("order date") || it.contains("date") }
                val priceIdx = headers.indexOfFirst { it.contains("item total") || it.contains("total amount") || it.contains("item subtotal") || it.contains("unit price") || it.contains("purchase price") }
                val taxIdx = headers.indexOfFirst { it.contains("tax") }
                val qtyIdx = headers.indexOfFirst { it.contains("quantity") || it == "qty" }
                val asinIdx = headers.indexOfFirst { it.contains("asin") }

                var line: String? = reader.readLine()
                while (line != null) {
                    if (line.isNotBlank()) {
                        val columns = parseCsvLine(line)
                        if (columns.isNotEmpty() && orderIdIdx in columns.indices) {
                            val orderId = columns.getOrNull(orderIdIdx)?.trim().orEmpty()
                            if (orderId.isNotBlank() && orderId != "Order ID") {
                                val title = if (titleIdx in columns.indices) columns[titleIdx].trim() else "Amazon Item"
                                val dateStr = if (dateIdx in columns.indices) columns[dateIdx].trim() else ""
                                val priceRaw = if (priceIdx in columns.indices) columns[priceIdx].replace("$", "").replace(",", "").trim() else "0"
                                val taxRaw = if (taxIdx in columns.indices) columns[taxIdx].replace("$", "").replace(",", "").trim() else "0"
                                val qtyRaw = if (qtyIdx in columns.indices) columns[qtyIdx].trim() else "1"
                                val asin = if (asinIdx in columns.indices) columns[asinIdx].trim() else ""

                                val price = priceRaw.toDoubleOrNull() ?: 0.0
                                val tax = taxRaw.toDoubleOrNull() ?: 0.0
                                val qty = qtyRaw.toIntOrNull() ?: 1

                                val item = AmazonOrderItem(
                                    title = title.ifBlank { "Amazon Item" },
                                    asin = asin,
                                    quantityOrdered = qty,
                                    itemPrice = price,
                                    itemTax = tax,
                                    totalPrice = (price * qty) + tax
                                )

                                ordersMap.getOrPut(orderId) { mutableListOf() }.add(item)
                                if (dateStr.isNotBlank()) orderDateMap[orderId] = dateStr
                                orderTotalMap[orderId] = (orderTotalMap[orderId] ?: 0.0) + item.totalPrice
                            }
                        }
                    }
                    line = reader.readLine()
                }
            }
        }

        return ordersMap.map { (orderId, items) ->
            MatchedAmazonOrder(
                orderId = orderId,
                purchaseDate = orderDateMap[orderId] ?: "",
                orderTotal = orderTotalMap[orderId] ?: items.sumOf { it.totalPrice },
                orderStatus = "Delivered",
                items = items
            )
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = java.lang.StringBuilder()
        var inQuotes = false

        for (ch in line) {
            when {
                ch == '\"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    tokens.add(sb.toString())
                    sb.setLength(0)
                }
                else -> sb.append(ch)
            }
        }
        tokens.add(sb.toString())
        return tokens
    }
}
