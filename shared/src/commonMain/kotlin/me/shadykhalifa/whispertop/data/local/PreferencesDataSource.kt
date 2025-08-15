package me.shadykhalifa.whispertop.data.local

import kotlinx.coroutines.flow.Flow
import me.shadykhalifa.whispertop.data.models.AppSettingsEntity
import me.shadykhalifa.whispertop.data.models.AudioFileEntity

interface PreferencesDataSource {
    suspend fun getSettings(): AppSettingsEntity
    suspend fun saveSettings(settings: AppSettingsEntity)
    fun getSettingsFlow(): Flow<AppSettingsEntity>
    
    suspend fun getLastRecording(): AudioFileEntity?
    suspend fun saveLastRecording(audioFile: AudioFileEntity)
    suspend fun clearLastRecording()
}

expect fun createPreferencesDataSource(): PreferencesDataSource