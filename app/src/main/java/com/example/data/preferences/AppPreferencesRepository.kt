package com.example.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore by preferencesDataStore(name = "aqua_settings")

data class AppConfig(
    val serverIp: String = "192.168.1.100",
    val simulationMode: Boolean = false
)

class AppPreferencesRepository(private val context: Context) {

    companion object {
        val SERVER_IP_KEY = stringPreferencesKey("server_ip")
        val SIMULATION_MODE_KEY = booleanPreferencesKey("simulation_mode")
    }

    val appConfig: Flow<AppConfig> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            AppConfig(
                serverIp = prefs[SERVER_IP_KEY] ?: "192.168.1.100",
                simulationMode = prefs[SIMULATION_MODE_KEY] ?: false
            )
        }

    suspend fun updateServerIp(ip: String) {
        try {
            context.dataStore.edit { it[SERVER_IP_KEY] = ip }
        } catch (e: IOException) {
            // Silently fail — preference will use default next read
        }
    }

    suspend fun updateSimulationMode(enabled: Boolean) {
        try {
            context.dataStore.edit { it[SIMULATION_MODE_KEY] = enabled }
        } catch (e: IOException) {
            // Silently fail — preference will use default next read
        }
    }
}
