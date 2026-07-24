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

    fun connect(serverIp: String) {
        disconnect()
        _connectionState.value = ConnectionState.CONNECTING

        val url = "ws://$serverIp:8765"
        val request = try {
            Request.Builder().url(url).build()
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid server IP format: $url")
            _connectionState.value = ConnectionState.ERROR
            return
        }

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected to $url")
                _connectionState.value = ConnectionState.CONNECTED
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
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $reason")
                _connectionState.value = ConnectionState.DISCONNECTED
                scheduleReconnect(serverIp)
            }
        })
    }

    fun sendCommand(command: String) {
        val json = JSONObject().apply {
            put("type", "command")
            put("command", command)
        }
        webSocket?.send(json.toString())
        Log.d(TAG, "Sent command: $command")
    }

    fun disconnect() {
        reconnectJob?.cancel()
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    private fun scheduleReconnect(serverIp: String) {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(5000)
            Log.d(TAG, "Attempting reconnect...")
            connect(serverIp)
        }
    }

    fun destroy() {
        disconnect()
        scope.cancel()
    }
}
