package com.example.data.models

data class TelemetryData(
    val waterLevelPct: Float = 0f,
    val flowRateLpm: Float = 0f,
    val totalLiters: Float = 0f,
    val pulses: Int = 0,
    val leak: Boolean = false,
    val pumpOn: Boolean = false,
    val autoMode: Boolean = true
)

data class SystemMetrics(
    val latencyMs: Long = 0L,
    val serverUptime: String = "--",
    val connectedClients: Int = 0,
    val uptimeMs: Long = 0L
)
