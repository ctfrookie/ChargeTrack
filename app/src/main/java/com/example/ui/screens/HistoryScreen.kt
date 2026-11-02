package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChargingSession
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricEmerald
import com.example.util.TimeUtils

@Composable
fun HistoryScreen(
    sessions: List<ChargingSession>,
    onDeleteSession: (ChargingSession) -> Unit,
    onUpdateNote: (sessionId: Long, note: String) -> Unit,
    onClearAll: () -> Unit,
    onOpenExport: () -> Unit,
    onSeedSampleData: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("全部") }
    var sessionToEditNote by remember { mutableStateOf<ChargingSession?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }

    val filterOptions = listOf("全部", "AC快充", "USB", "无线")

    val filteredSessions = remember(sessions, selectedFilter) {
        when (selectedFilter) {
            "AC快充" -> sessions.filter { it.plugType.contains("AC") || it.plugType.contains("快充") }
            "USB" -> sessions.filter { it.plugType.contains("USB") }
            "无线" -> sessions.filter { it.plugType.contains("无线") }
            else -> sessions
        }
    }

    val totalDurationMillis = remember(sessions) {
        sessions.sumOf { it.durationMillis }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "充电时长记录",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "共记录 ${sessions.size} 次 · 累计时长 ${TimeUtils.formatDurationChinese(totalDurationMillis, includeSeconds = false)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Export Button
                if (sessions.isNotEmpty()) {
                    IconButton(
                        onClick = onOpenExport,
                        modifier = Modifier.testTag("export_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "导出记录",
                            tint = ElectricEmerald
                        )
                    }

                    IconButton(
                        onClick = { showClearConfirm = true },
                        modifier = Modifier.testTag("clear_all_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "清空记录",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Filter chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filterOptions.forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter, fontSize = 12.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredSessions.isEmpty()) {
            EmptyHistoryView(
                onSeedSampleData = onSeedSampleData,
                isFiltered = sessions.isNotEmpty()
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredSessions, key = { it.id }) { session ->
                    SessionCard(
                        session = session,
                        onDelete = { onDeleteSession(session) },
                        onEditNote = { sessionToEditNote = session }
                    )
                }
            }
        }
    }

    // Edit Note Dialog
    sessionToEditNote?.let { session ->
        var noteInput by remember { mutableStateOf(session.note) }
        AlertDialog(
            onDismissRequest = { sessionToEditNote = null },
            title = { Text("编辑记录备注") },
            text = {
                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    label = { Text("备注说明") },
                    placeholder = { Text("例如: 65W GaN快充头") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateNote(session.id, noteInput)
                        sessionToEditNote = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricEmerald)
                ) {
                    Text("保存", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToEditNote = null }) {
                    Text("取消")
                }
            }
        )
    }

    // Clear All Confirmation Dialog
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("清空全部记录？") },
            text = { Text("此操作将永久删除全部历史充电时长、功率与电量记录，无法恢复。") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAll()
                        showClearConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("确认清空", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun SessionCard(
    session: ChargingSession,
    onDelete: () -> Unit,
    onEditNote: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("session_card_${session.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Date, Duration Chip, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(ElectricEmerald.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                session.plugType.contains("无线") -> Icons.Default.Wifi
                                session.plugType.contains("USB") -> Icons.Default.Cable
                                else -> Icons.Default.Bolt
                            },
                            contentDescription = null,
                            tint = ElectricEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = TimeUtils.formatDateGroup(session.startTimeMillis),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${TimeUtils.formatTimeOnly(session.startTimeMillis)} ~ ${TimeUtils.formatTimeOnly(session.endTimeMillis)}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Duration Highlight Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(ElectricEmerald.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = ElectricEmerald,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = TimeUtils.formatDurationChinese(session.durationMillis),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricEmerald
                        )
                    }
                }
            }

            // Target reached chip (if recorded)
            if (session.targetReachedDurationMillis != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF59E0B).copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "充至目标 ${session.targetBatteryLevel}% 耗时：${TimeUtils.formatDurationChinese(session.targetReachedDurationMillis)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFF59E0B)
                    )
                }
            }

            // Middle Stats Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Battery jump
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${session.startBatteryLevel}% ➔ ${session.endBatteryLevel}%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "(+${session.batteryGained}%)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricEmerald
                    )
                }

                // Power or Rate
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (session.avgPowerWatts > 0f) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = ElectricEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${session.avgPowerWatts}W 均功",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ElectricEmerald
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = TimeUtils.formatRate(session.ratePercentPerMinute),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Bottom Row: Plug details, Note & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = session.plugType,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (session.note.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "· ${session.note}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = ElectricCyan,
                            maxLines = 1
                        )
                    }
                }

                Row {
                    IconButton(
                        onClick = onEditNote,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "备注",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "删除",
                            tint = Color(0xFFEF4444).copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHistoryView(
    onSeedSampleData: () -> Unit,
    isFiltered: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.BatteryChargingFull,
                    contentDescription = null,
                    tint = ElectricEmerald,
                    modifier = Modifier.size(36.dp)
                )
            }

            Text(
                text = if (isFiltered) "该分类下暂无记录" else "暂无充电记录",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "手机接通电源后后台会自动开始计时，充到设定目标自动标记，拔出充电器后即可在此生成详细记录与导出报表。",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )

            if (!isFiltered) {
                Button(
                    onClick = onSeedSampleData,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricEmerald)
                ) {
                    Text("填充示例记录体验", color = Color.White)
                }
            }
        }
    }
}
