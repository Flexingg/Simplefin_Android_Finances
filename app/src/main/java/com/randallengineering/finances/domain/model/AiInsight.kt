package com.randallengineering.finances.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class InsightType {
    ANOMALY_OVERPACING,
    CASH_FLOW_FORECAST,
    SAVINGS_OPPORTUNITY,
    GENERAL
}

@Serializable
enum class InsightSeverity {
    HIGH,
    MEDIUM,
    LOW
}

@Serializable
data class AiInsight(
    val id: String,
    val title: String,
    val summary: String,
    val type: InsightType,
    val severity: InsightSeverity = InsightSeverity.MEDIUM,
    val actionableTip: String = "",
    val timestamp: Long = System.currentTimeMillis() / 1000
)
