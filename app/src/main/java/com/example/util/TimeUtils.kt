package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object TimeUtils {

    /**
     * Formats duration in Chinese readable format: e.g. "1小时24分30秒", "45分12秒", "30秒"
     */
    fun formatDurationChinese(durationMillis: Long, includeSeconds: Boolean = true): String {
        val totalSeconds = (durationMillis / 1000).coerceAtLeast(0)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return when {
            hours > 0 -> {
                if (includeSeconds && seconds > 0) {
                    "${hours}小时${minutes}分${seconds}秒"
                } else {
                    "${hours}小时${minutes}分"
                }
            }
            minutes > 0 -> {
                if (includeSeconds) {
                    "${minutes}分${seconds}秒"
                } else {
                    "${minutes}分钟"
                }
            }
            else -> {
                "${seconds}秒"
            }
        }
    }

    /**
     * Formats duration into digital clock style: e.g. "00:15:32" or "01:24:05"
     */
    fun formatDigitalTimer(durationMillis: Long): String {
        val totalSeconds = (durationMillis / 1000).coerceAtLeast(0)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }

    /**
     * Formats timestamp into full date and time: "2026-11-01 14:32"
     */
    fun formatDateTime(millis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    /**
     * Formats timestamp into time only: "14:32:05"
     */
    fun formatTimeWithSeconds(millis: Long): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    /**
     * Formats timestamp into time only: "14:32"
     */
    fun formatTimeOnly(millis: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    /**
     * Formats day header: "今天", "昨天", or "yyyy年MM月dd日"
     */
    fun formatDateGroup(millis: Long): String {
        val targetCalendar = Calendar.getInstance().apply { timeInMillis = millis }
        val now = Calendar.getInstance()

        val isSameYear = targetCalendar.get(Calendar.YEAR) == now.get(Calendar.YEAR)
        val targetDayOfYear = targetCalendar.get(Calendar.DAY_OF_YEAR)
        val currentDayOfYear = now.get(Calendar.DAY_OF_YEAR)

        return when {
            isSameYear && targetDayOfYear == currentDayOfYear -> "今天"
            isSameYear && targetDayOfYear == currentDayOfYear - 1 -> "昨天"
            isSameYear -> SimpleDateFormat("MM月dd日", Locale.getDefault()).format(Date(millis))
            else -> SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault()).format(Date(millis))
        }
    }

    /**
     * Formats charging rate: "+0.85%/分"
     */
    fun formatRate(ratePerMinute: Double): String {
        return if (ratePerMinute > 0) {
            String.format(Locale.getDefault(), "+%.2f%%/分", ratePerMinute)
        } else {
            "--"
        }
    }
}
