package me.shadykhalifa.whispertop.di

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import me.shadykhalifa.whispertop.data.local.PreferencesDataSource
import me.shadykhalifa.whispertop.data.models.AppSettingsEntity
import me.shadykhalifa.whispertop.data.models.AudioFileEntity
import me.shadykhalifa.whispertop.domain.models.AudioFile
import me.shadykhalifa.whispertop.domain.services.AudioRecorderService
import me.shadykhalifa.whispertop.domain.services.FileReaderService
import org.koin.dsl.module

class MockPreferencesDataSource : PreferencesDataSource {
    private val _settingsFlow = MutableStateFlow(AppSettingsEntity())
    
    override suspend fun getSettings(): AppSettingsEntity = AppSettingsEntity()
    
    override suspend fun saveSettings(settings: AppSettingsEntity) {
        _settingsFlow.value = settings
    }
    
    override fun getSettingsFlow(): Flow<AppSettingsEntity> = _settingsFlow
    
    override suspend fun getLastRecording(): AudioFileEntity? = null
    
    override suspend fun saveLastRecording(audioFile: AudioFileEntity) {}
    
    override suspend fun clearLastRecording() {}
}

class MockAudioRecorderService : AudioRecorderService {
    override suspend fun startRecording() {}
    
    override suspend fun stopRecording(): AudioFile = AudioFile(
        path = "/mock/path/recording.wav",
        durationMs = 10000L,
        sizeBytes = 1024L
    )
    
    override suspend fun cancelRecording() {}
}

class MockFileReaderService : FileReaderService {
    override suspend fun readFileAsBytes(filePath: String): ByteArray = byteArrayOf(1, 2, 3, 4, 5)
}

val mockPlatformModule = module {
    single<PreferencesDataSource> { MockPreferencesDataSource() }
    single<AudioRecorderService> { MockAudioRecorderService() }
    single<FileReaderService> { MockFileReaderService() }
}