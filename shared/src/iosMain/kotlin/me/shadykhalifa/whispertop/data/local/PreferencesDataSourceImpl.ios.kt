package me.shadykhalifa.whispertop.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.shadykhalifa.whispertop.data.models.AppSettingsEntity
import me.shadykhalifa.whispertop.data.models.AudioFileEntity
import platform.Foundation.*

class PreferencesDataSourceImpl : PreferencesDataSource {

    private companion object {
        const val KEY_SETTINGS = "settings"
        const val KEY_LAST_RECORDING = "last_recording"
    }

    private val userDefaults = NSUserDefaults.standardUserDefaults
    private val json = Json { ignoreUnknownKeys = true }
    
    private val _settingsFlow = MutableStateFlow(getSettingsSync())

    override suspend fun getSettings(): AppSettingsEntity {
        return getSettingsSync()
    }
    
    private fun getSettingsSync(): AppSettingsEntity {
        val settingsJson = userDefaults.stringForKey(KEY_SETTINGS)
        return if (settingsJson != null) {
            try {
                json.decodeFromString<AppSettingsEntity>(settingsJson)
            } catch (e: Exception) {
                AppSettingsEntity()
            }
        } else {
            AppSettingsEntity()
        }
    }

    override suspend fun saveSettings(settings: AppSettingsEntity) {
        val settingsJson = json.encodeToString(settings)
        userDefaults.setObject(settingsJson, KEY_SETTINGS)
        userDefaults.synchronize()
        _settingsFlow.value = settings
    }

    override fun getSettingsFlow(): Flow<AppSettingsEntity> = _settingsFlow.asStateFlow()

    override suspend fun getLastRecording(): AudioFileEntity? {
        val recordingJson = userDefaults.stringForKey(KEY_LAST_RECORDING)
        return if (recordingJson != null) {
            try {
                json.decodeFromString<AudioFileEntity>(recordingJson)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    override suspend fun saveLastRecording(audioFile: AudioFileEntity) {
        val recordingJson = json.encodeToString(audioFile)
        userDefaults.setObject(recordingJson, KEY_LAST_RECORDING)
        userDefaults.synchronize()
    }

    override suspend fun clearLastRecording() {
        userDefaults.removeObjectForKey(KEY_LAST_RECORDING)
        userDefaults.synchronize()
    }
}

actual fun createPreferencesDataSource(): PreferencesDataSource {
    return PreferencesDataSourceImpl()
}