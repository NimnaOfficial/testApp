package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TelemetrySnapshot::class, ActivityLog::class], version = 1, exportSchema = false)
abstract class AquaDatabase : RoomDatabase() {
    abstract fun aquaDao(): AquaDao

    companion object {
        @Volatile
        private var INSTANCE: AquaDatabase? = null

        fun getDatabase(context: Context): AquaDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AquaDatabase::class.java,
                    "aqua_database"
                ).fallbackToDestructiveMigration(false)
                    .build().also { INSTANCE = it }
            }
        }
    }
}
