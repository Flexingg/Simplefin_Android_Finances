package com.randallengineering.finances.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AmazonOrderItem(
    val title: String,
    val asin: String = "",
    val quantityOrdered: Int = 1,
    val itemPrice: Double = 0.0,
    val itemTax: Double = 0.0,
    val totalPrice: Double = 0.0,
    val imageUrl: String? = null
)

@Serializable
data class MatchedAmazonOrder(
    val orderId: String,
    val purchaseDate: String,
    val orderTotal: Double,
    val orderStatus: String = "Delivered",
    val items: List<AmazonOrderItem> = emptyList()
)
