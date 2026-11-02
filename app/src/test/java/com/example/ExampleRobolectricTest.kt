package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.ChargingSession
import com.example.util.TimeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("充电计时", appName)
    }

    @Test
    fun `test duration formatting`() {
        val formatted = TimeUtils.formatDurationChinese(90_000L) // 1m 30s
        assertEquals("1分30秒", formatted)

        val hourFormatted = TimeUtils.formatDurationChinese(3600_000L + 120_000L) // 1h 2m
        assertEquals("1小时2分", hourFormatted)

        val digital = TimeUtils.formatDigitalTimer(65_000L) // 01:05
        assertEquals("01:05", digital)
    }

    @Test
    fun `test charging session rate calculation`() {
        val session = ChargingSession(
            startTimeMillis = 1000L,
            endTimeMillis = 1000L + 60 * 60_000L, // 60 minutes
            durationMillis = 60 * 60_000L,
            startBatteryLevel = 20,
            endBatteryLevel = 80,
            batteryGained = 60,
            plugType = "AC 快充"
        )
        // 60% gained over 60 min = 1.0%/min = 60%/hr
        assertEquals(1.0, session.ratePercentPerMinute, 0.01)
        assertEquals(60.0, session.ratePercentPerHour, 0.01)
    }
}
