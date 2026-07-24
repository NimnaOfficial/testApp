package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberGlassCard
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningOrange

@Composable
fun HysteresisBar(
    currentPct: Double,
    startPct: Double = 50.0,
    stopPct: Double = 95.0,
    isPumpActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "HYSTERESIS AUTO-FILL THRESHOLDS",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 0.5.sp
            )

            Text(
                text = if (isPumpActive) "PUMP TRIGGERED (<${startPct.toInt()}%)" else "MONITORING",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isPumpActive) ElectricCyan else TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Visual Range Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(CyberGlassCard)
                .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
        ) {
            val startFraction = (startPct / 100.0).toFloat().coerceIn(0f, 1f)
            val stopFraction = (stopPct / 100.0).toFloat().coerceIn(0f, 1f)
            val currentFraction = (currentPct / 100.0).toFloat().coerceIn(0f, 1f)

            // Auto-Fill Active Region Highlight
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = startFraction)
                    .background(WarningOrange.copy(alpha = 0.25f))
            )

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = stopFraction)
                    .background(ElectricCyan.copy(alpha = 0.15f))
            )

            // Current level marker line
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = currentFraction)
                    .background(if (isPumpActive) ElectricCyan else StatusGreen)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "0%",
                fontSize = 12.sp,
                color = TextMuted
            )
            Text(
                text = "START: ${startPct.toInt()}%",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = WarningOrange
            )
            Text(
                text = "STOP: ${stopPct.toInt()}%",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = StatusGreen
            )
            Text(
                text = "100%",
                fontSize = 12.sp,
                color = TextMuted
            )
        }
    }
}
