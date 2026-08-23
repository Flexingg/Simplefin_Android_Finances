package com.randallengineering.finances

import android.app.Application
import com.google.firebase.FirebaseApp
import com.randallengineering.finances.core.di.appModule
import com.randallengineering.finances.core.notifications.NotificationHelper
import com.randallengineering.finances.core.work.WorkScheduler
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class RandallFinancesApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
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
