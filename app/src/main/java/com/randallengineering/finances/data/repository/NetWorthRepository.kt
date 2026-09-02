package com.randallengineering.finances.data.repository

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** One recorded net-worth reading. */
@Serializable
data class NetWorthPoint(
    val epochSeconds: Long,
    val netWorth: Double
)

/**
 * Persists a small, append-only time series of net-worth snapshots (one per
 * week) so the trend chart has real history rather than fabricated points.
 */
class NetWorthRepository(context: Context) {

    private val prefs = context.getSharedPreferences("randall_finances_networth", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    private val _snapshots = MutableStateFlow(load())
    val snapshots: StateFlow<List<NetWorthPoint>> = _snapshots.asStateFlow()

    /**
     * Records a weekly snapshot. Returns true if one was actually written
     * (i.e. it's a new week relative to the last snapshot).
     */
    fun recordIfNewWeek(netWorth: Double, nowEpoch: Long = System.currentTimeMillis() / 1000): Boolean {
        val last = _snapshots.value.lastOrNull()
        val WEEK_SECONDS = 7L * 24 * 3600
        if (last != null && nowEpoch - last.epochSeconds < WEEK_SECONDS) return false
        val next = _snapshots.value + NetWorthPoint(epochSeconds = nowEpoch, netWorth = netWorth)
        _snapshots.value = next.takeLast(260) // keep ~5 years
        persist(_snapshots.value)
        return true
    }

    fun currentNetWorth(): Double? = _snapshots.value.lastOrNull()?.netWorth

    private fun load(): List<NetWorthPoint> {
        return try {
            val raw = prefs.getString("snapshots", null)
            if (raw.isNullOrBlank()) emptyList() else json.decodeFromString<List<NetWorthPoint>>(raw)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun persist(list: List<NetWorthPoint>) {
        try {
            prefs.edit().putString("snapshots", json.encodeToString(list)).apply()
        } catch (e: Exception) {
            // non-fatal
        }
    }
}
