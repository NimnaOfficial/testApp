package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.example.data.models.SystemMetrics
import com.example.data.models.TelemetryData
import com.example.data.preferences.AppConfig
import com.example.network.ConnectionState
import com.example.ui.theme.AquaIntelTheme
import com.example.ui.components.GlassmorphicCard
import com.example.ui.components.HysteresisBar
import com.example.ui.components.StatusBadge
import com.example.ui.components.WaterTankVisualizer
import com.example.ui.theme.CyberDarkBg
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.NavyBlue
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun DashboardScreenPreview() {
    AquaIntelTheme {
        DashboardScreen(
            telemetry = TelemetryData(waterLevelPct = 65f, flowRateLpm = 12.5f, totalLiters = 450f, pumpOn = false),
            connectionState = ConnectionState.CONNECTED,
            systemMetrics = SystemMetrics(),
            appConfig = AppConfig(),
            pumpMode = "AUTO",
            isPending = false,
            pendingCommandName = "",
            dutyCycleValveOpen = false,
            dutyCycleSecondsLeft = 0,
            onDispatchCommand = {}
        )
    }
}

@Composable
fun DashboardScreen(
    telemetry: TelemetryData,
    connectionState: ConnectionState,
    systemMetrics: SystemMetrics,
    appConfig: AppConfig,
    pumpMode: String,
    isPending: Boolean,
    pendingCommandName: String,
    dutyCycleValveOpen: Boolean,
    dutyCycleSecondsLeft: Int,
    onDispatchCommand: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberDarkBg)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
            .testTag("dashboard_screen"),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(Modifier.height(12.dp))

        // Header Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Project WaterSys",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Real-time Monitoring",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            }
            StatusBadge(connectionState = connectionState)
        }

        // Main Tank Visualizer Card
        GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "TANK STATUS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyBlue,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (telemetry.waterLevelPct > 80f) "Level High" else if (telemetry.waterLevelPct < 20f) "Level Critical" else "Level Normal",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${"%.1f".format(telemetry.totalLiters)} Liters Remaining",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }
                
                WaterTankVisualizer(
                    percentage = telemetry.waterLevelPct,
                    liquidColor = when {
                        telemetry.waterLevelPct < 20f -> ErrorRed
                        telemetry.waterLevelPct < 40f -> NavyBlue
                        else -> ElectricCyan
                    }
                )
            }
        }

        // Control Section
        Column {
            Text(
                text = "System Controls",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ControlButton(
                    text = if (pumpMode == "AUTO") "Switch to Manual" else "Switch to Auto",
                    icon = Icons.Default.Lock,
                    color = NavyBlue,
                    enabled = !isPending,
                    isPending = isPending && (pendingCommandName == "MODE_AUTO" || pendingCommandName == "MODE_MANUAL"),
                    onClick = { onDispatchCommand(if (pumpMode == "AUTO") "MODE_MANUAL" else "MODE_AUTO") },
                    modifier = Modifier.weight(1f)
                )
                ControlButton(
                    text = if (telemetry.pumpOn) "Stop Pump" else "Start Pump",
                    icon = Icons.Default.PowerSettingsNew,
                    color = if (telemetry.pumpOn) ErrorRed else SuccessGreen,
                    enabled = !isPending && pumpMode == "MANUAL",
                    isPending = isPending && (pendingCommandName == "PUMP_ON" || pendingCommandName == "PUMP_OFF"),
                    onClick = { onDispatchCommand(if (telemetry.pumpOn) "PUMP_OFF" else "PUMP_ON") },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Secondary Metrics Grid
        Column {
            Text(
                text = "Live Telemetry",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MetricTile(icon = Icons.Default.Speed, label = "Flow Rate", value = "${"%.1f".format(telemetry.flowRateLpm)} L/m", color = NeonGreen)
                    MetricTile(icon = Icons.Default.WaterDrop, label = "Efficiency", value = "94%", color = ElectricCyan)
                    MetricTile(icon = Icons.Default.Warning, label = "Pressure", value = "High", color = NavyBlue)
                }
            }
        }

        // Hysteresis Bar (Refined)
        GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(text = "AUTO-FILL RANGE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NavyBlue, letterSpacing = 1.sp)
                Spacer(Modifier.height(12.dp))
                HysteresisBar(currentLevel = telemetry.waterLevelPct)
            }
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun MetricTile(icon: ImageVector, label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(4.dp))
        Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(text = label, fontSize = 11.sp, color = TextMuted)
    }
}

@Composable
private fun ControlButton(text: String, icon: ImageVector, color: Color, enabled: Boolean, isPending: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color.copy(alpha = 0.15f),
            contentColor = color,
            disabledContainerColor = color.copy(alpha = 0.05f),
            disabledContentColor = color.copy(alpha = 0.3f)
        )
    ) {
        if (isPending) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = color)
        } else {
            Icon(icon, contentDescription = text, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(text = text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}
