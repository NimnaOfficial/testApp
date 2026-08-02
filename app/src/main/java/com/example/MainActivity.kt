package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Water
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.AnalyticsLogsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.EmergencyLeakModal
import com.example.ui.screens.SettingsModal
import com.example.ui.theme.AquaIntelTheme
import com.example.ui.theme.CyberCardBg
import com.example.ui.theme.CyberDarkBg
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.AquaIntelViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: AquaIntelViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AquaIntelTheme {
                val context = LocalContext.current
                val snackbarHostState = remember { SnackbarHostState() }

                val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
                val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
                val systemMetrics by viewModel.systemMetrics.collectAsStateWithLifecycle()
                val appConfig by viewModel.appConfig.collectAsStateWithLifecycle()
                val pumpMode by viewModel.pumpMode.collectAsStateWithLifecycle()
                val isPending by viewModel.isCommandPending.collectAsStateWithLifecycle()
                val pendingCommandName by viewModel.pendingCommandName.collectAsStateWithLifecycle()
                val dutyCycleValveOpen by viewModel.dutyCycleValveOpen.collectAsStateWithLifecycle()
                val dutyCycleSecondsLeft by viewModel.dutyCycleSecondsLeft.collectAsStateWithLifecycle()

                val snapshots by viewModel.recentSnapshots.collectAsStateWithLifecycle(initialValue = emptyList())
                val activityLogs by viewModel.activityLogs.collectAsStateWithLifecycle(initialValue = emptyList())

                var selectedTab by remember { mutableStateOf("DASHBOARD") }
                var isSettingsOpen by remember { mutableStateOf(false) }

                // Observe Toast Events
                LaunchedEffect(Unit) {
                    viewModel.toastEvents.collect { msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = CyberDarkBg,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        AquaTopBar(
                            onOpenSettings = { isSettingsOpen = true }
                        )
                    },
                    bottomBar = {
                        AquaBottomBar(
                            selectedTab = selectedTab,
                            onTabSelected = { selectedTab = it }
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (selectedTab) {
                            "DASHBOARD" -> {
                                DashboardScreen(
                                    telemetry = telemetry,
                                    connectionState = connectionState,
                                    systemMetrics = systemMetrics,
                                    appConfig = appConfig,
                                    pumpMode = pumpMode,
                                    isPending = isPending,
                                    pendingCommandName = pendingCommandName,
                                    dutyCycleValveOpen = dutyCycleValveOpen,
                                    dutyCycleSecondsLeft = dutyCycleSecondsLeft,
                                    onDispatchCommand = { cmd -> viewModel.dispatchCommand(cmd) }
                                )
                            }
                            "ANALYTICS" -> {
                                AnalyticsLogsScreen(
                                    snapshots = snapshots,
                                    activityLogs = activityLogs
                                )
                            }
                        }

                        // Full Screen Red Emergency Leak Overlay (Feature 4)
                        EmergencyLeakModal(
                            isVisible = telemetry.leak,
                            pulses = telemetry.pulses,
                            flowRateLpm = telemetry.flowRateLpm,
                            isPending = isPending && pendingCommandName == "RESET_LEAK",
                            onClearLeakCommand = { viewModel.dispatchCommand("RESET_LEAK") }
                        )

                        // Settings & Dynamic IP Configuration Modal
                        SettingsModal(
                            isOpen = isSettingsOpen,
                            config = appConfig,
                            isSimulatedLeak = telemetry.leak,
                            onDismiss = { isSettingsOpen = false },
                            onSaveServerIp = { ip -> viewModel.updateServerIp(ip) },
                            onToggleSimulation = { enabled -> viewModel.toggleSimulationMode(enabled) },
                            onToggleSimulatedLeak = { leak -> viewModel.toggleSimulatedLeak(leak) },
                            onSimulateWaterLevel = { pct -> viewModel.setSimulatedWaterLevel(pct) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AquaTopBar(
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .background(CyberDarkBg)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(AccentBlue.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Water,
                contentDescription = "AquaIntel Logo",
                tint = AccentBlue,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "CONNECTED",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = AccentBlue,
            )
            Text(
                text = "AQUAINTEL",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                letterSpacing = 1.sp
            )
        }

        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .size(48.dp)
                .testTag("open_settings_button")
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = AccentBlue,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun AquaBottomBar(
    selectedTab: String,
    onTabSelected: (String) -> Unit
) {
    NavigationBar(
        containerColor = CyberDarkBg,
        tonalElevation = 0.dp,
        modifier = Modifier
            .navigationBarsPadding()
            .fillMaxWidth()
    ) {
        NavigationBarItem(
            selected = selectedTab == "DASHBOARD",
            onClick = { onTabSelected("DASHBOARD") },
            icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard", modifier = Modifier.size(22.dp)) },
            label = { Text("Dashboard", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = AccentBlue,
                selectedTextColor = AccentBlue,
                indicatorColor = Color.Transparent,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
            ),
            modifier = Modifier.testTag("nav_dashboard")
        )

        NavigationBarItem(
            selected = selectedTab == "ANALYTICS",
            onClick = { onTabSelected("ANALYTICS") },
            icon = { Icon(Icons.Default.Analytics, contentDescription = "Analytics", modifier = Modifier.size(22.dp)) },
            label = { Text("Logs & Trends", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = AccentBlue,
                selectedTextColor = AccentBlue,
                indicatorColor = Color.Transparent,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
            ),
            modifier = Modifier.testTag("nav_analytics")
        )
    }
}
