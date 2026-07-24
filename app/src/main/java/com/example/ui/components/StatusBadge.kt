package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.network.ConnectionState
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.WarningAmber

@Composable
fun StatusBadge(
    connectionState: ConnectionState,
    modifier: Modifier = Modifier
) {
    val (color, label) = when (connectionState) {
        ConnectionState.CONNECTED -> NeonGreen to "CONNECTED"
        ConnectionState.CONNECTING -> WarningAmber to "CONNECTING"
        ConnectionState.DISCONNECTED -> ErrorRed to "DISCONNECTED"
        ConnectionState.ERROR -> ErrorRed to "ERROR"
    }

    val infiniteTransition = rememberInfiniteTransition(label = "badge_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing)),
        label = "badge_alpha"
    )

    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color.copy(alpha = alpha), CircleShape)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            letterSpacing = 0.5.sp
        )
    }
}
