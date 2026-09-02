package com.randallengineering.finances.data.repository

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * User-facing notification preferences. All notification categories are
 * OPT-IN and default to OFF (the user's standing preference): nothing is
 * pushed until the user explicitly enables it here.
 */
class NotificationPrefsRepository(context: Context) {

    private val prefs = context.getSharedPreferences("randall_finances_notif", Context.MODE_PRIVATE)

    private val _budgetAlerts = MutableStateFlow(prefs.getBoolean("budget_alerts", false))
    private val _reviewAlerts = MutableStateFlow(prefs.getBoolean("review_alerts", false))

    val budgetAlerts: StateFlow<Boolean> = _budgetAlerts.asStateFlow()
    val reviewAlerts: StateFlow<Boolean> = _reviewAlerts.asStateFlow()

    fun setBudgetAlerts(enabled: Boolean) {
        _budgetAlerts.value = enabled
        prefs.edit().putBoolean("budget_alerts", enabled).apply()
    }

    fun setReviewAlerts(enabled: Boolean) {
        _reviewAlerts.value = enabled
        prefs.edit().putBoolean("review_alerts", enabled).apply()
    }
}
