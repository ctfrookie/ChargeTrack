package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ActiveSessionState
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricEmerald
import com.example.util.TimeUtils

@Composable
fun ChargingTimerDisplay(
    activeSession: ActiveSessionState?,
    isCharging: Boolean,
    targetBatteryLevel: Int = 80,
    stopTimerAtTarget: Boolean = true,
    modifier: Modifier = Modifier
) {
    val durationMillis = activeSession?.displayDurationMillis ?: 0L
    val digitalTimer = TimeUtils.formatDigitalTimer(durationMillis)
    val chineseDuration = TimeUtils.formatDurationChinese(durationMillis)
    val isTargetReached = activeSession?.isTargetReached == true

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = when {
                    isTargetReached -> Color(0xFFF59E0B)
                    isCharging -> ElectricEmerald.copy(alpha = 0.5f)
                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                },
                shape = RoundedCornerShape(24.dp)
            )
            .padding(20.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Status Header Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        when {
                            isTargetReached -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                            isCharging -> ElectricEmerald.copy(alpha = 0.15f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = when {
                        isTargetReached -> Icons.Default.CheckCircle
                        isCharging -> Icons.Default.Timer
                        else -> Icons.Default.Timer
                    },
                    contentDescription = null,
                    tint = when {
                        isTargetReached -> Color(0xFFF59E0B)
                        isCharging -> ElectricEmerald
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = when {
                        isTargetReached && stopTimerAtTarget -> "已达成 $targetBatteryLevel% 目标 (计时已锁定)"
                        isCharging -> "充电持续时长 (目标 $targetBatteryLevel%)"
                        else -> "等待接入充电器"
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = when {
                        isTargetReached -> Color(0xFFF59E0B)
                        isCharging -> ElectricEmerald
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Large Digital StopWatch
            Text(
                text = digitalTimer,
                fontSize = 46.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp,
                color = when {
                    isTargetReached -> Color(0xFFF59E0B)
                    isCharging -> ElectricEmerald
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            if (isCharging) {
                Text(
                    text = if (isTargetReached && stopTimerAtTarget) "达成目标用时：$chineseDuration" else "已充入时长：$chineseDuration",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "接通电源后台自动启动，充至目标 $targetBatteryLevel% 自动锁定",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Metric badges when charging
            AnimatedVisibility(visible = isCharging && activeSession != null) {
                if (activeSession != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Power highlight banner
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(ElectricCyan.copy(alpha = 0.12f))
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "实时充电功率",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ElectricCyan
                                )
                            }
                            Text(
                                text = "${String.format("%.1f", activeSession.currentPowerWatts)} W",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        // 3 Sub-Metrics
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Battery Gained
                            MetricItem(
                                icon = Icons.Default.TrendingUp,
                                label = "电量提升",
                                value = "${activeSession.startBatteryLevel}% ➔ ${activeSession.currentBatteryLevel}%",
                                subValue = "+${activeSession.batteryGained}%",
                                highlightColor = ElectricEmerald
                            )

                            // Charging Speed
                            MetricItem(
                                icon = Icons.Default.Speed,
                                label = "充电速率",
                                value = TimeUtils.formatRate(activeSession.ratePercentPerMinute),
                                subValue = "平均速率",
                                highlightColor = ElectricCyan
                            )

                            // Estimated to target
                            MetricItem(
                                icon = Icons.Default.HourglassTop,
                                label = "充至 $targetBatteryLevel%",
                                value = if (isTargetReached) "已达成" else (activeSession.estimatedMinutesToTarget?.let { "${it}分钟" } ?: "--"),
                                subValue = if (isTargetReached) "已完成" else "预估剩余",
                                highlightColor = Color(0xFFF59E0B)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    subValue: String,
    highlightColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = highlightColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = subValue,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = highlightColor
        )
    }
}
