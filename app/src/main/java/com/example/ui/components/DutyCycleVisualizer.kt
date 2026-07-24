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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberGlassCard
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.WarningOrange

@Composable
fun DutyCycleVisualizer(
    isValveOpen: Boolean,
    cycleRemainingSeconds: Int,
    totalCycleSeconds: Int = 15, // 5s open + 10s closed
    modifier: Modifier = Modifier
) {
    val stateColor = if (isValveOpen) ElectricCyan else WarningOrange
    val progress = (cycleRemainingSeconds.toFloat() / totalCycleSeconds.toFloat()).coerceIn(0f, 1f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(WarningOrange.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .border(1.dp, WarningOrange.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isValveOpen) Icons.Default.WaterDrop else Icons.Default.LockClock,
                    contentDescription = null,
                    tint = stateColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "RATIONING DUTY CYCLE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = WarningOrange,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = if (isValveOpen) "VALVE STATE: 5s OPEN (ALLOWING FLOW)" else "VALVE STATE: 10s CLOSED (LOCKDOWN)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }
            }

            Text(
                text = "${cycleRemainingSeconds}s",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = stateColor
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = stateColor,
            trackColor = CyberGlassCard,
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "5s OPEN RELEASE",
                fontSize = 12.sp,
                color = ElectricCyan,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "10s CLOSED RATION",
                fontSize = 12.sp,
                color = WarningOrange,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
