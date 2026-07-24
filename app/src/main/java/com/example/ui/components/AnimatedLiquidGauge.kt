package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.TextMuted
import kotlin.math.sin

@Composable
fun AnimatedLiquidGauge(
    percentage: Float,
    modifier: Modifier = Modifier,
    liquidColor: Color = ElectricCyan
) {
    val animatedPct by animateFloatAsState(
        targetValue = percentage.coerceIn(0f, 100f),
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        ),
        label = "gauge_pct"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "wave_phase"
    )

    Box(modifier = modifier.size(160.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val radius = w / 2f

            val circlePath = Path().apply {
                addOval(Rect(Offset.Zero, size))
            }

            drawCircle(color = liquidColor.copy(alpha = 0.08f), radius = radius)

            clipPath(circlePath, clipOp = ClipOp.Intersect) {
                val fillHeight = h * (1f - animatedPct / 100f)
                val wavePath = Path().apply {
                    moveTo(0f, fillHeight)
                    // OPTIMIZATION: step by 5 pixels to reduce path complexity and ensure 60fps on low-end devices
                    for (x in 0..w.toInt() step 5) {
                        val y = fillHeight + sin(x * 0.04f + wavePhase) * 6f
                        lineTo(x.toFloat(), y)
                    }
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(wavePath, color = liquidColor.copy(alpha = 0.35f))
            }

            drawCircle(
                color = liquidColor.copy(alpha = 0.4f),
                radius = radius - 2f,
                style = Stroke(width = 3f)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${animatedPct.toInt()}%",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = liquidColor
            )
            Text(
                text = "TANK LEVEL",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextMuted,
                letterSpacing = 1.sp
            )
        }
    }
}
