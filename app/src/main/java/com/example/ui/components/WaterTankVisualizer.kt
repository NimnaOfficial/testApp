package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import kotlin.math.sin

@Composable
fun WaterTankVisualizer(
    percentage: Float,
    modifier: Modifier = Modifier,
    liquidColor: Color = ElectricCyan
) {
    val animatedPct by animateFloatAsState(
        targetValue = percentage.coerceIn(0f, 100f),
        animationSpec = tween(1500, easing = FastOutSlowInEasing),
        label = "tank_pct"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "wave_phase"
    )

    Box(modifier = modifier.size(140.dp, 200.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val tankWidth = w * 0.8f
            val tankHeight = h * 0.9f
            val left = (w - tankWidth) / 2
            val top = (h - tankHeight) / 2

            val tankRect = Rect(left, top, left + tankWidth, top + tankHeight)
            val tankPath = Path().apply {
                addRoundRect(RoundRect(tankRect, CornerRadius(16.dp.toPx())))
            }

            // Draw Tank Background/Glass
            drawPath(
                path = tankPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.05f), Color.White.copy(alpha = 0.1f)),
                    startY = top,
                    endY = top + tankHeight
                )
            )

            // Clip for water
            clipPath(tankPath) {
                val fillHeight = top + tankHeight * (1f - animatedPct / 100f)
                
                // Primary Wave
                val wavePath = Path().apply {
                    moveTo(left, fillHeight)
                    for (x in left.toInt()..(left + tankWidth).toInt()) {
                        val relativeX = x - left
                        val waveY = fillHeight + sin(relativeX * 0.05f + wavePhase) * 4f
                        lineTo(x.toFloat(), waveY)
                    }
                    lineTo(left + tankWidth, top + tankHeight)
                    lineTo(left, top + tankHeight)
                    close()
                }
                
                drawPath(
                    path = wavePath,
                    brush = Brush.verticalGradient(
                        colors = listOf(liquidColor.copy(alpha = 0.6f), liquidColor.copy(alpha = 0.8f)),
                        startY = fillHeight,
                        endY = top + tankHeight
                    )
                )

                // Secondary Wave (back)
                val backWavePath = Path().apply {
                    moveTo(left, fillHeight)
                    for (x in left.toInt()..(left + tankWidth).toInt()) {
                        val relativeX = x - left
                        val waveY = fillHeight + sin(relativeX * 0.04f - wavePhase + 1f) * 6f
                        lineTo(x.toFloat(), waveY)
                    }
                    lineTo(left + tankWidth, top + tankHeight)
                    lineTo(left, top + tankHeight)
                    close()
                }
                drawPath(path = backWavePath, color = liquidColor.copy(alpha = 0.2f))
            }

            // Tank Reflections/Highlights
            drawRoundRect(
                color = Color.White.copy(alpha = 0.1f),
                topLeft = Offset(left + 8.dp.toPx(), top + 8.dp.toPx()),
                size = Size(4.dp.toPx(), tankHeight - 16.dp.toPx()),
                cornerRadius = CornerRadius(2.dp.toPx())
            )

            // Tank Outline
            drawPath(
                path = tankPath,
                color = Color.White.copy(alpha = 0.15f),
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // Percentage Indicator
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${animatedPct.toInt()}%",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "CAPACITY",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 1.sp
            )
        }
    }
}
