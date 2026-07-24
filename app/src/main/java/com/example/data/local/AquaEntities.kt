package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "telemetry_snapshots")
data class TelemetrySnapshot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val waterLevelPct: Float = 0f,
    val flowRateLpm: Float = 0f,
    val totalLiters: Float = 0f,
    val pulses: Int = 0,
    val leak: Boolean = false,
    val pumpOn: Boolean = false
)

@Entity(tableName = "activity_logs")
data class ActivityLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val message: String = "",
    val type: String = "INFO"
)
