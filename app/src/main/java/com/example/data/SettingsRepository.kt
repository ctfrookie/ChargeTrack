package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SettingsState(
    val targetBatteryLevel: Int = 80,
    val stopTimerAtTarget: Boolean = true,
    val webhookUrl: String = "",
    val webhookEnabled: Boolean = false,
    val notifyOnStart: Boolean = true,
    val notifyOnTarget: Boolean = true,
    val notifyOnFinish: Boolean = true,
    val vibrateOnTarget: Boolean = true
)

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("charge_timer_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<SettingsState> = _settings.asStateFlow()

    private fun loadSettings(): SettingsState {
        return SettingsState(
            targetBatteryLevel = prefs.getInt("target_battery_level", 80),
            stopTimerAtTarget = prefs.getBoolean("stop_timer_at_target", true),
            webhookUrl = prefs.getString("webhook_url", "") ?: "",
            webhookEnabled = prefs.getBoolean("webhook_enabled", false),
            notifyOnStart = prefs.getBoolean("notify_on_start", true),
            notifyOnTarget = prefs.getBoolean("notify_on_target", true),
            notifyOnFinish = prefs.getBoolean("notify_on_finish", true),
            vibrateOnTarget = prefs.getBoolean("vibrate_on_target", true)
        )
    }

    fun updateTargetBatteryLevel(level: Int) {
        val clamped = level.coerceIn(50, 100)
        prefs.edit().putInt("target_battery_level", clamped).apply()
        _settings.value = _settings.value.copy(targetBatteryLevel = clamped)
    }

    fun updateStopTimerAtTarget(stop: Boolean) {
        prefs.edit().putBoolean("stop_timer_at_target", stop).apply()
        _settings.value = _settings.value.copy(stopTimerAtTarget = stop)
    }

    fun updateWebhookUrl(url: String) {
        prefs.edit().putString("webhook_url", url).apply()
        _settings.value = _settings.value.copy(webhookUrl = url)
    }

    fun updateWebhookEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("webhook_enabled", enabled).apply()
        _settings.value = _settings.value.copy(webhookEnabled = enabled)
    }

    fun updateNotifyOnStart(enabled: Boolean) {
        prefs.edit().putBoolean("notify_on_start", enabled).apply()
        _settings.value = _settings.value.copy(notifyOnStart = enabled)
    }

    fun updateNotifyOnTarget(enabled: Boolean) {
        prefs.edit().putBoolean("notify_on_target", enabled).apply()
        _settings.value = _settings.value.copy(notifyOnTarget = enabled)
    }

    fun updateNotifyOnFinish(enabled: Boolean) {
        prefs.edit().putBoolean("notify_on_finish", enabled).apply()
        _settings.value = _settings.value.copy(notifyOnFinish = enabled)
    }

    fun updateVibrateOnTarget(enabled: Boolean) {
        prefs.edit().putBoolean("vibrate_on_target", enabled).apply()
        _settings.value = _settings.value.copy(vibrateOnTarget = enabled)
    }
}
