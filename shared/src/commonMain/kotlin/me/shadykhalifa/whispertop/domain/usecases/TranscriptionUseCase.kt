package me.shadykhalifa.whispertop.domain.usecases

import kotlinx.coroutines.flow.StateFlow
import me.shadykhalifa.whispertop.domain.managers.RecordingManager
import me.shadykhalifa.whispertop.domain.models.RecordingState
import me.shadykhalifa.whispertop.domain.repositories.SettingsRepository
import me.shadykhalifa.whispertop.domain.repositories.TranscriptionRepository
import me.shadykhalifa.whispertop.domain.models.ErrorNotificationService
import me.shadykhalifa.whispertop.utils.Result

class TranscriptionUseCase(
    private val recordingManager: RecordingManager,
    private val transcriptionRepository: TranscriptionRepository,
    private val settingsRepository: SettingsRepository,
    private val errorNotificationService: ErrorNotificationService
) {
    
    val recordingState: StateFlow<RecordingState> = recordingManager.recordingState
    
    suspend fun startRecording(): Result<Unit> {
        return try {
            val settings = settingsRepository.getSettings()
            
            if (settings.apiKey.isBlank()) {
                val error = IllegalStateException("API key not configured")
                errorNotificationService.showError(error, "transcription_start")
                Result.Error(error)
            } else if (!transcriptionRepository.isConfigured()) {
                val error = IllegalStateException("Transcription service not configured")
                errorNotificationService.showError(error, "transcription_start")
                Result.Error(error)
            } else {
                recordingManager.startRecording()
                Result.Success(Unit)
            }
        } catch (e: Exception) {
            errorNotificationService.showError(e, "transcription_start")
            Result.Error(e)
        }
    }
    
    suspend fun stopRecording(): Result<Unit> {
        return try {
            recordingManager.stopRecording()
            Result.Success(Unit)
        } catch (e: Exception) {
            errorNotificationService.showError(e, "transcription_stop")
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