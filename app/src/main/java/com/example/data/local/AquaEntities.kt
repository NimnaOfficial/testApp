package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "telemetry_snapshots")
data class TelemetrySnapshot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val waterLevelPct: Double,
    val distanceCm: Double,
    val pumpOn: Boolean,
    val valveOn: Boolean,
    val pulses: Int,
    val flowRateLpm: Double,
    val isLeak: Boolean,
    val isRationing: Boolean
)

@Entity(tableName = "activity_logs")
data class ActivityLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val title: String,
    val description: String,
    val logType: String // "PUMP", "VALVE", "RATIONING", "LEAK", "OVERRIDE", "SYSTEM"
)
