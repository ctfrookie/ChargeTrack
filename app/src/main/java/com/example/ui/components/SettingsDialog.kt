package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Webhook
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SettingsState
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricEmerald
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    settings: SettingsState,
    onDismiss: () -> Unit,
    onUpdateTargetLevel: (Int) -> Unit,
    onUpdateStopTimerAtTarget: (Boolean) -> Unit,
    onUpdateWebhookUrl: (String) -> Unit,
    onUpdateWebhookEnabled: (Boolean) -> Unit,
    onUpdateNotifyOnStart: (Boolean) -> Unit,
    onUpdateNotifyOnTarget: (Boolean) -> Unit,
    onUpdateNotifyOnFinish: (Boolean) -> Unit,
    onUpdateVibrateOnTarget: (Boolean) -> Unit,
    onTestWebhook: (String, (Boolean, String) -> Unit) -> Unit,
    onOpenVivoKeepAliveGuide: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    var webhookUrlInput by remember { mutableStateOf(settings.webhookUrl) }
    var targetSlider by remember { mutableFloatStateOf(settings.targetBatteryLevel.toFloat()) }
    var testResultMsg by remember { mutableStateOf<String?>(null) }
    var isTestingWebhook by remember { mutableStateOf(false) }

    val presetTargets = listOf(80, 85, 90, 100)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
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
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "充电与通知设置",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "目标电量停止计时 · Webhook推送 · 自动化",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "关闭")
                }
            }

            // Section 1: Target Battery & Timer Stop
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = ElectricEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "目标充电电量",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Text(
                            text = "${targetSlider.roundToInt()}%",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricEmerald
                        )
                    }

                    // Preset chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presetTargets.forEach { preset ->
                            FilterChip(
                                selected = targetSlider.roundToInt() == preset,
                                onClick = {
                                    targetSlider = preset.toFloat()
                                    onUpdateTargetLevel(preset)
                                },
                                label = { Text("$preset%") }
                            )
                        }
                    }

                    Slider(
                        value = targetSlider,
                        onValueChange = {
                            targetSlider = it
                            onUpdateTargetLevel(it.roundToInt())
                        },
                        valueRange = 50f..100f,
                        steps = 9
                    )

                    // Toggle: Stop Timer at Target
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "充到指定电量停止计时",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "电量到达 ${targetSlider.roundToInt()}% 时自动锁定耗时，准确统计充到目标的时间",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.stopTimerAtTarget,
                            onCheckedChange = onUpdateStopTimerAtTarget,
                            colors = SwitchDefaults.colors(checkedThumbColor = ElectricEmerald)
                        )
                    }

                    // Toggle: Vibrate on Target
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "达成目标震动提醒",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "充至指定电量时震动提示用户拔掉充电器",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.vibrateOnTarget,
                            onCheckedChange = onUpdateVibrateOnTarget,
                            colors = SwitchDefaults.colors(checkedThumbColor = ElectricEmerald)
                        )
                    }
                }
            }

            // Section 2: Webhook Notifications
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Webhook,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Webhook 自动化推送",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Switch(
                            checked = settings.webhookEnabled,
                            onCheckedChange = onUpdateWebhookEnabled,
                            colors = SwitchDefaults.colors(checkedThumbColor = ElectricCyan)
                        )
                    }

                    AnimatedVisibility(visible = settings.webhookEnabled) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "支持钉钉、企业微信机器人、飞书机器人、Bark、Server酱或任意接收 POST JSON 的自建服务端。",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )

                            OutlinedTextField(
                                value = webhookUrlInput,
                                onValueChange = {
                                    webhookUrlInput = it
                                    onUpdateWebhookUrl(it)
                                },
                                label = { Text("Webhook 完整 URL") },
                                placeholder = { Text("https://oapi.dingtalk.com/robot/send?access_token=...") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            // Event toggles
                            Text(
                                text = "推送事件选择",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("⚡ 开始充电时推送", fontSize = 13.sp)
                                Switch(
                                    checked = settings.notifyOnStart,
                                    onCheckedChange = onUpdateNotifyOnStart
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🎯 达到目标电量时推送", fontSize = 13.sp)
                                Switch(
                                    checked = settings.notifyOnTarget,
                                    onCheckedChange = onUpdateNotifyOnTarget
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🔌 拔掉断开充电时推送", fontSize = 13.sp)
                                Switch(
                                    checked = settings.notifyOnFinish,
                                    onCheckedChange = onUpdateNotifyOnFinish
                                )
                            }

                            // Test Button
                            OutlinedButton(
                                onClick = {
                                    isTestingWebhook = true
                                    testResultMsg = null
                                    onTestWebhook(webhookUrlInput) { success, msg ->
                                        isTestingWebhook = false
                                        testResultMsg = msg
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isTestingWebhook && webhookUrlInput.isNotBlank()
                            ) {
                                if (isTestingWebhook) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("正在测试发送...")
                                } else {
                                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("发送一条测试通知")
                                }
                            }

                            testResultMsg?.let { msg ->
                                Text(
                                    text = msg,
                                    fontSize = 12.sp,
                                    color = if (msg.contains("成功")) ElectricEmerald else Color(0xFFEF4444)
                                )
                            }
                        }
                    }
                }
            }

            // Section 3: Background auto-run tips
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "⚡ 后台自启与无感运行说明",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "应用已注册系统级开机自启（BOOT_COMPLETED）与电源连接监听器，插上充电器会直接拉起前台保活服务自动计时与功率监测，无需手动打开应用；拔掉自动保存并退出服务，轻量省电。",
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = onOpenVivoKeepAliveGuide,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("vivo / 国产机后台自启与防杀配置指南")
                    }
                }
            }
        }
    }
}
