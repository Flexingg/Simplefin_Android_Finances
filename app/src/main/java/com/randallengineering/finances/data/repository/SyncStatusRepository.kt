package com.randallengineering.finances.data.repository

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit

/**
 * Records the outcome of each background/manual bank sync so failures are never
 * silent. Backed by SharedPreferences (tiny state, few fields).
 */
class SyncStatusRepository(context: Context) {

    data class SyncStatus(
        val lastSyncEpoch: Long = 0L,      // 0 = never synced
        val succeeded: Boolean? = null,    // null = never synced
        val errorMessage: String? = null,
        val consecutiveFailures: Int = 0,
        val transactionCount: Int = 0,
        val accountCount: Int = 0
    ) {
        val syncedAgoText: String
            get() {
                if (lastSyncEpoch <= 0L) return "Never synced"
                val minutes = (System.currentTimeMillis() / 1000 - lastSyncEpoch) / 60
                return when {
                    minutes < 1 -> "just now"
                    minutes < 60 -> "$minutes min ago"
                    minutes < 60 * 24 -> "${minutes / 60} hr ago"
                    else -> {
                        val days = minutes / (60 * 24)
                        "$days day${if (days == 1L) "" else "s"} ago"
                    }
                }
            }
    }

    private val prefs = context.getSharedPreferences("randall_finances_sync", Context.MODE_PRIVATE)
    private val _flow = MutableStateFlow(load())
    val flow: StateFlow<SyncStatus> = _flow.asStateFlow()

    fun recordSuccess(transactionCount: Int = _flow.value.transactionCount, accountCount: Int = _flow.value.accountCount) {
        val next = SyncStatus(
            lastSyncEpoch = System.currentTimeMillis() / 1000,
            succeeded = true,
            errorMessage = null,
            consecutiveFailures = 0,
            transactionCount = transactionCount,
            accountCount = accountCount
        )
        _flow.value = next
        persist(next)
    }

    /** @return true when this is the first failure of a streak (a good time to notify). */
    fun recordFailure(errorMessage: String?): Boolean {
        val firstOfStreak = _flow.value.consecutiveFailures == 0
        val next = SyncStatus(
            lastSyncEpoch = System.currentTimeMillis() / 1000,
            succeeded = false,
            errorMessage = errorMessage,
            consecutiveFailures = _flow.value.consecutiveFailures + 1,
            transactionCount = _flow.value.transactionCount,
            accountCount = _flow.value.accountCount
        )
        _flow.value = next
        persist(next)
        return firstOfStreak
    }

    private fun load(): SyncStatus {
        return try {
            SyncStatus(
                lastSyncEpoch = prefs.getLong("last_sync_epoch", 0L),
                succeeded = if (prefs.contains("succeeded")) prefs.getBoolean("succeeded", false) else null,
                errorMessage = prefs.getString("error_message", null),
                consecutiveFailures = prefs.getInt("consecutive_failures", 0),
                transactionCount = prefs.getInt("tx_count", 0),
                accountCount = prefs.getInt("account_count", 0)
            )
        } catch (e: Exception) {
            SyncStatus()
        }
    }

    private fun persist(s: SyncStatus) {
        try {
            val e = prefs.edit()
                .putLong("last_sync_epoch", s.lastSyncEpoch)
                .putString("error_message", s.errorMessage)
                .putInt("consecutive_failures", s.consecutiveFailures)
                .putInt("tx_count", s.transactionCount)
                .putInt("account_count", s.accountCount)
            if (s.succeeded != null) e.putBoolean("succeeded", s.succeeded)
            e.apply()
        } catch (e: Exception) {
            // non-fatal
        }
    }
}
