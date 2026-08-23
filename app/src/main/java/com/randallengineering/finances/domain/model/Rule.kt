package com.randallengineering.finances.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Rule(
    val id: String,
    val name: String,
    val priority: Int,
    val pattern: String,
    val minAmount: Double? = null,
    val maxAmount: Double? = null,
    val category: String,
    val subCategory: String = "",
    val matchCount: Long = 0,
    val isActive: Boolean = true
) {
    fun matches(description: String, amount: Double): Boolean {
        if (!isActive) return false
        
        // Amount range validation
        if (minAmount != null && amount < minAmount) return false
        if (maxAmount != null && amount > maxAmount) return false

        return try {
            // Case-insensitive regex support with (?i) prefix or RegexOption.IGNORE_CASE
            val regex = Regex(pattern, setOf(RegexOption.IGNORE_CASE))
            regex.containsMatchIn(description)
        } catch (e: Exception) {
            false
        }
    }
}
