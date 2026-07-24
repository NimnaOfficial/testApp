package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ActivityLog
import com.example.data.local.TelemetrySnapshot
import com.example.ui.components.ConsumptionChart
import com.example.ui.components.GlassmorphicCard
import com.example.ui.theme.CyberDarkBg
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AnalyticsLogsScreen(
    snapshots: List<TelemetrySnapshot>,
    activityLogs: List<ActivityLog>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CyberDarkBg)
            .padding(16.dp)
            .testTag("analytics_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(text = "CONSUMPTION TRENDS", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ElectricCyan, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 12.dp))
                    ConsumptionChart(snapshots = snapshots)
                }
            }
        }

        item {
            GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(text = "ACTIVITY LOG", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ElectricCyan, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 8.dp))
                    if (activityLogs.isEmpty()) {
                        Text(text = "No activity recorded yet", color = TextMuted, fontSize = 13.sp, modifier = Modifier.padding(vertical = 16.dp))
                    }
                }
            }
        }

        items(activityLogs) { log -> LogItem(log = log) }
    }
}

@Composable
private fun LogItem(log: ActivityLog) {
    val typeColor = when (log.type) {
        "COMMAND" -> ElectricCyan
        "WARNING" -> WarningAmber
        "ERROR" -> ErrorRed
        else -> NeonGreen
    }

    val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val timeStr = dateFormat.format(Date(log.timestamp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(typeColor.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = timeStr, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextMuted)
        Text(
            text = log.type,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = typeColor,
            modifier = Modifier.background(typeColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
        )
        Text(text = log.message, fontSize = 13.sp, color = TextPrimary, modifier = Modifier.weight(1f))
    }
}
