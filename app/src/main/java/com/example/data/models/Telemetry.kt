package com.example.data.models

import androidx.annotation.Keep

@Keep
data class TelemetryData(
    val distanceCm: Double = 12.4,
    val waterLevelPct: Double = 78.5,
    val pump: Boolean = false,
    val valve: Boolean = false,
    val rationing: Boolean = false,
    val leak: Boolean = false,
    val pulses: Int = 0,
    val flowRateLpm: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)

enum class PumpMode {
    AUTO,
    MANUAL_ON,
    MANUAL_OFF
}

enum class ConnectionState {
    CONNECTED,
    RECONNECTING,
    DISCONNECTED,
    SIMULATED
}

data class SystemMetrics(
    val latencyMs: Long = 18,
    val wifiSignalDbm: Int = -62,
    val lastPayloadTime: Long = System.currentTimeMillis(),
    val totalVolumeLiters: Double = 1000.0,
    val currentVolumeLiters: Double = 785.0
)
