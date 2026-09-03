package com.randallengineering.finances.domain.model

import kotlinx.serialization.Serializable

/**
 * Discretionary-spending setpoint configuration.
 *
 * Monthly "fun money" allowance: every month (resetting on the 1st) the user is
 * allowed [setpoint] of discretionary spend. A category is discretionary unless
 * its name is in [necessaryCategories] (rent, utilities, groceries, ...).
 * Income, transfers and "Necessary"-flagged categories are excluded from the
 * discretionary total.
 */
@Serializable
data class DiscretionaryConfig(
    val setpoint: Double = 0.0,
    val necessaryCategories: List<String> = emptyList()
)
