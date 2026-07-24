package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ActivityLog
import com.example.data.local.AquaDatabase
import com.example.data.local.TelemetrySnapshot
import com.example.data.models.ConnectionState
import com.example.data.models.PumpMode
import com.example.data.models.SystemMetrics
import com.example.data.models.TelemetryData
import com.example.data.preferences.AppConfig
import com.example.data.preferences.AppPreferencesRepository
import com.example.network.AquaWebSocketClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AquaIntelViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AquaDatabase.getDatabase(application)
    private val dao = db.aquaDao()
    private val prefsRepo = AppPreferencesRepository(application)
    private val wsClient = AquaWebSocketClient()

    // Config & Preferences
    val appConfig: StateFlow<AppConfig> = prefsRepo.appConfigFlow
        .stateInScope(AppConfig())

    // UI Telemetry State
    private val _telemetry = MutableStateFlow(TelemetryData())
    val telemetry: StateFlow<TelemetryData> = _telemetry.asStateFlow()

    private val _connectionState = MutableStateFlow(ConnectionState.SIMULATED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _systemMetrics = MutableStateFlow(SystemMetrics())
    val systemMetrics: StateFlow<SystemMetrics> = _systemMetrics.asStateFlow()

    // Pump Mode
    private val _pumpMode = MutableStateFlow(PumpMode.AUTO)
    val pumpMode: StateFlow<PumpMode> = _pumpMode.asStateFlow()

    // Button Lock / Loading States for Debouncing
    private val _isCommandPending = MutableStateFlow(false)
    val isCommandPending: StateFlow<Boolean> = _isCommandPending.asStateFlow()

    private val _pendingCommandName = MutableStateFlow<String?>(null)
    val pendingCommandName: StateFlow<String?> = _pendingCommandName.asStateFlow()

    // UI Notifications & Messages
    private val _toastEvents = MutableSharedFlow<String>()
    val toastEvents: SharedFlow<String> = _toastEvents.asSharedFlow()

    // Rationing Duty Cycle State
    private val _dutyCycleValveOpen = MutableStateFlow(true)
    val dutyCycleValveOpen: StateFlow<Boolean> = _dutyCycleValveOpen.asStateFlow()

    private val _dutyCycleSecondsLeft = MutableStateFlow(5)
    val dutyCycleSecondsLeft: StateFlow<Int> = _dutyCycleSecondsLeft.asStateFlow()

    // Historical Snapshots & Logs from Room
    val recentSnapshots = dao.getRecentSnapshots()
    val activityLogs = dao.getAllActivityLogs()

    // Internal Jobs
    private var simulationJob: Job? = null
    private var dutyCycleJob: Job? = null
    private var snapshotTimerJob: Job? = null
    private var optimisticTimeoutJob: Job? = null

    init {
        observeWebSocket()
        observeConfig()
        startSnapshotRecorder()
        startRationingDutyCycle()
    }

    private fun observeConfig() {
        viewModelScope.launch {
            appConfig.collectLatest { config ->
                if (config.useSimulationMode) {
                    wsClient.disconnect()
                    _connectionState.value = ConnectionState.SIMULATED
                    startSimulationEngine()
                } else {
                    stopSimulationEngine()
                    wsClient.connect(config.serverIp)
                }
            }
        }
    }

    private fun observeWebSocket() {
        viewModelScope.launch {
            wsClient.connectionState.collectLatest { state ->
                if (!appConfig.value.useSimulationMode) {
                    _connectionState.value = state
                }
            }
        }

        viewModelScope.launch {
            wsClient.telemetryData.collectLatest { data ->
                if (!appConfig.value.useSimulationMode) {
                    handleIncomingTelemetry(data)
                }
            }
        }
    }

    private fun handleIncomingTelemetry(incoming: TelemetryData) {
        val previous = _telemetry.value

        // Check command confirmation for optimistic state reconciliation
        if (_isCommandPending.value) {
            val pendingCmd = _pendingCommandName.value
            var confirmed = false
            if (pendingCmd == "PUMP_ON" && incoming.pump) confirmed = true
            if (pendingCmd == "PUMP_OFF" && !incoming.pump) confirmed = true
            if (pendingCmd == "AUTO") confirmed = true
            if (pendingCmd == "RESET_LEAK" && !incoming.leak) confirmed = true

            if (confirmed) {
                _isCommandPending.value = false
                _pendingCommandName.value = null
                optimisticTimeoutJob?.cancel()
            }
        }

        // Log events if status changed
        if (incoming.pump != previous.pump) {
            logEvent(
                title = if (incoming.pump) "Submersible Pump Started" else "Submersible Pump Stopped",
                description = "Water level at %.1f%%".format(incoming.waterLevelPct),
                type = "PUMP"
            )
        }

        if (incoming.leak && !previous.leak) {
            logEvent(
                title = "🚨 CRITICAL WATER LEAK DETECTED",
                description = "YF-S201 sensor recorded anomalous pulse surge (${incoming.pulses} pulses)",
                type = "LEAK"
            )
        }

        if (incoming.rationing && !previous.rationing) {
            logEvent(
                title = "⚠️ RATIONING PROTOCOL ENGAGED",
                description = "Water level dropped below 15% threshold",
                type = "RATIONING"
            )
        }

        _telemetry.value = incoming
        _systemMetrics.value = _systemMetrics.value.copy(
            lastPayloadTime = System.currentTimeMillis(),
            currentVolumeLiters = (incoming.waterLevelPct / 100.0) * appConfig.value.tankCapacityLiters
        )
    }

    // Command Dispatching with Debouncing & Optimistic UI
    fun dispatchCommand(command: String) {
        if (_isCommandPending.value) {
            viewModelScope.launch { _toastEvents.emit("Please wait for current command to complete...") }
            return
        }

        _isCommandPending.value = true
        _pendingCommandName.value = command

        val currentData = _telemetry.value

        // Optimistic UI Update
        when (command) {
            "PUMP_ON" -> {
                _pumpMode.value = PumpMode.MANUAL_ON
                _telemetry.value = currentData.copy(pump = true)
                logEvent("Manual Override: Pump ON", "User manually turned pump ON", "OVERRIDE")
            }
            "PUMP_OFF" -> {
                _pumpMode.value = PumpMode.MANUAL_OFF
                _telemetry.value = currentData.copy(pump = false)
                logEvent("Manual Override: Pump OFF", "User manually turned pump OFF", "OVERRIDE")
            }
            "AUTO" -> {
                _pumpMode.value = PumpMode.AUTO
                logEvent("Hand Control to ESP32 Auto", "Auto-Fill Hysteresis active", "OVERRIDE")
            }
            "RESET_LEAK" -> {
                _telemetry.value = currentData.copy(leak = false, valve = true)
                logEvent("Leak Lockdown Cleared", "Valve operation restored by user", "LEAK")
            }
        }

        // Send via WebSocket if live
        if (!appConfig.value.useSimulationMode) {
            wsClient.sendCommand(command)
        }

        // 2-second Optimistic Timeout Revert
        optimisticTimeoutJob?.cancel()
        optimisticTimeoutJob = viewModelScope.launch {
            delay(2000)
            if (_isCommandPending.value) {
                // Command failed to get payload confirmation
                _isCommandPending.value = false
                _pendingCommandName.value = null
                _toastEvents.emit("Command timeout / failed. Reverting state.")
            }
        }
    }

    // Simulated Telemetry Engine (Hardware Demo Mode)
    private fun startSimulationEngine() {
        stopSimulationEngine()
        simulationJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val current = _telemetry.value
                val config = appConfig.value
                val isAuto = (_pumpMode.value == PumpMode.AUTO)

                var newPump = current.pump
                var newLevel = current.waterLevelPct
                var newPulses = current.pulses
                var newLeak = current.leak

                // Auto-Fill Hysteresis logic
                if (isAuto) {
                    if (newLevel <= config.hysteresisLowPct) {
                        newPump = true
                    } else if (newLevel >= config.hysteresisHighPct) {
                        newPump = false
                    }
                }

                // Level physics simulation
                if (newPump) {
                    newLevel += 2.5 // Fill rate
                    newPulses += 15
                } else {
                    newLevel -= 0.6 // Drain rate
                    if (newLevel > 0) newPulses += 2
                }

                newLevel = newLevel.coerceIn(0.0, 100.0)
                val newRationing = (newLevel < 15.0)
                val newDistance = ((100.0 - newLevel) / 100.0) * config.tankHeightCm

                val updated = TelemetryData(
                    distanceCm = newDistance,
                    waterLevelPct = newLevel,
                    pump = newPump,
                    valve = !newLeak,
                    rationing = newRationing,
                    leak = newLeak,
                    pulses = newPulses,
                    flowRateLpm = if (newPump) 8.2 else 1.5,
                    timestamp = System.currentTimeMillis()
                )

                handleIncomingTelemetry(updated)
            }
        }
    }

    private fun stopSimulationEngine() {
        simulationJob?.cancel()
        simulationJob = null
    }

    // Duty Cycle Timer for Rationing Mode (5s OPEN / 10s CLOSED)
    private fun startRationingDutyCycle() {
        dutyCycleJob?.cancel()
        dutyCycleJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val current = _telemetry.value
                if (current.rationing) {
                    var secs = _dutyCycleSecondsLeft.value - 1
                    var isOpen = _dutyCycleValveOpen.value

                    if (secs <= 0) {
                        isOpen = !isOpen
                        secs = if (isOpen) 5 else 10
                    }

                    _dutyCycleSecondsLeft.value = secs
                    _dutyCycleValveOpen.value = isOpen
                } else {
                    _dutyCycleValveOpen.value = true
                    _dutyCycleSecondsLeft.value = 5
                }
            }
        }
    }

    // Periodic Room Snapshot Recorder (Every 3s)
    private fun startSnapshotRecorder() {
        snapshotTimerJob?.cancel()
        snapshotTimerJob = viewModelScope.launch {
            while (true) {
                delay(3000)
                val t = _telemetry.value
                dao.insertSnapshot(
                    TelemetrySnapshot(
                        timestamp = t.timestamp,
                        waterLevelPct = t.waterLevelPct,
                        distanceCm = t.distanceCm,
                        pumpOn = t.pump,
                        valveOn = t.valve,
                        pulses = t.pulses,
                        flowRateLpm = t.flowRateLpm,
                        isLeak = t.leak,
                        isRationing = t.rationing
                    )
                )
                dao.pruneOldSnapshots()
            }
        }
    }

    // Simulation Trigger Helpers for UI Testing
    fun toggleSimulatedLeak(trigger: Boolean) {
        val current = _telemetry.value
        _telemetry.value = current.copy(leak = trigger)
        if (trigger) {
            logEvent("🚨 SIMULATED LEAK TRIGGERED", "Manual testing leak flag enabled", "LEAK")
        }
    }

    fun setSimulatedWaterLevel(pct: Double) {
        val current = _telemetry.value
        _telemetry.value = current.copy(waterLevelPct = pct.coerceIn(0.0, 100.0))
    }

    fun updateServerIp(ip: String) {
        viewModelScope.launch {
            prefsRepo.updateServerIp(ip)
            _toastEvents.emit("Server IP updated to: $ip")
        }
    }

    fun toggleSimulationMode(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateSimulationMode(enabled)
            _toastEvents.emit(if (enabled) "Switched to Simulated Telemetry" else "Connecting to Hardware...")
        }
    }

    private fun logEvent(title: String, description: String, type: String) {
        viewModelScope.launch {
            dao.insertActivityLog(
                ActivityLog(
                    timestamp = System.currentTimeMillis(),
                    title = title,
                    description = description,
                    logType = type
                )
            )
        }
    }

    private fun <T> kotlinx.coroutines.flow.Flow<T>.stateInScope(initialValue: T): StateFlow<T> {
        val stateFlow = MutableStateFlow(initialValue)
        viewModelScope.launch {
            this@stateInScope.collect { stateFlow.value = it }
        }
        return stateFlow.asStateFlow()
    }
}
