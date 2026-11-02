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
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.PowerOff
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricEmerald
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualSimulationSheet(
    isCharging: Boolean,
    currentBatteryLevel: Int,
    onDismiss: () -> Unit,
    onSimulateConnect: (startPercent: Int, plugType: String) -> Unit,
    onSimulateAdvanceBattery: (newPercent: Int) -> Unit,
    onSimulateDisconnect: (endPercent: Int) -> Unit,
    onSeedSampleData: () -> Unit
) {
    var selectedPercent by remember { mutableFloatStateOf(currentBatteryLevel.toFloat()) }
    var selectedPlug by remember { mutableStateOf("AC 快充 (65W)") }

    val plugOptions = listOf("AC 快充 (65W)", "USB 接口", "无线充电")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ElectricCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "充电模拟与测试控制",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "在模拟器中即刻体验插拔与计时记录",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Status chip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isCharging) ElectricEmerald.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isCharging) Icons.Default.Bolt else Icons.Default.Cable,
                        contentDescription = null,
                        tint = if (isCharging) ElectricEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isCharging) "当前状态：已接通电源，正在计时中" else "当前状态：未接入电源，等待充电",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isCharging) ElectricEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Battery Level Slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isCharging) "调整当前电量" else "模拟起始电量",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${selectedPercent.roundToInt()}%",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricEmerald
                    )
                }
                Slider(
                    value = selectedPercent,
                    onValueChange = {
                        selectedPercent = it
                        if (isCharging) {
                            onSimulateAdvanceBattery(it.roundToInt())
                        }
                    },
                    valueRange = 1f..100f
                )
            }

            // Plug type selector
            Column {
                Text(
                    text = "模拟充电类型",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    plugOptions.forEach { option ->
                        FilterChip(
                            selected = selectedPlug == option,
                            onClick = { selectedPlug = option },
                            label = { Text(option, fontSize = 12.sp) }
                        )
                    }
                }
            }

            // Action Buttons
            if (!isCharging) {
                Button(
                    onClick = {
                        onSimulateConnect(selectedPercent.roundToInt(), selectedPlug)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricEmerald)
                ) {
                    Icon(Icons.Default.FlashOn, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("模拟插上充电器 (开始计时)", color = Color.White, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = {
                        onSimulateDisconnect(selectedPercent.roundToInt())
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Icon(Icons.Default.PowerOff, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("模拟拔掉充电器 (保存记录)", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            // Seed Sample Data
            OutlinedButton(
                onClick = {
                    onSeedSampleData()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.HistoryEdu, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("一键填充示例历史记录")
            }
        }
    }
}
