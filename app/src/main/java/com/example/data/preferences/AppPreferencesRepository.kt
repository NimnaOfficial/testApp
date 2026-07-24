package com.example.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "aquaintel_settings")

data class AppConfig(
    val serverIp: String = "192.168.1.5:8080",
    val useSimulationMode: Boolean = true, // Defaults to true so app works out-of-the-box in browser!
    val hysteresisLowPct: Double = 50.0,
    val hysteresisHighPct: Double = 95.0,
    val tankHeightCm: Double = 100.0,
    val tankCapacityLiters: Double = 1000.0,
    val telegramBotEnabled: Boolean = true,
    val telegramChatId: String = "12345678"
)

class AppPreferencesRepository(private val context: Context) {

    private object Keys {
        val SERVER_IP = stringPreferencesKey("server_ip")
        val USE_SIMULATION = booleanPreferencesKey("use_simulation")
        val HYSTERESIS_LOW = doublePreferencesKey("hysteresis_low")
        val HYSTERESIS_HIGH = doublePreferencesKey("hysteresis_high")
        val TANK_HEIGHT = doublePreferencesKey("tank_height")
        val TANK_CAPACITY = doublePreferencesKey("tank_capacity")
        val TELEGRAM_ENABLED = booleanPreferencesKey("telegram_enabled")
        val TELEGRAM_CHAT_ID = stringPreferencesKey("telegram_chat_id")
    }

    val appConfigFlow: Flow<AppConfig> = context.dataStore.data.map { prefs ->
        AppConfig(
            serverIp = prefs[Keys.SERVER_IP] ?: "192.168.1.5:8080",
            useSimulationMode = prefs[Keys.USE_SIMULATION] ?: true,
            hysteresisLowPct = prefs[Keys.HYSTERESIS_LOW] ?: 50.0,
            hysteresisHighPct = prefs[Keys.HYSTERESIS_HIGH] ?: 95.0,
            tankHeightCm = prefs[Keys.TANK_HEIGHT] ?: 100.0,
            tankCapacityLiters = prefs[Keys.TANK_CAPACITY] ?: 1000.0,
            telegramBotEnabled = prefs[Keys.TELEGRAM_ENABLED] ?: true,
            telegramChatId = prefs[Keys.TELEGRAM_CHAT_ID] ?: "12345678"
        )
    }

    suspend fun updateServerIp(ip: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SERVER_IP] = ip
        }
    }

    suspend fun updateSimulationMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.USE_SIMULATION] = enabled
        }
    }

    suspend fun updateHysteresis(low: Double, high: Double) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HYSTERESIS_LOW] = low
            prefs[Keys.HYSTERESIS_HIGH] = high
        }
    }

    suspend fun updateTankParameters(heightCm: Double, capacityLiters: Double) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TANK_HEIGHT] = heightCm
            prefs[Keys.TANK_CAPACITY] = capacityLiters
        }
    }
}
