package com.randallengineering.finances.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SimpleFinAccount(
    val id: String,
    val name: String,
    val orgName: String = "",
    val currency: String = "USD",
    val balance: Double = 0.0,
    val availableBalance: Double? = null,
    val balanceDateEpochSeconds: Long? = null
)
