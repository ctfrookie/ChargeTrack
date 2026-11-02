package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.service.ChargingForegroundService
import com.example.tracker.ChargingTracker

class PowerConnectionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val tracker = ChargingTracker.getInstance(context)
        when (intent.action) {
            Intent.ACTION_POWER_CONNECTED -> {
                tracker.handlePowerConnected(intent, isManual = false)
                ChargingForegroundService.start(context)
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                tracker.handlePowerDisconnected(intent, isManual = false)
                ChargingForegroundService.stop(context)
            }
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                // Device rebooted or app updated, ensure tracker verifies current charging state
                tracker.checkCurrentHardwareState()
            }
        }
    }
}
