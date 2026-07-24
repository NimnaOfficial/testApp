package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.AppConfig
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCardBg
import com.example.ui.theme.CyberDarkBg
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

@Composable
fun SettingsModal(
    isOpen: Boolean,
    config: AppConfig,
    isSimulatedLeak: Boolean,
    onDismiss: () -> Unit,
    onSaveServerIp: (String) -> Unit,
    onToggleSimulation: (Boolean) -> Unit,
    onToggleSimulatedLeak: (Boolean) -> Unit,
    onSimulateWaterLevel: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isOpen,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 }
    ) {
        Box(
            modifier = modifier.fillMaxSize().background(CyberDarkBg.copy(alpha = 0.95f)).testTag("settings_modal")
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "SETTINGS", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = ElectricCyan, letterSpacing = 1.sp)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close", tint = TextSecondary) }
                }

                Spacer(Modifier.height(24.dp))

                SettingsSection(title = "SERVER CONNECTION", icon = Icons.Default.Wifi) {
                    var ipText by remember(config.serverIp) { mutableStateOf(config.serverIp) }
                    OutlinedTextField(
                        value = ipText,
                        onValueChange = { ipText = it },
                        label = { Text("Server IP Address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_server_ip"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = CyberBorder,
                            focusedLabelColor = ElectricCyan,
                            unfocusedLabelColor = TextMuted,
                            cursorColor = ElectricCyan,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { onSaveServerIp(ipText) },
                        modifier = Modifier.fillMaxWidth().testTag("btn_save_ip"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan.copy(alpha = 0.15f), contentColor = ElectricCyan)
                    ) {
                        Icon(Icons.Default.Save, "Save", modifier = Modifier.padding(end = 8.dp))
                        Text("Save IP", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(20.dp))

                SettingsSection(title = "SIMULATION MODE", icon = Icons.Default.Science) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Enable Simulation", fontSize = 15.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            Text("Use mock data without hardware", fontSize = 12.sp, color = TextMuted)
                        }
                        Switch(
                            checked = config.simulationMode,
                            onCheckedChange = onToggleSimulation,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NeonGreen,
                                checkedTrackColor = NeonGreen.copy(alpha = 0.3f),
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = CyberBorder
                            ),
                            modifier = Modifier.testTag("switch_simulation")
                        )
                    }

                    if (config.simulationMode) {
                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider(color = CyberBorder)
                        Spacer(Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Simulate Leak", fontSize = 15.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                Text("Trigger emergency leak overlay", fontSize = 12.sp, color = TextMuted)
                            }
                            Switch(
                                checked = isSimulatedLeak,
                                onCheckedChange = onToggleSimulatedLeak,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = ErrorRed,
                                    checkedTrackColor = ErrorRed.copy(alpha = 0.3f),
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = CyberBorder
                                ),
                                modifier = Modifier.testTag("switch_leak")
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        var sliderValue by remember { mutableFloatStateOf(50f) }
                        Column {
                            Text("Simulate Water Level: ${sliderValue.toInt()}%", fontSize = 15.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            Slider(
                                value = sliderValue,
                                onValueChange = { sliderValue = it },
                                onValueChangeFinished = { onSimulateWaterLevel(sliderValue) },
                                valueRange = 0f..100f,
                                modifier = Modifier.fillMaxWidth().testTag("slider_water_level"),
                                colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan, inactiveTrackColor = CyberBorder)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                SettingsSection(title = "ABOUT", icon = Icons.Default.Warning) {
                    Text(text = "AquaIntel v1.0", fontSize = 15.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    Text(text = "Smart Water Infrastructure Control Center", fontSize = 13.sp, color = TextMuted)
                    Text(text = "Enterprise-grade telemetry with real-time WebSocket connectivity.", fontSize = 12.sp, color = TextMuted, modifier = Modifier.padding(top = 4.dp))
                }

                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, icon: ImageVector, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().background(CyberCardBg, RoundedCornerShape(16.dp)).padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
            Icon(icon, contentDescription = title, tint = ElectricCyan, modifier = Modifier.padding(end = 8.dp))
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ElectricCyan, letterSpacing = 1.sp)
        }
        content()
    }
}
