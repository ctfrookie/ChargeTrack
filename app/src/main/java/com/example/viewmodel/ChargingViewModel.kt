package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ActiveSessionState
import com.example.data.AppDatabase
import com.example.data.SettingsRepository
import com.example.data.SettingsState
import com.example.model.ChargingSession
import com.example.tracker.ChargingTracker
import com.example.util.ExportHelper
import com.example.util.WebhookSender
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChargingViewModel(application: Application) : AndroidViewModel(application) {

    private val tracker = ChargingTracker.getInstance(application)
    private val database = AppDatabase.getInstance(application)
    private val dao = database.chargingSessionDao()
    private val settingsRepo = SettingsRepository(application)

    val allSessions: StateFlow<List<ChargingSession>> = dao.getAllSessions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val isCharging: StateFlow<Boolean> = tracker.isCharging
    val activeSession: StateFlow<ActiveSessionState?> = tracker.activeSession
    val batteryLevel: StateFlow<Int> = tracker.batteryLevel
    val batteryStatus: StateFlow<String> = tracker.batteryStatus
    val plugType: StateFlow<String> = tracker.plugType
    val voltageMv: StateFlow<Int> = tracker.voltageMv
    val currentMa: StateFlow<Int> = tracker.currentMa
    val powerWatts: StateFlow<Float> = tracker.powerWatts
    val temperatureC: StateFlow<Float> = tracker.temperatureC
    val health: StateFlow<String> = tracker.health
    val lastCompletedSession: StateFlow<ChargingSession?> = tracker.lastCompletedSession
    val settings: StateFlow<SettingsState> = settingsRepo.settings

    fun updateSessionNote(sessionId: Long, note: String) {
        viewModelScope.launch {
            val session = dao.getSessionById(sessionId)
            if (session != null) {
                dao.updateSession(session.copy(note = note))
            }
        }
    }

    fun deleteSession(session: ChargingSession) {
        viewModelScope.launch {
            dao.deleteSession(session)
        }
    }

    fun deleteAllSessions() {
        viewModelScope.launch {
            dao.deleteAllSessions()
        }
    }

    fun seedSampleData() {
        tracker.seedSampleHistory()
    }

    fun simulateConnect(startPercent: Int, plug: String) {
        tracker.simulateConnect(startPercent, plug)
    }

    fun simulateAdvanceBattery(newPercent: Int) {
        tracker.simulateAdvanceBattery(newPercent)
    }

    fun simulateDisconnect(endPercent: Int? = null) {
        tracker.simulateDisconnect(endPercent)
    }

    fun dismissLastCompleted() {
        tracker.dismissLastCompletedSession()
    }

    // Settings actions
    fun setTargetBatteryLevel(level: Int) = settingsRepo.updateTargetBatteryLevel(level)
    fun setStopTimerAtTarget(stop: Boolean) = settingsRepo.updateStopTimerAtTarget(stop)
    fun setWebhookUrl(url: String) = settingsRepo.updateWebhookUrl(url)
    fun setWebhookEnabled(enabled: Boolean) = settingsRepo.updateWebhookEnabled(enabled)
    fun setNotifyOnStart(enabled: Boolean) = settingsRepo.updateNotifyOnStart(enabled)
    fun setNotifyOnTarget(enabled: Boolean) = settingsRepo.updateNotifyOnTarget(enabled)
    fun setNotifyOnFinish(enabled: Boolean) = settingsRepo.updateNotifyOnFinish(enabled)
    fun setVibrateOnTarget(enabled: Boolean) = settingsRepo.updateVibrateOnTarget(enabled)

    fun testWebhook(url: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val res = WebhookSender.testWebhook(url)
            onResult(res.first, res.second)
        }
    }

    fun exportCsv(context: Context) {
        val sessions = allSessions.value
        val csv = ExportHelper.generateCsv(sessions)
        ExportHelper.shareContent(context, csv, "充电时长记录表.csv")
    }

    fun exportJson(context: Context) {
        val sessions = allSessions.value
        val json = ExportHelper.generateJson(sessions)
        ExportHelper.shareContent(context, json, "充电记录数据.json")
    }

    fun copySummaryReport(context: Context) {
        val sessions = allSessions.value
        val summary = ExportHelper.generateSummaryText(sessions)
        ExportHelper.copyToClipboard(context, summary, "数据分析报表已复制到剪贴板")
    }
}
