package me.shadykhalifa.whispertop.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.shadykhalifa.whispertop.data.models.AppSettingsEntity
import me.shadykhalifa.whispertop.data.models.AudioFileEntity

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "whispertop_settings")

class PreferencesDataSourceImpl(
    private val context: Context
) : PreferencesDataSource {

    private companion object {
        val KEY_SETTINGS = stringPreferencesKey("settings")
        val KEY_LAST_RECORDING = stringPreferencesKey("last_recording")
    }

    private val dataStore = context.dataStore
    private val json = Json { ignoreUnknownKeys = true }
    
    override suspend fun getSettings(): AppSettingsEntity {
        return dataStore.data.map { preferences ->
            val settingsJson = preferences[KEY_SETTINGS]
            if (settingsJson != null) {
                try {
                    json.decodeFromString<AppSettingsEntity>(settingsJson)
                } catch (e: Exception) {
                    AppSettingsEntity()
                }
            } else {
                AppSettingsEntity()
            }
        }.first()
    }

    override suspend fun saveSettings(settings: AppSettingsEntity) {
        println("PreferencesDataSourceImpl: saveSettings called")
        println("PreferencesDataSourceImpl: settings.selectedModel='${settings.selectedModel}'")
        println("PreferencesDataSourceImpl: settings.apiKey length=${settings.apiKey.length}")
        println("PreferencesDataSourceImpl: settings.baseUrl='${settings.baseUrl}'")
        val settingsJson = json.encodeToString(settings)
        println("PreferencesDataSourceImpl: JSON serialized, length=${settingsJson.length}")
        dataStore.edit { preferences ->
            println("PreferencesDataSourceImpl: DataStore edit block started")
            preferences[KEY_SETTINGS] = settingsJson
            println("PreferencesDataSourceImpl: DataStore preferences updated")
        }
        println("PreferencesDataSourceImpl: DataStore edit completed")
    }

    override fun getSettingsFlow(): Flow<AppSettingsEntity> {
        return dataStore.data.map { preferences ->
            val settingsJson = preferences[KEY_SETTINGS]
            if (settingsJson != null) {
                try {
                    json.decodeFromString<AppSettingsEntity>(settingsJson)
                } catch (e: Exception) {
                    AppSettingsEntity()
                }
            } else {
                AppSettingsEntity()
            }
        }
    }

    override suspend fun getLastRecording(): AudioFileEntity? {
        return dataStore.data.map { preferences ->
            val recordingJson = preferences[KEY_LAST_RECORDING]
            if (recordingJson != null) {
                try {
                    json.decodeFromString<AudioFileEntity>(recordingJson)
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
        }.first()
    }

    override suspend fun saveLastRecording(audioFile: AudioFileEntity) {
        val recordingJson = json.encodeToString(audioFile)
        dataStore.edit { preferences ->
            preferences[KEY_LAST_RECORDING] = recordingJson
        }
    }

    override suspend fun clearLastRecording() {
        dataStore.edit { preferences ->
            preferences.remove(KEY_LAST_RECORDING)
        }
    }
}

actual fun createPreferencesDataSource(): PreferencesDataSource {
    throw IllegalStateException("Context must be provided via DI. Use AndroidModule to inject PreferencesDataSource.")
}