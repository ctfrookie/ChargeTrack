package com.example.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object WebhookSender {
    private const val TAG = "WebhookSender"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    suspend fun sendNotification(
        webhookUrl: String,
        event: String,
        title: String,
        message: String,
        durationText: String = "",
        batteryLevel: Int = 0,
        powerWatts: Float = 0f,
        extraData: Map<String, Any> = emptyMap()
    ): Boolean = withContext(Dispatchers.IO) {
        if (webhookUrl.isBlank()) return@withContext false

        try {
            // Build versatile multi-platform compatible payload
            val fullContent = "【充电计时】$title\n$message" +
                    (if (durationText.isNotBlank()) "\n⏱️ 持续耗时：$durationText" else "") +
                    (if (powerWatts > 0f) "\n⚡ 实时功率：${String.format("%.1f", powerWatts)}W" else "")

            val rootJson = JSONObject().apply {
                put("event", event)
                put("title", title)
                put("message", message)
                put("duration", durationText)
                put("battery", batteryLevel)
                put("power_watts", powerWatts.toDouble())
                put("timestamp", System.currentTimeMillis())

                // DingTalk & WeCom compatible structure
                put("msgtype", "text")
                put("text", JSONObject().put("content", fullContent))

                // Feishu compatible structure
                put("msg_type", "text")
                put("content", JSONObject().put("text", fullContent))

                // Additional custom data
                extraData.forEach { (k, v) -> put(k, v) }
            }

            val requestBody = rootJson.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url(webhookUrl.trim())
                .post(requestBody)
                .header("User-Agent", "ChargeTimer-Android/2.0")
                .build()

            client.newCall(request).execute().use { response ->
                val isSuccess = response.isSuccessful
                Log.d(TAG, "Webhook send event=$event status=${response.code} success=$isSuccess")
                return@withContext isSuccess
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send webhook to $webhookUrl: ${e.message}")
            return@withContext false
        }
    }

    suspend fun testWebhook(webhookUrl: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (webhookUrl.isBlank()) return@withContext Pair(false, "Webhook URL 不能为空")
        try {
            val success = sendNotification(
                webhookUrl = webhookUrl,
                event = "TEST_PING",
                title = "Webhook 测试通知",
                message = "恭喜！充电计时应用的 Webhook 已经成功连接，后续充电事件将自动推送。",
                durationText = "00:00:00",
                batteryLevel = 100,
                powerWatts = 65.0f
            )
            if (success) {
                Pair(true, "测试通知发送成功！请检查接收端。")
            } else {
                Pair(false, "发送失败，请检查 URL 是否有效或接收端服务状态。")
            }
        } catch (e: Exception) {
            Pair(false, "错误：${e.localizedMessage ?: "连接失败"}")
        }
    }
}
