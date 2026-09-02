package com.randallengineering.finances

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.randallengineering.finances.core.di.appModule
import com.randallengineering.finances.core.notifications.NotificationHelper
import com.randallengineering.finances.core.work.WorkScheduler
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

class RandallFinancesApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Persist any uncaught crash to a log file so it can be retrieved and fixed.
        val prevHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                val stack = sw.toString()
                Log.e("RandallFinances", "Uncaught crash on ${thread.name}", throwable)
                val f = File(filesDir, "crash.log")
                f.appendText("\n--- ${System.currentTimeMillis()} on ${thread.name} ---\n$stack\n")
            } catch (_: Exception) {
            }
            prevHandler?.uncaughtException(thread, throwable)
        }

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Initialize Koin Dependency Injection
        startKoin {
            androidLogger(Level.INFO)
            androidContext(this@RandallFinancesApp)
            modules(appModule)
        }

        // Initialize Notification Channels
        NotificationHelper.createNotificationChannels(this)

        // Schedule Background Bank Sync & Rule Auto-Runs
        WorkScheduler.schedulePeriodicBankSync(this)
    }
}
