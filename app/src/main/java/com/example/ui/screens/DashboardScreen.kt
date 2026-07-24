package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.PowerOff
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.ConnectionState
import com.example.data.models.PumpMode
import com.example.data.models.SystemMetrics
import com.example.data.models.TelemetryData
import com.example.data.preferences.AppConfig
import com.example.ui.components.AnimatedLiquidGauge
import com.example.ui.components.DutyCycleVisualizer
import com.example.ui.components.GlassmorphicCard
import com.example.ui.components.HysteresisBar
import com.example.ui.components.StatusBadge
import com.example.ui.theme.CriticalRed
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberDarkBg
import com.example.ui.theme.CyberGlassCard
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningOrange

import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

@Composable
fun DashboardScreen(
    telemetry: TelemetryData,
    connectionState: ConnectionState,
    systemMetrics: SystemMetrics,
    appConfig: AppConfig,
    pumpMode: PumpMode,
    isPending: Boolean,
    pendingCommandName: String?,
    dutyCycleValveOpen: Boolean,
    dutyCycleSecondsLeft: Int,
    onDispatchCommand: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDisconnected = (connectionState == ConnectionState.DISCONNECTED)
    val haptic = LocalHapticFeedback.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CyberDarkBg)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Banner 1: Connection Lost Warning Banner
        item {
            AnimatedVisibility(
                visible = isDisconnected,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CriticalRed.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                        .border(1.dp, CriticalRed, RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Connection Warning",
                        tint = CriticalRed,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CONNECTION LOST: Reconnecting...",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = CriticalRed
                        )
                        Text(
                            text = "Controls paused until telemetry reconnects.",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // Banner 2: Emergency Water Rationing Warning Banner
        item {
            AnimatedVisibility(
                visible = telemetry.rationing && !telemetry.leak,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(WarningOrange.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
                        .border(1.dp, WarningOrange, RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = "Rationing Active",
                        tint = WarningOrange,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "RATIONING PROTOCOL ACTIVE",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = WarningOrange,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Water level below 15.0%. Valve cycling engaged.",
                            fontSize = 13.sp,
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        // Section 1: Visual Gauge & Central Status Card (Feature 1)
        item {
            GlassmorphicCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = when {
                    telemetry.leak -> CriticalRed
                    telemetry.rationing -> WarningOrange
                    else -> CyberBorder
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top System Status Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RESERVOIR TANK STATUS",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 0.5.sp
                        )

                        StatusBadge(
                            text = when {
                                telemetry.leak -> "• LEAK DETECTED"
                                telemetry.rationing -> "• RATIONING"
                                telemetry.pump -> "• AUTO-FILLING"
                                connectionState == ConnectionState.SIMULATED -> "• SIMULATED"
                                connectionState == ConnectionState.CONNECTED -> "• LIVE ESP32"
                                else -> "• RECONNECTING"
                            },
                            badgeColor = when {
                                telemetry.leak -> CriticalRed
                                telemetry.rationing -> WarningOrange
                                telemetry.pump -> ElectricCyan
                                connectionState == ConnectionState.SIMULATED -> NeonBlue
                                connectionState == ConnectionState.CONNECTED -> StatusGreen
                                else -> CriticalRed
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Animated Wave Liquid Gauge (Clean Readout inside circle)
                    AnimatedLiquidGauge(
                        waterLevelPct = telemetry.waterLevelPct,
                        distanceCm = telemetry.distanceCm,
                        volumeLiters = systemMetrics.currentVolumeLiters,
                        isLeak = telemetry.leak,
                        isRationing = telemetry.rationing
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Dedicated Raw Sensor Telemetry Cards (Plain-Language Dual-Coding)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SensorCardItem(
                            label = "Ultrasonic Sensor",
                            value = "%.1f cm".format(telemetry.distanceCm),
                            statusText = if (telemetry.waterLevelPct < 15.0) "Low Water Level" else "Normal Level",
                            icon = Icons.Default.Sensors,
                            tint = if (telemetry.waterLevelPct < 15.0) WarningOrange else ElectricCyan,
                            modifier = Modifier.weight(1f)
                        )

                        SensorCardItem(
                            label = "Flow Pulse Rate",
                            value = "${telemetry.pulses} Hz",
                            statusText = if (telemetry.leak) "Anomalous Flow" else "Normal Flow",
                            icon = Icons.Default.Speed,
                            tint = if (telemetry.leak) CriticalRed else NeonBlue,
                            modifier = Modifier.weight(1f)
                        )

                        SensorCardItem(
                            label = "Solenoid Valve",
                            value = if (telemetry.valve) "OPEN" else "SEALED",
                            statusText = if (telemetry.valve) "Operational" else "Lockdown",
                            icon = Icons.Default.WaterDrop,
                            tint = if (telemetry.valve) StatusGreen else CriticalRed,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Section 2: Bi-Directional Auto-Fill Engine Panel (Feature 2)
        item {
            GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ElectricalServices,
                                contentDescription = "Pump Engine",
                                tint = ElectricCyan,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "PUMP ENGINE CONTROL",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                letterSpacing = 0.5.sp
                            )
                        }

                        StatusBadge(
                            text = if (telemetry.pump) "• OPERATIONAL (ON)" else "• PUMP IDLE (OFF)",
                            badgeColor = if (telemetry.pump) ElectricCyan else TextMuted,
                            isPulsing = telemetry.pump
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Hysteresis Visualizer Range Bar
                    HysteresisBar(
                        currentPct = telemetry.waterLevelPct,
                        startPct = appConfig.hysteresisLowPct,
                        stopPct = appConfig.hysteresisHighPct,
                        isPumpActive = telemetry.pump
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "MANUAL CONTROL OVERRIDE",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Mode Switcher Controls [ AUTO | MANUAL ON | MANUAL OFF ]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // AUTO BUTTON
                        ControlButton(
                            text = "AUTO",
                            icon = Icons.Default.AutoMode,
                            isSelected = pumpMode == PumpMode.AUTO,
                            isPending = isPending && pendingCommandName == "AUTO",
                            disabled = isDisconnected,
                            activeColor = ElectricCyan,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onDispatchCommand("AUTO")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("auto_mode_button")
                        )

                        // MANUAL ON BUTTON
                        ControlButton(
                            text = "PUMP ON",
                            icon = Icons.Default.Power,
                            isSelected = pumpMode == PumpMode.MANUAL_ON,
                            isPending = isPending && pendingCommandName == "PUMP_ON",
                            disabled = isDisconnected,
                            activeColor = StatusGreen,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onDispatchCommand("PUMP_ON")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("manual_on_button")
                        )

                        // MANUAL OFF BUTTON
                        ControlButton(
                            text = "PUMP OFF",
                            icon = Icons.Default.PowerOff,
                            isSelected = pumpMode == PumpMode.MANUAL_OFF,
                            isPending = isPending && pendingCommandName == "PUMP_OFF",
                            disabled = isDisconnected,
                            activeColor = CriticalRed,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onDispatchCommand("PUMP_OFF")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("manual_off_button")
                        )
                    }
                }
            }
        }

        // Section 3: Emergency Water Rationing Monitor (Feature 3)
        if (telemetry.rationing || telemetry.waterLevelPct < 15.0) {
            item {
                DutyCycleVisualizer(
                    isValveOpen = dutyCycleValveOpen,
                    cycleRemainingSeconds = dutyCycleSecondsLeft
                )
            }
        }

        // Section 4: System Telemetry & Signal Health
        item {
            GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Router, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Wi-Fi Signal: ${systemMetrics.wifiSignalDbm} dBm (Good)", fontSize = 13.sp, color = TextSecondary)
                    }

                    Box(
                        modifier = Modifier
                            .height(20.dp)
                            .width(1.dp)
                            .background(CyberBorder)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Refresh: 1000ms Live Payload", fontSize = 13.sp, color = TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun ControlButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    isPending: Boolean,
    disabled: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = !disabled && !isPending,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) activeColor else CyberGlassCard,
            contentColor = if (isSelected) Color.Black else TextPrimary,
            disabledContainerColor = CyberGlassCard.copy(alpha = 0.4f),
            disabledContentColor = TextMuted
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.height(52.dp) // Ensure 48dp+ touch target
    ) {
        if (isPending) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = activeColor,
                strokeWidth = 2.5.dp
            )
        } else {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SensorCardItem(
    label: String,
    value: String,
    statusText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(CyberGlassCard, RoundedCornerShape(14.dp))
            .border(1.dp, CyberBorder, RoundedCornerShape(14.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(tint.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(text = statusText, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = tint)
        Text(text = label, fontSize = 11.sp, color = TextMuted)
    }
}
