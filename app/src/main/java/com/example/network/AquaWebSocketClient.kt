package com.example.network

import android.util.Log
import com.example.data.models.ConnectionState
import com.example.data.models.TelemetryData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AquaWebSocketClient {

    private val tag = "AquaWebSocketClient"

    private val client = OkHttpClient.Builder()
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .pingInterval(2, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var heartbeatJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _telemetryData = MutableStateFlow(TelemetryData())
    val telemetryData: StateFlow<TelemetryData> = _telemetryData.asStateFlow()

    private val _errorEvents = MutableSharedFlow<String>()
    val errorEvents: SharedFlow<String> = _errorEvents.asSharedFlow()

    private var lastReceivedTimestamp: Long = 0

    fun connect(serverAddress: String) {
        disconnect()

        val formattedUrl = when {
            serverAddress.startsWith("ws://") || serverAddress.startsWith("wss://") -> serverAddress
            else -> "ws://$serverAddress/ws"
        }

        Log.d(tag, "Connecting to WebSocket URL: $formattedUrl")
        _connectionState.value = ConnectionState.RECONNECTING

        val request = Request.Builder()
            .url(formattedUrl)
            .build()

        webSocket = client.newWebSocket(request, createWebSocketListener())
        startHeartbeatMonitor()
    }

    fun disconnect() {
        heartbeatJob?.cancel()
        heartbeatJob = null
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    fun sendCommand(command: String): Boolean {
        val ws = webSocket
        if (ws != null && _connectionState.value == ConnectionState.CONNECTED) {
            val success = ws.send(command)
            Log.d(tag, "Dispatched command '$command' -> success: $success")
            return success
        }
        Log.w(tag, "Cannot send command '$command'. Connection state: ${_connectionState.value}")
        return false
    }

    private fun createWebSocketListener(): WebSocketListener {
        return object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(tag, "WebSocket connection established!")
                _connectionState.value = ConnectionState.CONNECTED
                lastReceivedTimestamp = System.currentTimeMillis()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                lastReceivedTimestamp = System.currentTimeMillis()
                _connectionState.value = ConnectionState.CONNECTED
                parseTelemetryJson(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(tag, "WebSocket closing: $code / $reason")
                _connectionState.value = ConnectionState.DISCONNECTED
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(tag, "WebSocket failure", t)
                _connectionState.value = ConnectionState.DISCONNECTED
                scope.launch {
                    _errorEvents.emit("Connection failed: ${t.localizedMessage ?: "Network error"}")
                }
            }
        }
    }

    private fun parseTelemetryJson(jsonString: String) {
        try {
            val root = JSONObject(jsonString)
            val telemetryObj = if (root.has("telemetry")) root.getJSONObject("telemetry") else root

            val distanceCm = telemetryObj.optDouble("distance_cm", 12.4)
            val waterLevelPct = telemetryObj.optDouble("water_level_pct", 78.5)
            val pump = telemetryObj.optBoolean("pump", false)
            val valve = telemetryObj.optBoolean("valve", false)
            val rationing = telemetryObj.optBoolean("rationing", false)
            val leak = telemetryObj.optBoolean("leak", false)
            val pulses = telemetryObj.optInt("pulses", 0)

            // Flow rate calculation: 7.5 pulses per second ~ 1 L/min
            val flowRateLpm = (pulses / 7.5)

            _telemetryData.value = TelemetryData(
                distanceCm = distanceCm,
                waterLevelPct = waterLevelPct.coerceIn(0.0, 100.0),
                pump = pump,
                valve = valve,
                rationing = rationing,
                leak = leak,
                pulses = pulses,
                flowRateLpm = flowRateLpm,
                timestamp = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.e(tag, "Error parsing telemetry JSON", e)
        }
    }

    private fun startHeartbeatMonitor() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (true) {
                delay(1000)
                val elapsedSinceLastMsg = System.currentTimeMillis() - lastReceivedTimestamp
                if (lastReceivedTimestamp > 0 && elapsedSinceLastMsg > 3000) {
                    if (_connectionState.value == ConnectionState.CONNECTED) {
                        Log.w(tag, "No WebSocket payload for 3+ seconds! Marking DISCONNECTED")
                        _connectionState.value = ConnectionState.DISCONNECTED
                    }
                }
            }
        }
    }
}
