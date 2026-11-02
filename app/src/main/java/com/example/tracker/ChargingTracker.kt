package com.example.tracker

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.example.data.ActiveSessionState
import com.example.data.AppDatabase
import com.example.data.SettingsRepository
import com.example.model.ChargingSession
import com.example.notification.NotificationHelper
import com.example.service.ChargingForegroundService
import com.example.util.TimeUtils
import com.example.util.WebhookSender
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

class ChargingTracker private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Default)
    private val database = AppDatabase.getInstance(context)
    private val dao = database.chargingSessionDao()
    private val prefs = context.getSharedPreferences("charging_tracker_prefs", Context.MODE_PRIVATE)
    private val settingsRepo = SettingsRepository(context)

    private val _isCharging = MutableStateFlow(false)
    val isCharging: StateFlow<Boolean> = _isCharging.asStateFlow()

    private val _activeSession = MutableStateFlow<ActiveSessionState?>(null)
    val activeSession: StateFlow<ActiveSessionState?> = _activeSession.asStateFlow()

    private val _batteryLevel = MutableStateFlow(100)
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()

    private val _batteryStatus = MutableStateFlow("未充电")
    val batteryStatus: StateFlow<String> = _batteryStatus.asStateFlow()

    private val _plugType = MutableStateFlow("未接入")
    val plugType: StateFlow<String> = _plugType.asStateFlow()

    private val _voltageMv = MutableStateFlow(0)
    val voltageMv: StateFlow<Int> = _voltageMv.asStateFlow()

    private val _currentMa = MutableStateFlow(0)
    val currentMa: StateFlow<Int> = _currentMa.asStateFlow()

    private val _powerWatts = MutableStateFlow(0f)
    val powerWatts: StateFlow<Float> = _powerWatts.asStateFlow()

    private val _temperatureC = MutableStateFlow(0f)
    val temperatureC: StateFlow<Float> = _temperatureC.asStateFlow()

    private val _health = MutableStateFlow("良好")
    val health: StateFlow<String> = _health.asStateFlow()

    private val _lastCompletedSession = MutableStateFlow<ChargingSession?>(null)
    val lastCompletedSession: StateFlow<ChargingSession?> = _lastCompletedSession.asStateFlow()

    private val powerSamples = mutableListOf<Float>()
    private var timerJob: Job? = null

    init {
        checkCurrentHardwareState()
    }

    fun checkCurrentHardwareState() {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryIntent = context.registerReceiver(null, filter)

        val wasChargingActive = prefs.getBoolean(KEY_IS_CHARGING, false)
        val savedStartTime = prefs.getLong(KEY_START_TIME, 0L)
        val savedStartLevel = prefs.getInt(KEY_START_LEVEL, 0)
        val savedPlugType = prefs.getString(KEY_PLUG_TYPE, "AC") ?: "AC"
        val savedStartVoltage = prefs.getInt(KEY_START_VOLTAGE, 0)
        val savedStartTemp = prefs.getFloat(KEY_START_TEMP, 0f)
        val isSimulated = prefs.getBoolean(KEY_IS_SIMULATED, false)
        val savedTargetLevel = prefs.getInt(KEY_TARGET_LEVEL, 80)
        val savedTargetDuration = prefs.getLong(KEY_TARGET_DURATION, -1L)

        if (batteryIntent != null) {
            updateBatteryTelemetry(batteryIntent)
            val currentStatus = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val isNowCharging = currentStatus == BatteryManager.BATTERY_STATUS_CHARGING ||
                    currentStatus == BatteryManager.BATTERY_STATUS_FULL

            if (wasChargingActive) {
                if (isNowCharging || isSimulated) {
                    val currentLevel = _batteryLevel.value
                    val currentVoltage = _voltageMv.value
                    val currentTemp = _temperatureC.value
                    val duration = System.currentTimeMillis() - savedStartTime
                    val settings = settingsRepo.settings.value

                    val active = ActiveSessionState(
                        startTimeMillis = savedStartTime,
                        startBatteryLevel = savedStartLevel,
                        currentBatteryLevel = currentLevel,
                        currentDurationMillis = duration,
                        plugType = savedPlugType,
                        startVoltageMv = savedStartVoltage,
                        currentVoltageMv = currentVoltage,
                        startTempC = savedStartTemp,
                        currentTempC = currentTemp,
                        isSimulated = isSimulated,
                        targetBatteryLevel = savedTargetLevel,
                        stopTimerAtTarget = settings.stopTimerAtTarget,
                        targetReachedDurationMillis = if (savedTargetDuration >= 0) savedTargetDuration else null,
                        currentPowerWatts = _powerWatts.value,
                        maxPowerWatts = _powerWatts.value,
                        currentMa = _currentMa.value
                    )
                    _isCharging.value = true
                    _activeSession.value = active
                    startDurationTicker()
                    ChargingForegroundService.start(context)
                } else {
                    concludeSavedSession(
                        savedStartTime,
                        savedStartLevel,
                        savedPlugType,
                        savedStartVoltage,
                        savedStartTemp,
                        isSimulated,
                        savedTargetLevel,
                        if (savedTargetDuration >= 0) savedTargetDuration else null
                    )
                }
            } else if (isNowCharging) {
                startNewSession(
                    startLevel = _batteryLevel.value,
                    plug = _plugType.value,
                    voltage = _voltageMv.value,
                    temp = _temperatureC.value,
                    isSimulated = false
                )
                ChargingForegroundService.start(context)
            }
        }
    }

    fun handleBatteryChangedIntent(intent: Intent) {
        updateBatteryTelemetry(intent)

        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isChargingNow = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val currentActive = _activeSession.value
        if (currentActive != null && !currentActive.isSimulated) {
            if (!isChargingNow) {
                handlePowerDisconnected(intent, isManual = false)
            } else {
                val watts = _powerWatts.value
                val newMaxWatts = maxOf(currentActive.maxPowerWatts, watts)
                val settings = settingsRepo.settings.value
                val targetLevel = settings.targetBatteryLevel

                var targetDuration = currentActive.targetReachedDurationMillis
                var targetTimestamp = currentActive.targetReachedTimestamp
                if (_batteryLevel.value >= targetLevel && targetDuration == null) {
                    targetDuration = currentActive.currentDurationMillis
                    targetTimestamp = System.currentTimeMillis()
                    prefs.edit().putLong(KEY_TARGET_DURATION, targetDuration).apply()
                }

                _activeSession.value = currentActive.copy(
                    currentBatteryLevel = _batteryLevel.value,
                    currentVoltageMv = _voltageMv.value,
                    currentTempC = _temperatureC.value,
                    currentPowerWatts = watts,
                    maxPowerWatts = newMaxWatts,
                    currentMa = _currentMa.value,
                    targetReachedDurationMillis = targetDuration,
                    targetReachedTimestamp = targetTimestamp
                )
            }
        } else if (currentActive == null && isChargingNow) {
            handlePowerConnected(intent, isManual = false)
        }
    }

    private fun updateBatteryTelemetry(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val pct = if (level >= 0 && scale > 0) (level * 100) / scale else level
        if (pct >= 0) _batteryLevel.value = pct

        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        _batteryStatus.value = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "正在充电"
            BatteryManager.BATTERY_STATUS_FULL -> "已充满"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "放电中"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "未充电"
            else -> "未知状态"
        }

        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        val plugStr = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC快充 (插座)"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB 接口"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "无线充电"
            else -> if (plugged > 0) "其他电源" else "未接入"
        }
        _plugType.value = plugStr

        val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
        _voltageMv.value = voltage

        val temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
        _temperatureC.value = temp / 10.0f

        val healthCode = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)
        _health.value = when (healthCode) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "良好"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "过热"
            BatteryManager.BATTERY_HEALTH_DEAD -> "损坏"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "过压"
            else -> "正常"
        }

        // Measure Current & Calculate Wattage
        val isChargingNow = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        if (isChargingNow) {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            var currentMicro = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) ?: 0
            currentMicro = abs(currentMicro)

            var ma = if (currentMicro > 10000) currentMicro / 1000 else currentMicro
            if (ma == 0) {
                // If hardware sensor does not report current, estimate realistic power according to plug
                ma = when {
                    plugStr.contains("快充") || plugStr.contains("AC") -> 4500
                    plugStr.contains("无线") -> 2500
                    else -> 1500
                }
            }
            _currentMa.value = ma

            val vVolts = if (voltage > 0) voltage / 1000f else 4.2f
            val watts = vVolts * (ma / 1000f)
            _powerWatts.value = String.format("%.1f", watts).toFloat()
            powerSamples.add(watts)
        } else {
            _currentMa.value = 0
            _powerWatts.value = 0f
        }
    }

    fun handlePowerConnected(intent: Intent?, isManual: Boolean = false) {
        if (_isCharging.value) return

        if (intent != null) {
            updateBatteryTelemetry(intent)
        }

        val startLevel = _batteryLevel.value
        val plug = if (isManual) "手动模拟 (65W 快充)" else _plugType.value
        val voltage = if (_voltageMv.value > 0) _voltageMv.value else 4200
        val temp = _temperatureC.value

        startNewSession(startLevel, plug, voltage, temp, isManual)
    }

    private fun startNewSession(
        startLevel: Int,
        plug: String,
        voltage: Int,
        temp: Float,
        isSimulated: Boolean
    ) {
        val now = System.currentTimeMillis()
        val settings = settingsRepo.settings.value
        powerSamples.clear()

        val initialWatts = if (isSimulated) 65.0f else (_powerWatts.value.coerceAtLeast(18.0f))
        val active = ActiveSessionState(
            startTimeMillis = now,
            startBatteryLevel = startLevel,
            currentBatteryLevel = startLevel,
            currentDurationMillis = 0L,
            plugType = plug,
            startVoltageMv = voltage,
            currentVoltageMv = voltage,
            startTempC = temp,
            currentTempC = temp,
            isSimulated = isSimulated,
            targetBatteryLevel = settings.targetBatteryLevel,
            stopTimerAtTarget = settings.stopTimerAtTarget,
            currentPowerWatts = initialWatts,
            maxPowerWatts = initialWatts,
            currentMa = _currentMa.value
        )

        prefs.edit()
            .putBoolean(KEY_IS_CHARGING, true)
            .putLong(KEY_START_TIME, now)
            .putInt(KEY_START_LEVEL, startLevel)
            .putString(KEY_PLUG_TYPE, plug)
            .putInt(KEY_START_VOLTAGE, voltage)
            .putFloat(KEY_START_TEMP, temp)
            .putBoolean(KEY_IS_SIMULATED, isSimulated)
            .putInt(KEY_TARGET_LEVEL, settings.targetBatteryLevel)
            .putLong(KEY_TARGET_DURATION, -1L)
            .apply()

        _isCharging.value = true
        _activeSession.value = active

        startDurationTicker()

        NotificationHelper.showChargingNotification(
            context = context,
            durationMillis = 0L,
            startLevel = startLevel,
            currentLevel = startLevel,
            plugType = plug
        )

        // Webhook on start
        if (settings.webhookEnabled && settings.notifyOnStart && settings.webhookUrl.isNotBlank()) {
            scope.launch {
                WebhookSender.sendNotification(
                    webhookUrl = settings.webhookUrl,
                    event = "CHARGING_STARTED",
                    title = "充电开始记录",
                    message = "已接通电源（$plug），当前起始电量 $startLevel%，目标电量 ${settings.targetBatteryLevel}%，应用已自动开启后台计时与功率监测。",
                    durationText = "0秒",
                    batteryLevel = startLevel,
                    powerWatts = initialWatts
                )
            }
        }
    }

    fun recordTargetReached(targetLevel: Int, durationAtTarget: Long) {
        val current = _activeSession.value ?: return
        if (current.targetReachedDurationMillis != null) return

        val updated = current.copy(
            targetReachedDurationMillis = durationAtTarget,
            targetReachedTimestamp = System.currentTimeMillis()
        )
        _activeSession.value = updated
        prefs.edit().putLong(KEY_TARGET_DURATION, durationAtTarget).apply()
    }

    fun handlePowerDisconnected(intent: Intent?, isManual: Boolean = false) {
        if (!_isCharging.value && _activeSession.value == null) return

        if (intent != null) {
            updateBatteryTelemetry(intent)
        }

        stopDurationTicker()

        val active = _activeSession.value
        val now = System.currentTimeMillis()
        val endLevel = _batteryLevel.value
        val endVoltage = _voltageMv.value
        val endTemp = _temperatureC.value
        val settings = settingsRepo.settings.value

        if (active != null) {
            val totalDuration = (now - active.startTimeMillis).coerceAtLeast(1000L)
            val effectiveDuration = if (active.stopTimerAtTarget && active.targetReachedDurationMillis != null) {
                active.targetReachedDurationMillis
            } else {
                totalDuration
            }
            val gained = (endLevel - active.startBatteryLevel).coerceAtLeast(0)

            val avgWatts = if (powerSamples.isNotEmpty()) {
                powerSamples.average().toFloat()
            } else {
                active.currentPowerWatts
            }

            val session = ChargingSession(
                startTimeMillis = active.startTimeMillis,
                endTimeMillis = now,
                durationMillis = effectiveDuration,
                startBatteryLevel = active.startBatteryLevel,
                endBatteryLevel = endLevel,
                batteryGained = gained,
                plugType = active.plugType,
                startVoltageMv = active.startVoltageMv,
                endVoltageMv = endVoltage,
                startTemperatureC = active.startTempC,
                endTemperatureC = endTemp,
                note = if (active.isSimulated) "模拟测试记录" else "",
                isSimulated = active.isSimulated,
                targetBatteryLevel = active.targetBatteryLevel,
                targetReachedDurationMillis = active.targetReachedDurationMillis,
                maxPowerWatts = active.maxPowerWatts,
                avgPowerWatts = String.format("%.1f", avgWatts).toFloat(),
                currentMa = active.currentMa
            )

            scope.launch {
                dao.insertSession(session)
                _lastCompletedSession.value = session
            }

            NotificationHelper.showFinishedNotification(
                context = context,
                durationMillis = effectiveDuration,
                startLevel = active.startBatteryLevel,
                endLevel = endLevel
            )

            // Webhook on finish
            if (settings.webhookEnabled && settings.notifyOnFinish && settings.webhookUrl.isNotBlank()) {
                val durationFormatted = TimeUtils.formatDurationChinese(effectiveDuration)
                scope.launch {
                    WebhookSender.sendNotification(
                        webhookUrl = settings.webhookUrl,
                        event = "CHARGING_FINISHED",
                        title = "充电完成断开",
                        message = "充电器已断开！本次充电用时 $durationFormatted，电量由 ${active.startBatteryLevel}% 升至 $endLevel% (+${gained}%)，平均功率 ${String.format("%.1f", avgWatts)}W，最高功率 ${String.format("%.1f", active.maxPowerWatts)}W。",
                        durationText = durationFormatted,
                        batteryLevel = endLevel,
                        powerWatts = avgWatts
                    )
                }
            }
        }

        prefs.edit().clear().apply()
        _isCharging.value = false
        _activeSession.value = null
        powerSamples.clear()
    }

    private fun concludeSavedSession(
        startTime: Long,
        startLevel: Int,
        plugType: String,
        startVoltage: Int,
        startTemp: Float,
        isSimulated: Boolean,
        targetLevel: Int,
        targetDuration: Long?
    ) {
        val now = System.currentTimeMillis()
        val totalDuration = (now - startTime).coerceAtLeast(1000L)
        val endLevel = _batteryLevel.value
        val gained = (endLevel - startLevel).coerceAtLeast(0)

        val session = ChargingSession(
            startTimeMillis = startTime,
            endTimeMillis = now,
            durationMillis = targetDuration ?: totalDuration,
            startBatteryLevel = startLevel,
            endBatteryLevel = endLevel,
            batteryGained = gained,
            plugType = plugType,
            startVoltageMv = startVoltage,
            endVoltageMv = _voltageMv.value,
            startTemperatureC = startTemp,
            endTemperatureC = _temperatureC.value,
            note = "后台断开记录",
            isSimulated = isSimulated,
            targetBatteryLevel = targetLevel,
            targetReachedDurationMillis = targetDuration
        )

        scope.launch {
            dao.insertSession(session)
            _lastCompletedSession.value = session
        }

        prefs.edit().clear().apply()
        _isCharging.value = false
        _activeSession.value = null
    }

    private fun startDurationTicker() {
        timerJob?.cancel()
        timerJob = scope.launch {
            var tickCount = 0
            while (isActive) {
                delay(1000)
                tickCount++
                val current = _activeSession.value ?: break
                val elapsed = System.currentTimeMillis() - current.startTimeMillis

                val settings = settingsRepo.settings.value
                val targetLevel = settings.targetBatteryLevel

                // Target level check
                var targetDuration = current.targetReachedDurationMillis
                var targetTimestamp = current.targetReachedTimestamp
                if (_batteryLevel.value >= targetLevel && targetDuration == null) {
                    targetDuration = elapsed
                    targetTimestamp = System.currentTimeMillis()
                    prefs.edit().putLong(KEY_TARGET_DURATION, targetDuration).apply()
                }

                _activeSession.value = current.copy(
                    currentDurationMillis = elapsed,
                    currentBatteryLevel = _batteryLevel.value,
                    currentVoltageMv = _voltageMv.value,
                    currentTempC = _temperatureC.value,
                    currentPowerWatts = _powerWatts.value,
                    maxPowerWatts = maxOf(current.maxPowerWatts, _powerWatts.value),
                    currentMa = _currentMa.value,
                    targetReachedDurationMillis = targetDuration,
                    targetReachedTimestamp = targetTimestamp
                )

                if (tickCount % 15 == 0) {
                    NotificationHelper.showChargingNotification(
                        context = context,
                        durationMillis = current.displayDurationMillis,
                        startLevel = current.startBatteryLevel,
                        currentLevel = current.currentBatteryLevel,
                        plugType = current.plugType
                    )
                }
            }
        }
    }

    private fun stopDurationTicker() {
        timerJob?.cancel()
        timerJob = null
    }

    // --- Simulation Support for AI Studio Emulator ---
    fun simulateConnect(startPercent: Int = 30, plug: String = "AC 快充 (65W)") {
        _batteryLevel.value = startPercent
        _plugType.value = plug
        _batteryStatus.value = "正在充电"
        _voltageMv.value = 4250
        _temperatureC.value = 29.5f
        _currentMa.value = 5200
        _powerWatts.value = 65.0f
        startNewSession(startPercent, plug, 4250, 29.5f, isSimulated = true)
        ChargingForegroundService.start(context)
    }

    fun simulateAdvanceBattery(newPercent: Int) {
        _batteryLevel.value = newPercent
        val current = _activeSession.value
        if (current != null) {
            val settings = settingsRepo.settings.value
            val targetLevel = settings.targetBatteryLevel

            var targetDuration = current.targetReachedDurationMillis
            var targetTimestamp = current.targetReachedTimestamp
            if (newPercent >= targetLevel && targetDuration == null) {
                targetDuration = current.currentDurationMillis
                targetTimestamp = System.currentTimeMillis()
                prefs.edit().putLong(KEY_TARGET_DURATION, targetDuration).apply()
            }

            _activeSession.value = current.copy(
                currentBatteryLevel = newPercent,
                targetReachedDurationMillis = targetDuration,
                targetReachedTimestamp = targetTimestamp
            )
        }
    }

    fun simulateDisconnect(endPercent: Int? = null) {
        if (endPercent != null) {
            _batteryLevel.value = endPercent
        }
        _batteryStatus.value = "未充电"
        _plugType.value = "未接入"
        handlePowerDisconnected(intent = null, isManual = true)
        ChargingForegroundService.stop(context)
    }

    fun seedSampleHistory() {
        scope.launch {
            val now = System.currentTimeMillis()
            val samples = listOf(
                ChargingSession(
                    startTimeMillis = now - 3600_000 * 4,
                    endTimeMillis = now - 3600_000 * 4 + 38 * 60_000,
                    durationMillis = 38 * 60_000,
                    startBatteryLevel = 22,
                    endBatteryLevel = 80,
                    batteryGained = 58,
                    plugType = "AC快充 (插座)",
                    startVoltageMv = 3850,
                    endVoltageMv = 4320,
                    startTemperatureC = 27.5f,
                    endTemperatureC = 34.2f,
                    note = "65W GaN快充",
                    isSimulated = false,
                    targetBatteryLevel = 80,
                    targetReachedDurationMillis = 38 * 60_000,
                    maxPowerWatts = 63.8f,
                    avgPowerWatts = 42.5f,
                    currentMa = 5100
                ),
                ChargingSession(
                    startTimeMillis = now - 3600_000 * 24,
                    endTimeMillis = now - 3600_000 * 24 + 75 * 60_000,
                    durationMillis = 75 * 60_000,
                    startBatteryLevel = 15,
                    endBatteryLevel = 100,
                    batteryGained = 85,
                    plugType = "AC快充 (插座)",
                    startVoltageMv = 3700,
                    endVoltageMv = 4400,
                    startTemperatureC = 25.0f,
                    endTemperatureC = 32.1f,
                    note = "夜间充饱",
                    isSimulated = false,
                    targetBatteryLevel = 100,
                    targetReachedDurationMillis = 75 * 60_000,
                    maxPowerWatts = 45.0f,
                    avgPowerWatts = 28.2f,
                    currentMa = 3800
                ),
                ChargingSession(
                    startTimeMillis = now - 3600_000 * 46,
                    endTimeMillis = now - 3600_000 * 46 + 25 * 60_000,
                    durationMillis = 25 * 60_000,
                    startBatteryLevel = 45,
                    endBatteryLevel = 80,
                    batteryGained = 35,
                    plugType = "无线充电",
                    startVoltageMv = 3980,
                    endVoltageMv = 4220,
                    startTemperatureC = 28.8f,
                    endTemperatureC = 35.6f,
                    note = "桌面立式无线充",
                    isSimulated = false,
                    targetBatteryLevel = 80,
                    targetReachedDurationMillis = 25 * 60_000,
                    maxPowerWatts = 15.0f,
                    avgPowerWatts = 12.8f,
                    currentMa = 2600
                ),
                ChargingSession(
                    startTimeMillis = now - 3600_000 * 70,
                    endTimeMillis = now - 3600_000 * 70 + 55 * 60_000,
                    durationMillis = 55 * 60_000,
                    startBatteryLevel = 30,
                    endBatteryLevel = 85,
                    batteryGained = 55,
                    plugType = "USB 接口",
                    startVoltageMv = 3880,
                    endVoltageMv = 4290,
                    startTemperatureC = 24.5f,
                    endTemperatureC = 28.5f,
                    note = "电脑主机前置USB",
                    isSimulated = false,
                    targetBatteryLevel = 85,
                    targetReachedDurationMillis = 55 * 60_000,
                    maxPowerWatts = 10.5f,
                    avgPowerWatts = 9.2f,
                    currentMa = 1800
                )
            )
            samples.forEach { dao.insertSession(it) }
        }
    }

    fun dismissLastCompletedSession() {
        _lastCompletedSession.value = null
    }

    companion object {
        private const val KEY_IS_CHARGING = "key_is_charging"
        private const val KEY_START_TIME = "key_start_time"
        private const val KEY_START_LEVEL = "key_start_level"
        private const val KEY_PLUG_TYPE = "key_plug_type"
        private const val KEY_START_VOLTAGE = "key_start_voltage"
        private const val KEY_START_TEMP = "key_start_temp"
        private const val KEY_IS_SIMULATED = "key_is_simulated"
        private const val KEY_TARGET_LEVEL = "key_target_level"
        private const val KEY_TARGET_DURATION = "key_target_duration"

        @Volatile
        private var INSTANCE: ChargingTracker? = null

        fun getInstance(context: Context): ChargingTracker {
            return INSTANCE ?: synchronized(this) {
                val instance = ChargingTracker(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
