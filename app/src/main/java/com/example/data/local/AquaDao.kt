package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AquaDao {
    @Insert
    suspend fun insertSnapshot(snapshot: TelemetrySnapshot)

    @Query("SELECT * FROM telemetry_snapshots ORDER BY timestamp DESC LIMIT 30")
    fun getRecentSnapshots(): Flow<List<TelemetrySnapshot>>

    @Insert
    suspend fun insertLog(log: ActivityLog)

    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC LIMIT 50")
    fun getRecentLogs(): Flow<List<ActivityLog>>
}
