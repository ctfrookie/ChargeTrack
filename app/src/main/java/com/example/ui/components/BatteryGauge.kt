package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricEmerald
import com.example.ui.theme.NeonGreen

@Composable
fun BatteryGauge(
    level: Int,
    isCharging: Boolean,
    powerWatts: Float = 0f,
    targetLevel: Int = 80,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "charging_animation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val strokeColor = when {
        isCharging -> ElectricEmerald
        level > 60 -> ElectricEmerald
        level > 20 -> ElectricCyan
        else -> Color(0xFFEF4444)
    }

    Box(
        modifier = modifier.size(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(190.dp)) {
            val strokeWidth = 14.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)

            // Background circle track
            drawArc(
                color = strokeColor.copy(alpha = 0.15f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Target level marker arc (e.g. 80% line)
            val targetAngle = -90f + (targetLevel.coerceIn(0, 100) / 100f) * 360f
            drawArc(
                color = Color(0xFFF59E0B).copy(alpha = 0.7f),
                startAngle = targetAngle - 1f,
                sweepAngle = 3f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth + 6f, cap = StrokeCap.Round)
            )

            // Progress arc
            val sweep = (level.coerceIn(0, 100) / 100f) * 360f
            val gradientBrush = Brush.sweepGradient(
                colors = listOf(
                    ElectricCyan,
                    ElectricEmerald,
                    NeonGreen,
                    ElectricCyan
                )
            )

            if (isCharging) {
                rotate(rotation) {
                    drawArc(
                        brush = gradientBrush,
                        startAngle = -90f,
                        sweepAngle = sweep.coerceAtLeast(10f),
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            } else {
                drawArc(
                    color = strokeColor,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        // Center Content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isCharging) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(ElectricEmerald.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "功率",
                        tint = ElectricEmerald,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = if (powerWatts > 0f) "${String.format("%.1f", powerWatts)}W" else "快充中",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricEmerald
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
            }

            Text(
                text = "$level%",
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = if (isCharging) "目标 $targetLevel%" else "当前电量",
                fontSize = 12.sp,
                color = if (isCharging && level >= targetLevel) ElectricEmerald else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
