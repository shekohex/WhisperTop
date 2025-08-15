package me.shadykhalifa.whispertop.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.shadykhalifa.whispertop.data.models.AppSettingsEntity
import me.shadykhalifa.whispertop.data.models.AudioFileEntity

class PreferencesDataSourceImpl(
    private val context: Context
) : PreferencesDataSource {

    private companion object {
        const val PREFS_NAME = "whispertop_prefs"
        const val KEY_SETTINGS = "settings"
        const val KEY_LAST_RECORDING = "last_recording"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    
    private val _settingsFlow = MutableStateFlow(getSettingsSync())
    
    override suspend fun getSettings(): AppSettingsEntity = withContext(Dispatchers.IO) {
        getSettingsSync()
    }
    
    private fun getSettingsSync(): AppSettingsEntity {
        val settingsJson = prefs.getString(KEY_SETTINGS, null)
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

    override suspend fun saveSettings(settings: AppSettingsEntity) = withContext(Dispatchers.IO) {
        val settingsJson = json.encodeToString(settings)
        prefs.edit().putString(KEY_SETTINGS, settingsJson).apply()
        _settingsFlow.value = settings
    }

    override fun getSettingsFlow(): Flow<AppSettingsEntity> = _settingsFlow.asStateFlow()

    override suspend fun getLastRecording(): AudioFileEntity? = withContext(Dispatchers.IO) {
        val recordingJson = prefs.getString(KEY_LAST_RECORDING, null)
        return@withContext if (recordingJson != null) {
            try {
                json.decodeFromString<AudioFileEntity>(recordingJson)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    override suspend fun saveLastRecording(audioFile: AudioFileEntity) = withContext(Dispatchers.IO) {
        val recordingJson = json.encodeToString(audioFile)
        prefs.edit().putString(KEY_LAST_RECORDING, recordingJson).apply()
    }

    override suspend fun clearLastRecording() = withContext(Dispatchers.IO) {
        prefs.edit().remove(KEY_LAST_RECORDING).apply()
    }
}

actual fun createPreferencesDataSource(): PreferencesDataSource {
    throw IllegalStateException("Context must be provided via DI. Use AndroidModule to inject PreferencesDataSource.")
}