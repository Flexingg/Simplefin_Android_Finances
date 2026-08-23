package com.randallengineering.finances

import android.app.Application
import com.google.firebase.FirebaseApp
import com.randallengineering.finances.core.di.appModule
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
    }
}
