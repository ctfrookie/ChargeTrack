package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

@Composable
fun SessionFinishedDialog(
    session: ChargingSession,
    onDismiss: () -> Unit,
    onSaveNote: (String) -> Unit
) {
    var noteText by remember { mutableStateOf(session.note) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(ElectricEmerald.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = ElectricEmerald,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "充电完成已归档",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "电源已断开，数据已写入本地数据库",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Key Metrics
                MetricRow(
                    icon = Icons.Default.Timer,
                    label = "本次充电耗时",
                    value = TimeUtils.formatDurationChinese(session.durationMillis),
                    color = ElectricEmerald
                )

                if (session.targetReachedDurationMillis != null) {
                    MetricRow(
                        icon = Icons.Default.CheckCircle,
                        label = "充至${session.targetBatteryLevel}%耗时",
                        value = TimeUtils.formatDurationChinese(session.targetReachedDurationMillis),
                        color = Color(0xFFF59E0B)
                    )
                }

                MetricRow(
                    icon = Icons.Default.TrendingUp,
                    label = "充入电量",
                    value = "${session.startBatteryLevel}% ➔ ${session.endBatteryLevel}% (+${session.batteryGained}%)",
                    color = ElectricCyan
                )

                MetricRow(
                    icon = Icons.Default.Bolt,
                    label = "充电功率",
                    value = if (session.avgPowerWatts > 0f) "平均 ${session.avgPowerWatts}W (最高 ${session.maxPowerWatts}W)" else "标准充速",
                    color = ElectricEmerald
                )

                MetricRow(
                    icon = Icons.Default.Speed,
                    label = "充入速率",
                    value = TimeUtils.formatRate(session.ratePercentPerMinute),
                    color = Color(0xFFEC4899)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Optional Note
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("添加备注 (例如: 65W快充/床头充电)") },
                    placeholder = { Text("例如: GaN快充头") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (noteText != session.note) {
                        onSaveNote(noteText)
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricEmerald)
            ) {
                Text("完成", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Composable
private fun MetricRow(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
