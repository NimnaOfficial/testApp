package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ActivityLog
import com.example.data.local.AquaDatabase
import com.example.data.local.TelemetrySnapshot
import com.example.data.models.SystemMetrics
import com.example.data.models.TelemetryData
import com.example.data.preferences.AppConfig
import com.example.data.preferences.AppPreferencesRepository
import com.example.network.AquaWebSocketClient
import com.example.network.ConnectionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject

class AquaIntelViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "AquaIntelVM"
    private val prefsRepo = AppPreferencesRepository(application)
    private val db = AquaDatabase.getDatabase(application)
    private val dao = db.aquaDao()
    private val wsClient = AquaWebSocketClient()

    // --- State Flows ---
    private val _telemetry = MutableStateFlow(TelemetryData())
    val telemetry: StateFlow<TelemetryData> = _telemetry.asStateFlow()

    val connectionState: StateFlow<ConnectionState> = wsClient.connectionState

    private val _systemMetrics = MutableStateFlow(SystemMetrics())
    val systemMetrics: StateFlow<SystemMetrics> = _systemMetrics.asStateFlow()

    private val _appConfig = MutableStateFlow(AppConfig())
    val appConfig: StateFlow<AppConfig> = _appConfig.asStateFlow()

    private val _pumpMode = MutableStateFlow("AUTO")
    val pumpMode: StateFlow<String> = _pumpMode.asStateFlow()

    private val _isCommandPending = MutableStateFlow(false)
    val isCommandPending: StateFlow<Boolean> = _isCommandPending.asStateFlow()

    private val _pendingCommandName = MutableStateFlow("")
    val pendingCommandName: StateFlow<String> = _pendingCommandName.asStateFlow()

    private val _dutyCycleValveOpen = MutableStateFlow(false)
    val dutyCycleValveOpen: StateFlow<Boolean> = _dutyCycleValveOpen.asStateFlow()

    private val _dutyCycleSecondsLeft = MutableStateFlow(0)
    val dutyCycleSecondsLeft: StateFlow<Int> = _dutyCycleSecondsLeft.asStateFlow()

    private val _toastEvents = MutableSharedFlow<String>()
    val toastEvents: SharedFlow<String> = _toastEvents.asSharedFlow()

    // Room DB flows
    val recentSnapshots = dao.getRecentSnapshots()
    val activityLogs = dao.getRecentLogs()

    // Simulation state
    private var simulationJob: Job? = null

    init {
        // Observe preferences
        viewModelScope.launch {
            prefsRepo.appConfig.collect { config ->
                _appConfig.value = config
                if (config.simulationMode) {
                    startSimulation()
                } else {
                    stopSimulation()
                    if (config.serverIp.isNotBlank()) {
                        wsClient.connect(config.serverIp)
                    } else {
                        Log.e(TAG, "Server IP is blank, cannot connect")
                    }
                }
            }
        }

        // Observe WebSocket messages
        viewModelScope.launch {
            wsClient.messages.collect { msg ->
                parseTelemetry(msg)
            }
        }
    }

    private fun parseTelemetry(json: String) {
        try {
            val obj = JSONObject(json)
            val type = obj.optString("type", "telemetry")

            when (type) {
                "telemetry" -> {
                    val newData = TelemetryData(
                        waterLevelPct = obj.optDouble("waterLevelPct", 0.0).toFloat(),
                        flowRateLpm = obj.optDouble("flowRateLpm", 0.0).toFloat(),
                        totalLiters = obj.optDouble("totalLiters", 0.0).toFloat(),
                        pulses = obj.optInt("pulses", 0),
                        leak = obj.optBoolean("leak", false),
                        pumpOn = obj.optBoolean("pumpOn", false),
                        autoMode = obj.optBoolean("autoMode", true)
                    )
                    _telemetry.value = newData
                    _pumpMode.value = if (newData.autoMode) "AUTO" else "MANUAL"

                    // Persist snapshot safely
                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            dao.insertSnapshot(
                                TelemetrySnapshot(
                                    waterLevelPct = newData.waterLevelPct,
                                    flowRateLpm = newData.flowRateLpm,
                                    totalLiters = newData.totalLiters,
                                    pulses = newData.pulses,
                                    leak = newData.leak,
                                    pumpOn = newData.pumpOn
                                )
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "DB Insert Error: ${e.message}")
                        }
                    }
                }
                "metrics" -> {
                    _systemMetrics.value = SystemMetrics(
                        latencyMs = obj.optLong("latencyMs", 0),
                        serverUptime = obj.optString("serverUptime", "--"),
                        connectedClients = obj.optInt("connectedClients", 0),
                        uptimeMs = obj.optLong("uptimeMs", 0)
                    )
                }
                "command_ack" -> {
                    _isCommandPending.value = false
                    val ackCmd = obj.optString("command", "")
                    viewModelScope.launch {
                        _toastEvents.emit("$ackCmd acknowledged")
                        dao.insertLog(ActivityLog(message = "Command ACK: $ackCmd", type = "COMMAND"))
                    }
                }
                "duty_cycle" -> {
                    _dutyCycleValveOpen.value = obj.optBoolean("valveOpen", false)
                    _dutyCycleSecondsLeft.value = obj.optInt("secondsLeft", 0)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Parse error: ${e.message}")
            // Optional: emit an error if data is severely corrupted
            // viewModelScope.launch { _toastEvents.emit("Data parse error") }
        }
    }

    fun dispatchCommand(command: String) {
        _isCommandPending.value = true
        _pendingCommandName.value = command

        if (_appConfig.value.simulationMode) {
            viewModelScope.launch {
                delay(500)
                handleSimulatedCommand(command)
                _isCommandPending.value = false
                _toastEvents.emit("$command (simulated)")
            }
        } else {
            wsClient.sendCommand(command)
            // Timeout fallback
            viewModelScope.launch {
                delay(8000)
                if (_isCommandPending.value && _pendingCommandName.value == command) {
                    _isCommandPending.value = false
                    _toastEvents.emit("$command timed out")
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                dao.insertLog(ActivityLog(message = "Sent command: $command", type = "COMMAND"))
            } catch (e: Exception) {
                Log.e(TAG, "Log insert error: ${e.message}")
            }
        }
    }

    private fun handleSimulatedCommand(command: String) {
        val current = _telemetry.value
        when (command) {
            "PUMP_ON" -> _telemetry.value = current.copy(pumpOn = true)
            "PUMP_OFF" -> _telemetry.value = current.copy(pumpOn = false)
            "MODE_AUTO" -> {
                _telemetry.value = current.copy(autoMode = true)
                _pumpMode.value = "AUTO"
            }
            "MODE_MANUAL" -> {
                _telemetry.value = current.copy(autoMode = false)
                _pumpMode.value = "MANUAL"
            }
            "RESET_LEAK" -> _telemetry.value = current.copy(leak = false)
        }
    }

    // --- Settings Actions ---
    fun updateServerIp(ip: String) {
        viewModelScope.launch {
            prefsRepo.updateServerIp(ip)
            _toastEvents.emit("Server IP updated to $ip")
        }
    }

    fun toggleSimulationMode(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateSimulationMode(enabled)
        }
    }

    fun toggleSimulatedLeak(leak: Boolean) {
        _telemetry.value = _telemetry.value.copy(leak = leak)
    }

    fun setSimulatedWaterLevel(pct: Float) {
        _telemetry.value = _telemetry.value.copy(waterLevelPct = pct)
    }

    // --- Simulation Engine ---
    private fun startSimulation() {
        stopSimulation()
        wsClient.disconnect()

        simulationJob = viewModelScope.launch {
            _telemetry.value = TelemetryData(
                waterLevelPct = 65f,
                flowRateLpm = 2.5f,
                totalLiters = 1250f,
                pulses = 5000,
                pumpOn = true,
                autoMode = true
            )
            _systemMetrics.value = SystemMetrics(
                latencyMs = 12,
                serverUptime = "SIM",
                connectedClients = 1,
                uptimeMs = 0
            )

            while (isActive) {
                delay(2000)
                val current = _telemetry.value
                val newLevel = (current.waterLevelPct + (Math.random() * 4 - 2).toFloat())
                    .coerceIn(0f, 100f)
                val newFlow = (current.flowRateLpm + (Math.random() * 1 - 0.5).toFloat())
                    .coerceIn(0f, 15f)
                _telemetry.value = current.copy(
                    waterLevelPct = newLevel,
                    flowRateLpm = newFlow,
                    totalLiters = current.totalLiters + newFlow * (2f / 60f)
                )
            }
        }
    }

    private fun stopSimulation() {
        simulationJob?.cancel()
        simulationJob = null
    }

    override fun onCleared() {
        super.onCleared()
        wsClient.destroy()
        stopSimulation()
    }
}
