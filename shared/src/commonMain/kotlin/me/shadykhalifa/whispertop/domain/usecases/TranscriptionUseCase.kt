package me.shadykhalifa.whispertop.domain.usecases

import kotlinx.coroutines.flow.StateFlow
import me.shadykhalifa.whispertop.domain.managers.RecordingManager
import me.shadykhalifa.whispertop.domain.models.RecordingState
import me.shadykhalifa.whispertop.domain.repositories.SettingsRepository
import me.shadykhalifa.whispertop.domain.repositories.TranscriptionRepository
import me.shadykhalifa.whispertop.utils.Result

class TranscriptionUseCase(
    private val recordingManager: RecordingManager,
    private val transcriptionRepository: TranscriptionRepository,
    private val settingsRepository: SettingsRepository
) {
    
    val recordingState: StateFlow<RecordingState> = recordingManager.recordingState
    
    suspend fun startRecording(): Result<Unit> {
        return try {
            val settings = settingsRepository.getSettings()
            
            if (settings.apiKey.isBlank()) {
                Result.Error(IllegalStateException("API key not configured"))
            } else if (!transcriptionRepository.isConfigured()) {
                Result.Error(IllegalStateException("Transcription service not configured"))
            } else {
                recordingManager.startRecording()
                Result.Success(Unit)
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    suspend fun stopRecording(): Result<Unit> {
        return try {
            recordingManager.stopRecording()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    fun cancelRecording() {
        recordingManager.cancelRecording()
    }
    
    fun retryFromError() {
        recordingManager.retryFromError()
    }
    
    fun resetToIdle() {
        recordingManager.resetToIdle()
    }
    
    fun cleanup() {
        recordingManager.cleanup()
    }
}