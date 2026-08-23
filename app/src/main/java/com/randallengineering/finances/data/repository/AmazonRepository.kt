package com.randallengineering.finances.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri

class AmazonRepository(
    private val context: Context
) {
    /**
     * Opens the Amazon App or default web browser directly to the user's Order History.
     */
    fun openOrderHistory(ctx: Context = context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.amazon.com/gp/your-account/order-history")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            ctx.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Opens a specific Amazon order if an orderId is known.
     */
    fun openOrderDetails(orderId: String, ctx: Context = context) {
        try {
            val url = if (orderId.isNotBlank()) {
                "https://www.amazon.com/gp/your-account/order-details?orderID=$orderId"
            } else {
                "https://www.amazon.com/gp/your-account/order-history"
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            ctx.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
