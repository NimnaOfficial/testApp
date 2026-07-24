package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CriticalRed
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCardBg
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningOrange
import kotlin.math.sin

@Composable
fun AnimatedLiquidGauge(
    waterLevelPct: Double,
    distanceCm: Double,
    volumeLiters: Double,
    isLeak: Boolean,
    isRationing: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 240.dp
) {
    val animatedLevel by animateFloatAsState(
        targetValue = waterLevelPct.coerceIn(0.0, 100.0).toFloat(),
        animationSpec = tween(durationMillis = 800, easing = LinearEasing),
        label = "waterLevelAnimation"
    )

    // Wave phase animation
    val infiniteTransition = rememberInfiniteTransition(label = "waveTransition")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing)
        ),
        label = "wavePhase"
    )

    // Liquid Colors based on system state
    val (primaryColor, secondaryColor) = when {
        isLeak -> Pair(CriticalRed, Color(0xFF990000))
        isRationing || animatedLevel < 15f -> Pair(WarningOrange, Color(0xFFCC6600))
        else -> Pair(ElectricCyan, NeonBlue)
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(CyberCardBg)
            .border(3.dp, primaryColor.copy(alpha = 0.6f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.toPx()
            val height = size.toPx()

            // Height of liquid in pixels
            val waterHeightPx = (animatedLevel / 100f) * height
            val baseWaterY = height - waterHeightPx

            val waveAmplitude = 12f

            // Front Wave Path
            val frontWavePath = Path().apply {
                moveTo(0f, height)
                lineTo(0f, baseWaterY)
                var x = 0f
                while (x <= width) {
                    val y = baseWaterY + sin((x / width * 2 * Math.PI) + wavePhase).toFloat() * waveAmplitude
                    lineTo(x, y)
                    x += 4f
                }
                lineTo(width, height)
                close()
            }

            // Back Wave Path (offset phase for 3D depth effect)
            val backWavePath = Path().apply {
                moveTo(0f, height)
                lineTo(0f, baseWaterY)
                var x = 0f
                while (x <= width) {
                    val y = baseWaterY + sin((x / width * 2 * Math.PI) + wavePhase + Math.PI / 2).toFloat() * (waveAmplitude * 0.7f)
                    lineTo(x, y)
                    x += 4f
                }
                lineTo(width, height)
                close()
            }

            // Draw Back Wave
            drawPath(
                path = backWavePath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        secondaryColor.copy(alpha = 0.45f),
                        secondaryColor.copy(alpha = 0.25f)
                    )
                )
            )

            // Draw Front Wave
            drawPath(
                path = frontWavePath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.85f),
                        secondaryColor.copy(alpha = 0.95f)
                    )
                )
            )
        }

        // Overlay Telemetry Digital Readout (Clean Readout: 78.5% and Volume in Pure White)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "%.1f%%".format(animatedLevel),
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "%.0f L".format(volumeLiters),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = ElectricCyan
            )

            Text(
                text = "RESERVOIR CAPACITY",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
                letterSpacing = 0.5.sp
            )
        }
    }
}
