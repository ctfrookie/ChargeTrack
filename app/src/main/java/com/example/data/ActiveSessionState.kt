package com.example.data

data class ActiveSessionState(
    val startTimeMillis: Long,
    val startBatteryLevel: Int,
    val currentBatteryLevel: Int,
    val currentDurationMillis: Long = 0L,
    val plugType: String = "AC",
    val startVoltageMv: Int = 0,
    val currentVoltageMv: Int = 0,
    val startTempC: Float = 0f,
    val currentTempC: Float = 0f,
    val isSimulated: Boolean = false,
    // Power and target charging settings
    val targetBatteryLevel: Int = 80,
    val stopTimerAtTarget: Boolean = true,
    val targetReachedTimestamp: Long? = null,
    val targetReachedDurationMillis: Long? = null,
    val currentPowerWatts: Float = 0f,
    val maxPowerWatts: Float = 0f,
    val currentMa: Int = 0
) {
    val isTargetReached: Boolean
        get() = targetReachedDurationMillis != null || currentBatteryLevel >= targetBatteryLevel

    /**
     * Display duration: if stopTimerAtTarget is enabled and target is reached,
     * display the duration at which target was reached; otherwise display current elapsed time.
     */
    val displayDurationMillis: Long
        get() = if (stopTimerAtTarget && targetReachedDurationMillis != null) {
            targetReachedDurationMillis
        } else {
            currentDurationMillis
        }

    val batteryGained: Int
        get() = (currentBatteryLevel - startBatteryLevel).coerceAtLeast(0)

    val ratePercentPerMinute: Double
        get() {
            val minutes = displayDurationMillis / 60000.0
            return if (minutes > 0.1 && batteryGained > 0) batteryGained / minutes else 0.0
        }

    val estimatedMinutesToTarget: Int?
        get() {
            val remainingPercent = targetBatteryLevel - currentBatteryLevel
            if (remainingPercent <= 0) return 0
            val rate = ratePercentPerMinute
            return if (rate > 0.05) {
                (remainingPercent / rate).toInt()
            } else {
                null
            }
        }
}
