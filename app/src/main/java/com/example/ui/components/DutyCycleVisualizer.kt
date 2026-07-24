package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@Composable
fun DutyCycleVisualizer(
    valveOpen: Boolean,
    secondsLeft: Int,
    modifier: Modifier = Modifier
) {
    val color = if (valveOpen) NeonGreen else ErrorRed

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing)),
        label = "pulse_alpha"
    )

    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(16.dp),
                shape = CircleShape,
                color = color.copy(alpha = if (valveOpen) pulseAlpha else 0.5f)
            ) {}

            Column {
                Text(
                    text = if (valveOpen) "VALVE OPEN" else "VALVE CLOSED",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(text = "Duty Cycle Active", fontSize = 11.sp, color = TextMuted)
            }
        }

        Text(text = "${secondsLeft}s", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}
