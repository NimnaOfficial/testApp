package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

@Composable
fun HysteresisBar(
    currentLevel: Float,
    lowThreshold: Float = 20f,
    highThreshold: Float = 85f,
    modifier: Modifier = Modifier
) {
    val animLevel by animateFloatAsState(
        targetValue = currentLevel.coerceIn(0f, 100f),
        animationSpec = tween(800),
        label = "hyst_level"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("LOW ${lowThreshold.toInt()}%", fontSize = 10.sp, color = AccentBlue, fontWeight = FontWeight.Bold)
            Text("Current: ${animLevel.toInt()}%", fontSize = 10.sp, color = TextSecondary)
            Text("HIGH ${highThreshold.toInt()}%", fontSize = 10.sp, color = NeonGreen, fontWeight = FontWeight.Bold)
        }

        Canvas(modifier = Modifier.fillMaxWidth().height(20.dp)) {
            val w = size.width
            val h = size.height

            drawRoundRect(color = CyberBorder, size = Size(w, h), cornerRadius = CornerRadius(10f))

            val lowX = w * lowThreshold / 100f
            drawRoundRect(color = AccentBlue.copy(alpha = 0.2f), size = Size(lowX, h), cornerRadius = CornerRadius(10f))

            val highX = w * highThreshold / 100f
            drawRoundRect(color = NeonGreen.copy(alpha = 0.2f), topLeft = Offset(highX, 0f), size = Size(w - highX, h), cornerRadius = CornerRadius(10f))

            val indicatorX = w * animLevel / 100f
            val indicatorColor = when {
                animLevel < lowThreshold -> AccentBlue
                animLevel > highThreshold -> NeonGreen
                else -> ElectricCyan
            }
            drawCircle(color = indicatorColor, radius = h / 2f + 2f, center = Offset(indicatorX.coerceIn(h / 2f, w - h / 2f), h / 2f))
        }

        val statusText = when {
            animLevel < lowThreshold -> "Below low threshold - pump should be ON"
            animLevel > highThreshold -> "Above high threshold - pump should be OFF"
            else -> "Within hysteresis band"
        }
        Text(text = statusText, fontSize = 11.sp, color = TextMuted, modifier = Modifier.padding(top = 4.dp))
    }
}
