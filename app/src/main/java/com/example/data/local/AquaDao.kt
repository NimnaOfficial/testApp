package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AquaDao {
    // Snapshots for historical charts
    @Query("SELECT * FROM telemetry_snapshots ORDER BY timestamp DESC LIMIT 60")
    fun getRecentSnapshots(): Flow<List<TelemetrySnapshot>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: TelemetrySnapshot)

    @Query("DELETE FROM telemetry_snapshots WHERE id NOT IN (SELECT id FROM telemetry_snapshots ORDER BY timestamp DESC LIMIT 500)")
    suspend fun pruneOldSnapshots()

    // Activity Logs
    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC LIMIT 100")
    fun getAllActivityLogs(): Flow<List<ActivityLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityLog(log: ActivityLog)

    @Query("DELETE FROM activity_logs")
    suspend fun clearActivityLogs()
}
