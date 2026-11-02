package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "charging_sessions")
data class ChargingSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val durationMillis: Long,
    val startBatteryLevel: Int,
    val endBatteryLevel: Int,
    val batteryGained: Int,
    val plugType: String, // AC, USB, Wireless, or Unknown
    val startVoltageMv: Int = 0,
    val endVoltageMv: Int = 0,
    val startTemperatureC: Float = 0f,
    val endTemperatureC: Float = 0f,
    val note: String = "",
    val isSimulated: Boolean = false,
    // New fields: Target level & duration, real-time power metrics
    val targetBatteryLevel: Int = 80,
    val targetReachedDurationMillis: Long? = null,
    val maxPowerWatts: Float = 0f,
    val avgPowerWatts: Float = 0f,
    val currentMa: Int = 0
) {
    /**
     * Charging rate in percentage points per minute (e.g. 0.85%/min)
     */
    val ratePercentPerMinute: Double
        get() {
            val effectiveDuration = targetReachedDurationMillis ?: durationMillis
            val minutes = effectiveDuration / 60000.0
            return if (minutes > 0.1) batteryGained / minutes else 0.0
        }

    /**
     * Charging rate in percentage points per hour (e.g. 51%/hr)
     */
    val ratePercentPerHour: Double
        get() = ratePercentPerMinute * 60.0
}
