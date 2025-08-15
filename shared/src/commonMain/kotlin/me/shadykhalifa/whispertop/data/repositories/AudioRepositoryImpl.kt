package me.shadykhalifa.whispertop.data.repositories

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.shadykhalifa.whispertop.data.local.PreferencesDataSource
import me.shadykhalifa.whispertop.data.models.toDomain
import me.shadykhalifa.whispertop.data.models.toEntity
import me.shadykhalifa.whispertop.domain.models.AudioFile
import me.shadykhalifa.whispertop.domain.models.RecordingState
import me.shadykhalifa.whispertop.domain.repositories.AudioRepository
import me.shadykhalifa.whispertop.domain.services.AudioRecorderService
import me.shadykhalifa.whispertop.utils.Result

class AudioRepositoryImpl(
    private val preferencesDataSource: PreferencesDataSource,
    private val audioRecorderService: AudioRecorderService
) : BaseRepository(), AudioRepository {

    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
    override val recordingState: Flow<RecordingState> = _recordingState.asStateFlow()

    override suspend fun startRecording(): Result<Unit> = execute {
        if (_recordingState.value != RecordingState.Idle) {
            throw IllegalStateException("Recording already in progress")
        }
        
        _recordingState.value = RecordingState.Recording
        audioRecorderService.startRecording()
    }

    override suspend fun stopRecording(): Result<AudioFile> = execute {
        if (_recordingState.value != RecordingState.Recording) {
            throw IllegalStateException("No recording in progress")
        }
        
        _recordingState.value = RecordingState.Processing
        
        try {
            val audioFile = audioRecorderService.stopRecording()
            preferencesDataSource.saveLastRecording(audioFile.toEntity())
            _recordingState.value = RecordingState.Idle
            audioFile
        } catch (e: Exception) {
            _recordingState.value = RecordingState.Error("Failed to stop recording", e)
            throw e
        }
    }

    override suspend fun cancelRecording(): Result<Unit> = execute {
        if (_recordingState.value != RecordingState.Recording) {
            throw IllegalStateException("No recording in progress")
        }
        
        audioRecorderService.cancelRecording()
        _recordingState.value = RecordingState.Idle
    }

    override fun isRecording(): Boolean {
        return _recordingState.value == RecordingState.Recording
    }

    override suspend fun getLastRecording(): AudioFile? {
        return preferencesDataSource.getLastRecording()?.toDomain()
    }
}

expect class AudioRecorder {
    suspend fun startRecording()
    suspend fun stopRecording(): AudioFile
    suspend fun cancelRecording()
}