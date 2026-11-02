package com.example.service

import android.app.Notification
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.data.SettingsRepository
import com.example.notification.NotificationHelper
import com.example.tracker.ChargingTracker
import com.example.util.TimeUtils
import com.example.util.WebhookSender
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ChargingForegroundService : Service() {

    private val scope = CoroutineScope(Dispatchers.Default)
    private var tickerJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var tracker: ChargingTracker
    private lateinit var settingsRepo: SettingsRepository

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
                tracker.handleBatteryChangedIntent(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        tracker = ChargingTracker.getInstance(this)
        settingsRepo = SettingsRepository(this)

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "ChargeTimer::ChargingWakeLock"
        ).apply {
            setReferenceCounted(false)
            acquire(6 * 3600 * 1000L) // Safety timeout: 6 hours max
        }

        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        NotificationHelper.createNotificationChannel(this)
        val initialNotification = buildNotification("⚡ 充电自动计时已启动", "正在持续监测充电时长与实时功率...")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                initialNotification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, initialNotification)
        }

        startTicker()
        return START_STICKY
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            var targetAlertTriggered = false

            while (isActive) {
                delay(1000)

                if (!tracker.isCharging.value) {
                    // Charging stopped, finish service
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    break
                }

                val active = tracker.activeSession.value
                val settings = settingsRepo.settings.value

                if (active != null) {
                    val targetLevel = settings.targetBatteryLevel
                    val isAtTarget = active.currentBatteryLevel >= targetLevel

                    // Target level reached event
                    if (isAtTarget && !targetAlertTriggered && active.targetReachedDurationMillis == null) {
                        targetAlertTriggered = true
                        val durationAtTarget = active.currentDurationMillis
                        tracker.recordTargetReached(targetLevel, durationAtTarget)

                        if (settings.vibrateOnTarget) {
                            triggerVibration()
                        }

                        if (settings.webhookEnabled && settings.notifyOnTarget && settings.webhookUrl.isNotBlank()) {
                            val durationFormatted = TimeUtils.formatDurationChinese(durationAtTarget)
                            scope.launch {
                                WebhookSender.sendNotification(
                                    webhookUrl = settings.webhookUrl,
                                    event = "TARGET_REACHED",
                                    title = "已达到设定目标电量 ($targetLevel%)",
                                    message = "电池电量已充至 $targetLevel%！充电用时 $durationFormatted，当前功率 ${String.format("%.1f", active.currentPowerWatts)}W，建议及时拔掉充电器以延长电池寿命。",
                                    durationText = durationFormatted,
                                    batteryLevel = active.currentBatteryLevel,
                                    powerWatts = active.currentPowerWatts
                                )
                            }
                        }
                    }

                    // Update persistent notification
                    val durationText = TimeUtils.formatDigitalTimer(active.displayDurationMillis)
                    val powerStr = if (active.currentPowerWatts > 0f) "${String.format("%.1f", active.currentPowerWatts)}W" else ""
                    val title = if (active.isTargetReached) {
                        "🎯 已达到目标电量 ${settings.targetBatteryLevel}% ($durationText)"
                    } else {
                        "⚡ 充电计时进行中 ($durationText)"
                    }

                    val content = buildString {
                        append("电量 ${active.startBatteryLevel}% ➔ ${active.currentBatteryLevel}% (+${active.batteryGained}%)")
                        if (powerStr.isNotBlank()) append(" · $powerStr")
                        append(" · ${active.plugType}")
                    }

                    val updatedNotification = buildNotification(title, content)
                    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                    manager.notify(NOTIFICATION_ID, updatedNotification)
                }
            }
        }
    }

    private fun triggerVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 300, 200, 300), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                        VibrationEffect.createWaveform(longArrayOf(0, 300, 200, 300), -1)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0, 300, 200, 300), -1)
                }
            }
        } catch (_: Exception) {}
    }

    private fun buildNotification(title: String, content: String): Notification {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = android.app.PendingIntent.getActivity(
            this,
            0,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, "charging_timer_channel")
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setContentTitle(title)
            .setContentText(content)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // If phone is currently charging when user clears recent apps, keep service alive
        if (::tracker.isInitialized && tracker.isCharging.value) {
            try {
                val restartIntent = Intent(applicationContext, ChargingForegroundService::class.java).apply {
                    setPackage(packageName)
                }
                val pendingIntent = android.app.PendingIntent.getService(
                    applicationContext,
                    99,
                    restartIntent,
                    android.app.PendingIntent.FLAG_ONE_SHOT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
                val alarmManager = getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager
                alarmManager?.set(
                    android.app.AlarmManager.ELAPSED_REALTIME,
                    android.os.SystemClock.elapsedRealtime() + 800,
                    pendingIntent
                )
            } catch (_: Exception) {}
        }
        ChargingJobService.scheduleJob(applicationContext)
    }

    override fun onDestroy() {
        super.onDestroy()
        tickerJob?.cancel()
        scope.cancel()
        try {
            unregisterReceiver(batteryReceiver)
        } catch (_: Exception) {}
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {}
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, ChargingForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, ChargingForegroundService::class.java)
            context.stopService(intent)
        }
    }
}
