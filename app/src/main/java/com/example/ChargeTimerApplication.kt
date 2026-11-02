package com.example

import android.app.Application
import com.example.notification.NotificationHelper
import com.example.tracker.ChargingTracker

class ChargeTimerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)
        // Initialize charging tracker so that any pending or ongoing charging is tracked immediately
        ChargingTracker.getInstance(this)
    }
}
