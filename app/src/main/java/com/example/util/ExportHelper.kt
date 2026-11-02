package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.model.ChargingSession
import org.json.JSONArray
import org.json.JSONObject

object ExportHelper {

    fun generateCsv(sessions: List<ChargingSession>): String {
        val sb = StringBuilder()
        sb.append("ID,开始时间,结束时间,有效时长,总插电时长,起始电量(%),结束电量(%),充入电量(%),目标电量(%),目标达成耗时,充电类型,最高功率(W),平均功率(W),起始电压(mV),结束电压(mV),起始温度(℃),结束温度(℃),备注\n")

        sessions.forEach { s ->
            val targetDurationStr = s.targetReachedDurationMillis?.let { TimeUtils.formatDurationChinese(it) } ?: "未记录"
            val effectiveDuration = TimeUtils.formatDurationChinese(s.durationMillis)
            val totalPluggedDuration = TimeUtils.formatDurationChinese(s.endTimeMillis - s.startTimeMillis)

            sb.append("${s.id},")
            sb.append("\"${TimeUtils.formatDateTime(s.startTimeMillis)}\",")
            sb.append("\"${TimeUtils.formatDateTime(s.endTimeMillis)}\",")
            sb.append("\"$effectiveDuration\",")
            sb.append("\"$totalPluggedDuration\",")
            sb.append("${s.startBatteryLevel},")
            sb.append("${s.endBatteryLevel},")
            sb.append("${s.batteryGained},")
            sb.append("${s.targetBatteryLevel},")
            sb.append("\"$targetDurationStr\",")
            sb.append("\"${s.plugType}\",")
            sb.append("${s.maxPowerWatts},")
            sb.append("${s.avgPowerWatts},")
            sb.append("${s.startVoltageMv},")
            sb.append("${s.endVoltageMv},")
            sb.append("${s.startTemperatureC},")
            sb.append("${s.endTemperatureC},")
            sb.append("\"${s.note.replace("\"", "\"\"")}\"\n")
        }
        return sb.toString()
    }

    fun generateJson(sessions: List<ChargingSession>): String {
        val rootArray = JSONArray()
        sessions.forEach { s ->
            val obj = JSONObject().apply {
                put("id", s.id)
                put("start_time", TimeUtils.formatDateTime(s.startTimeMillis))
                put("end_time", TimeUtils.formatDateTime(s.endTimeMillis))
                put("duration_formatted", TimeUtils.formatDurationChinese(s.durationMillis))
                put("duration_millis", s.durationMillis)
                put("start_battery_level", s.startBatteryLevel)
                put("end_battery_level", s.endBatteryLevel)
                put("battery_gained", s.batteryGained)
                put("target_battery_level", s.targetBatteryLevel)
                put("target_duration_millis", s.targetReachedDurationMillis ?: JSONObject.NULL)
                put("plug_type", s.plugType)
                put("max_power_watts", s.maxPowerWatts.toDouble())
                put("avg_power_watts", s.avgPowerWatts.toDouble())
                put("note", s.note)
            }
            rootArray.put(obj)
        }
        return rootArray.toString(2)
    }

    fun generateSummaryText(sessions: List<ChargingSession>): String {
        if (sessions.isEmpty()) return "暂无充电数据记录"
        val totalMillis = sessions.sumOf { it.durationMillis }
        val totalGained = sessions.sumOf { it.batteryGained }
        val avgWatts = if (sessions.isNotEmpty()) sessions.map { it.avgPowerWatts }.average() else 0.0

        val sb = StringBuilder()
        sb.append("📊【充电计时·数据分析汇总报表】\n")
        sb.append("----------------------------\n")
        sb.append("• 累计记录次数：${sessions.size} 次\n")
        sb.append("• 累计充电总时长：${TimeUtils.formatDurationChinese(totalMillis)}\n")
        sb.append("• 累计充入电量：+$totalGained%\n")
        sb.append("• 平均单次充电功率：${String.format("%.1f", avgWatts)}W\n")
        sb.append("----------------------------\n")
        sb.append("最近记录：\n")
        sessions.take(5).forEachIndexed { idx, s ->
            sb.append("${idx + 1}. ${TimeUtils.formatDateGroup(s.startTimeMillis)} ${TimeUtils.formatTimeOnly(s.startTimeMillis)}: ")
            sb.append("${TimeUtils.formatDurationChinese(s.durationMillis)} (${s.startBatteryLevel}%➔${s.endBatteryLevel}%) [${s.plugType}]\n")
        }
        return sb.toString()
    }

    fun shareContent(context: Context, content: String, title: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, content)
            putExtra(Intent.EXTRA_TITLE, title)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "导出与分享充电记录")
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    }

    fun copyToClipboard(context: Context, text: String, toastMsg: String = "已复制到剪贴板") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Charging Records", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
    }
}
