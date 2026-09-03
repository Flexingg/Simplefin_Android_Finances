package com.randallengineering.finances.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.randallengineering.finances.MainActivity
import com.randallengineering.finances.R
import com.randallengineering.finances.core.util.CurrencyFormatter

object NotificationHelper {

    object DeepLinkTarget {
        const val BUDGETS = "budgets"
        const val REVIEW_QUEUE = "queue"
        const val SYNC = "settings"
    }
    private const val EXTRA_DEEP_LINK = "deep_link_target"

    const val CHANNEL_SYNC = "finances_sync_channel"
    const val CHANNEL_BUDGET_ALERTS = "finances_budget_alerts"
    const val CHANNEL_GAMIFICATION = "finances_gamification_alerts"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val syncChannel = NotificationChannel(
                CHANNEL_SYNC,
                "Bank Sync Updates",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background bank account synchronization status"
            }

            val budgetChannel = NotificationChannel(
                CHANNEL_BUDGET_ALERTS,
                "Budget & Spending Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when approaching budget thresholds or large purchases"
                enableVibration(true)
            }

            val gamificationChannel = NotificationChannel(
                CHANNEL_GAMIFICATION,
                "Habit & Quest Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily streak reminders and quest milestones"
            }

            manager.createNotificationChannel(syncChannel)
            manager.createNotificationChannel(budgetChannel)
            manager.createNotificationChannel(gamificationChannel)
        }
    }

    private fun getAppLaunchPendingIntent(context: Context, target: String? = null, requestCode: Int = 0): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (target != null) putExtra(EXTRA_DEEP_LINK, target)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun sendBudgetWarningNotification(
        context: Context,
        category: String,
        percent: Double,
        spent: Double,
        limit: Double
    ) {
        try {
            val title = if (percent >= 1.0) "🚨 Over Budget: $category" else "⚠️ Budget Alert: $category at ${(percent * 100).toInt()}%"
            val message = if (percent >= 1.0) {
                "You've spent ${CurrencyFormatter.format(spent)} of your ${CurrencyFormatter.format(limit)} limit."
            } else {
                "You've used ${(percent * 100).toInt()}% (${CurrencyFormatter.format(spent)} / ${CurrencyFormatter.format(limit)})."
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_BUDGET_ALERTS)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(getAppLaunchPendingIntent(context, DeepLinkTarget.BUDGETS, 1))
                .setAutoCancel(true)
                .build()

            NotificationManagerCompat.from(context).notify(category.hashCode(), notification)
        } catch (e: SecurityException) {
            // Notification permission not granted on Android 13+
        }
    }

    fun sendHighSpendAlert(
        context: Context,
        merchant: String,
        amount: Double
    ) {
        try {
            val notification = NotificationCompat.Builder(context, CHANNEL_BUDGET_ALERTS)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("💳 Large Transaction Detected")
                .setContentText("${CurrencyFormatter.format(amount)} spent at $merchant. Review in Queue!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(getAppLaunchPendingIntent(context, DeepLinkTarget.REVIEW_QUEUE, 2))
                .setAutoCancel(true)
                .build()

            NotificationManagerCompat.from(context).notify(merchant.hashCode(), notification)
        } catch (e: SecurityException) {
            // Ignored if permission not granted
        }
    }

    fun sendSyncSummaryNotification(
        context: Context,
        newTxCount: Int,
        autoCategorizedCount: Int
    ) {
        if (newTxCount <= 0) return
        try {
            val title = "⚡ Synced $newTxCount Bank Transactions"
            val message = if (autoCategorizedCount > 0) {
                "Auto-rules categorized $autoCategorizedCount items! Review remainder in Queue."
            } else {
                "New purchases available for daily review."
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_SYNC)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(getAppLaunchPendingIntent(context, DeepLinkTarget.REVIEW_QUEUE, 2))
                .setAutoCancel(true)
                .build()

            NotificationManagerCompat.from(context).notify(1001, notification)
        } catch (e: SecurityException) {
            // Ignored
        }
    }

    fun sendSyncFailureNotification(context: Context, errorMessage: String?) {
        try {
            val title = "⚠️ Background sync failed"
            val message = errorMessage?.takeIf { it.isNotBlank() }
                ?.let { "Couldn't refresh your accounts: $it" }
                ?: "Couldn't refresh your accounts. Check your connection and try again."

            val notification = NotificationCompat.Builder(context, CHANNEL_SYNC)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(getAppLaunchPendingIntent(context, DeepLinkTarget.SYNC, 3))
                .setAutoCancel(true)
                .build()

            NotificationManagerCompat.from(context).notify(1002, notification)
        } catch (e: SecurityException) {
            // Ignored
        }
    }
}
