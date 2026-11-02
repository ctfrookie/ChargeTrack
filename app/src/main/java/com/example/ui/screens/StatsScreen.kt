package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChargingSession
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricEmerald
import com.example.util.TimeUtils
import java.util.Calendar
import kotlin.math.roundToInt

@Composable
fun StatsScreen(
    sessions: List<ChargingSession>,
    onOpenExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val totalDurationMillis = remember(sessions) { sessions.sumOf { it.durationMillis } }
    val totalGained = remember(sessions) { sessions.sumOf { it.batteryGained } }
    val avgDurationMillis = remember(sessions) {
        if (sessions.isNotEmpty()) totalDurationMillis / sessions.size else 0L
    }

    val avgPowerWatts = remember(sessions) {
        val withPower = sessions.filter { it.avgPowerWatts > 0f }
        if (withPower.isNotEmpty()) withPower.map { it.avgPowerWatts }.average().toFloat() else 0f
    }

    val maxPowerWatts = remember(sessions) {
        sessions.maxOfOrNull { it.maxPowerWatts } ?: 0f
    }

    // Average time to target level
    val avgTimeToTarget = remember(sessions) {
        val targetSessions = sessions.mapNotNull { it.targetReachedDurationMillis }
        if (targetSessions.isNotEmpty()) targetSessions.average().toLong() else null
    }

    // Charging Source Breakdown
    val acCount = remember(sessions) { sessions.count { it.plugType.contains("AC") || it.plugType.contains("快充") } }
    val usbCount = remember(sessions) { sessions.count { it.plugType.contains("USB") } }
    val wirelessCount = remember(sessions) { sessions.count { it.plugType.contains("无线") } }

    // Time of day breakdown
    val timeOfDayDistribution = remember(sessions) {
        val map = mutableMapOf("早晨 (6-12点)" to 0, "下午 (12-18点)" to 0, "晚间 (18-24点)" to 0, "深夜 (0-6点)" to 0)
        val cal = Calendar.getInstance()
        sessions.forEach { s ->
            cal.timeInMillis = s.startTimeMillis
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            when (hour) {
                in 6..11 -> map["早晨 (6-12点)"] = (map["早晨 (6-12点)"] ?: 0) + 1
                in 12..17 -> map["下午 (12-18点)"] = (map["下午 (12-18点)"] ?: 0) + 1
                in 18..23 -> map["晚间 (18-24点)"] = (map["晚间 (18-24点)"] ?: 0) + 1
                else -> map["深夜 (0-6点)"] = (map["深夜 (0-6点)"] ?: 0) + 1
            }
        }
        map
    }

    val recentSessions = remember(sessions) { sessions.take(8) }
    val maxDurationInRecents = remember(recentSessions) {
        recentSessions.maxOfOrNull { it.durationMillis }?.coerceAtLeast(60_000L) ?: 3600_000L
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "充电数据深度分析",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "功率统计 · 目标效率 · 充电习惯图表",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (sessions.isNotEmpty()) {
                Button(
                    onClick = onOpenExport,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricEmerald)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("导出报表", fontSize = 12.sp, color = Color.White)
                }
            }
        }

        // 4 KPI Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                title = "累计总时长",
                value = TimeUtils.formatDurationChinese(totalDurationMillis, includeSeconds = false),
                sub = "共 ${sessions.size} 次充电记录",
                icon = Icons.Default.Timer,
                color = ElectricEmerald,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "平均单次时长",
                value = if (sessions.isNotEmpty()) TimeUtils.formatDurationChinese(avgDurationMillis, includeSeconds = false) else "--",
                sub = "单次周期均值",
                icon = Icons.Default.AvTimer,
                color = ElectricCyan,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                title = "平均充电功率",
                value = if (avgPowerWatts > 0f) "${String.format("%.1f", avgPowerWatts)} W" else "快充分析中",
                sub = "最高峰值 ${String.format("%.1f", maxPowerWatts)}W",
                icon = Icons.Default.Bolt,
                color = Color(0xFFF59E0B),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "充至目标均耗时",
                value = avgTimeToTarget?.let { TimeUtils.formatDurationChinese(it, includeSeconds = false) } ?: "进行中",
                sub = "预设目标达成效率",
                icon = Icons.Default.CheckCircle,
                color = Color(0xFFEC4899),
                modifier = Modifier.weight(1f)
            )
        }

        // Recent Durations Chart
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        tint = ElectricEmerald,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "近期单次充电时长与功率分布",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                if (recentSessions.isEmpty()) {
                    Text(
                        text = "暂无数据，充电断开后将自动绘制统计柱状图",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        recentSessions.forEach { item ->
                            val progress = (item.durationMillis.toFloat() / maxDurationInRecents).coerceIn(0.08f, 1f)
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${TimeUtils.formatDateGroup(item.startTimeMillis)} ${TimeUtils.formatTimeOnly(item.startTimeMillis)} · ${item.plugType}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${TimeUtils.formatDurationChinese(item.durationMillis)} (+${item.batteryGained}%)" +
                                                (if (item.avgPowerWatts > 0f) " · ${item.avgPowerWatts}W" else ""),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ElectricEmerald
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(progress)
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(ElectricEmerald)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Power Source & Time of Day Distribution Analysis
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PieChart,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "充电来源与时段习惯分析",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                // Sources
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "充电方式分布：", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        DistributionPill("⚡ 快充/AC", acCount, ElectricEmerald)
                        DistributionPill("🔌 USB接口", usbCount, ElectricCyan)
                        DistributionPill("🛜 无线充", wirelessCount, Color(0xFFF59E0B))
                    }
                }

                // Time of day
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "常用充电时段：", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    timeOfDayDistribution.forEach { (period, count) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = period, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "$count 次", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                }
            }
        }

        // Export and Share banner
        OutlinedButton(
            onClick = onOpenExport,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.FileDownload, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("导出全量 CSV 报表与数据分析结果")
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun DistributionPill(label: String, count: Int, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(text = label, fontSize = 11.sp, color = color, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = "$count 次", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    sub: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(14.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1
            )
            Text(
                text = sub,
                fontSize = 11.sp,
                color = color,
                maxLines = 1
            )
        }
    }
}
