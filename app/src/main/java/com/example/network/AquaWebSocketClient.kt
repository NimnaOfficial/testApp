package com.example.network

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject

enum class ConnectionState { CONNECTING, CONNECTED, DISCONNECTED, ERROR }

class AquaWebSocketClient {

    private val TAG = "AquaWebSocket"
    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    private var reconnectJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var reconnectAttempts = 0
    private val MAX_RECONNECT_ATTEMPTS = 10
    private val BASE_DELAY_MS = 3000L

    fun connect(serverIp: String) {
        disconnect()
        _connectionState.value = ConnectionState.CONNECTING
        reconnectAttempts = 0

        connectInternal(serverIp)
    }

    private fun connectInternal(serverIp: String) {
        val url = "ws://$serverIp:8765"
        val request = try {
            Request.Builder().url(url).build()
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid server IP format: $url")
            _connectionState.value = ConnectionState.ERROR
            return
        }

        try {
            webSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Log.d(TAG, "WebSocket connected to $url")
                    _connectionState.value = ConnectionState.CONNECTED
                    reconnectAttempts = 0
                    reconnectJob?.cancel()
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    scope.launch { _messages.send(text) }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.e(TAG, "WebSocket error: ${t.message}")
                    _connectionState.value = ConnectionState.ERROR
                    scheduleReconnect(serverIp)
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    Log.d(TAG, "WebSocket closing: $reason")
                    try {
                        webSocket.close(1000, null)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error closing WebSocket: ${e.message}")
                    }
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    Log.d(TAG, "WebSocket closed: $reason")
                    _connectionState.value = ConnectionState.DISCONNECTED
                    scheduleReconnect(serverIp)
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create WebSocket connection: ${e.message}")
            _connectionState.value = ConnectionState.ERROR
        }
    }

    fun sendCommand(command: String) {
        try {
            val json = JSONObject().apply {
                put("type", "command")
                put("command", command)
            }
            val sent = webSocket?.send(json.toString()) ?: false
            if (!sent) {
                Log.w(TAG, "Command not sent, WebSocket is disconnected: $command")
            } else {
                Log.d(TAG, "Sent command: $command")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send command '$command': ${e.message}")
        }
    }

    fun disconnect() {
        reconnectJob?.cancel()
        try {
            webSocket?.close(1000, "Client disconnect")
        } catch (e: Exception) {
            Log.e(TAG, "Error during disconnect: ${e.message}")
        }
        webSocket = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    private fun scheduleReconnect(serverIp: String) {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            Log.w(TAG, "Max reconnect attempts ($MAX_RECONNECT_ATTEMPTS) reached. Giving up.")
            _connectionState.value = ConnectionState.ERROR
            return
        }

        reconnectJob?.cancel()
        reconnectAttempts++

        // Exponential backoff: 3s, 6s, 12s, 24s... capped at 60s
        val delayMs = (BASE_DELAY_MS * (1L shl (reconnectAttempts - 1).coerceAtMost(4)))
            .coerceAtMost(60_000L)

        reconnectJob = scope.launch {
            Log.d(TAG, "Reconnect attempt $reconnectAttempts/$MAX_RECONNECT_ATTEMPTS in ${delayMs}ms...")
            delay(delayMs)
            connectInternal(serverIp)
        }
    }

    fun destroy() {
        disconnect()
        scope.cancel()
    }
}
