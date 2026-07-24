package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.TelemetrySnapshot
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

@Composable
fun ConsumptionChart(
    snapshots: List<TelemetrySnapshot>,
    modifier: Modifier = Modifier
) {
    if (snapshots.isEmpty()) {
        Box(modifier = modifier.height(180.dp).fillMaxWidth()) {
            Text(text = "No data yet", color = TextMuted, fontSize = 14.sp, modifier = Modifier.padding(16.dp))
        }
        return
    }

    val data = snapshots.reversed().takeLast(20)
    val maxLevel = data.maxOf { it.waterLevelPct }.coerceAtLeast(1f)
    val maxFlow = data.maxOf { it.flowRateLpm }.coerceAtLeast(0.1f)

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Canvas(modifier = Modifier.size(10.dp).padding(top = 2.dp)) { drawCircle(ElectricCyan, radius = 5f) }
                Text(text = "Water Level (%)", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Canvas(modifier = Modifier.size(10.dp).padding(top = 2.dp)) { drawCircle(NeonGreen, radius = 5f) }
                Text(text = "Flow Rate (L/min)", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
            }
        }

        Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
            val w = size.width
            val h = size.height
            val stepX = if (data.size > 1) w / (data.size - 1) else w

            for (i in 0..4) {
                val y = h * i / 4f
                drawLine(CyberBorder, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
            }

            if (data.size >= 2) {
                val levelPath = Path().apply {
                    data.forEachIndexed { i, snap ->
                        val x = i * stepX
                        val y = h - (snap.waterLevelPct / maxLevel) * h
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                }
                drawPath(levelPath, ElectricCyan, style = Stroke(width = 2.5f))
            }

            val barWidth = (stepX * 0.3f).coerceAtMost(12f)
            data.forEachIndexed { i, snap ->
                val x = i * stepX - barWidth / 2
                val barH = (snap.flowRateLpm / maxFlow) * h * 0.8f
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(NeonGreen.copy(alpha = 0.7f), NeonGreen.copy(alpha = 0.2f))),
                    topLeft = Offset(x, h - barH),
                    size = Size(barWidth, barH),
                    cornerRadius = CornerRadius(3f)
                )
            }
        }
    }
}
