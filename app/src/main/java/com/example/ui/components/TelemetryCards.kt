package com.example.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ElectricMeter
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricEmerald

@Composable
fun TelemetryCards(
    plugType: String,
    powerWatts: Float,
    currentMa: Int,
    voltageMv: Int,
    temperatureC: Float,
    health: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Row 1: Power & Current
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TelemetryCard(
                title = "实时功率",
                value = if (powerWatts > 0f) "${String.format("%.1f", powerWatts)} W" else "--",
                icon = Icons.Default.Bolt,
                iconTint = ElectricEmerald,
                modifier = Modifier.weight(1f)
            )
            TelemetryCard(
                title = "充入电流",
                value = if (currentMa > 0) "${currentMa} mA" else "--",
                icon = Icons.Default.Speed,
                iconTint = ElectricCyan,
                modifier = Modifier.weight(1f)
            )
        }

        // Row 2: Plug Type & Voltage
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TelemetryCard(
                title = "电源来源",
                value = if (plugType.isBlank() || plugType == "未接入") "未接入" else plugType,
                icon = Icons.Default.Power,
                iconTint = Color(0xFF38BDF8),
                modifier = Modifier.weight(1f)
            )
            TelemetryCard(
                title = "实时电压",
                value = if (voltageMv > 0) "${voltageMv} mV" else "--",
                icon = Icons.Default.ElectricMeter,
                iconTint = Color(0xFFF59E0B),
                modifier = Modifier.weight(1f)
            )
        }

        // Row 3: Temperature & Health
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TelemetryCard(
                title = "电池温度",
                value = if (temperatureC > 0) String.format("%.1f ℃", temperatureC) else "正常",
                icon = Icons.Default.Thermostat,
                iconTint = if (temperatureC > 38f) Color(0xFFEF4444) else ElectricEmerald,
                modifier = Modifier.weight(1f)
            )
            TelemetryCard(
                title = "健康度",
                value = health,
                icon = Icons.Default.Favorite,
                iconTint = Color(0xFFEC4899),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TelemetryCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(14.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconTint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1
            )
        }
    }
}
