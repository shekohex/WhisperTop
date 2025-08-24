package me.shadykhalifa.whispertop.domain.usecases

import me.shadykhalifa.whispertop.domain.repositories.AudioRepository
import me.shadykhalifa.whispertop.domain.repositories.SettingsRepository
import me.shadykhalifa.whispertop.domain.models.ErrorNotificationService
import me.shadykhalifa.whispertop.utils.Result

class StartRecordingUseCase(
    private val audioRepository: AudioRepository,
    private val settingsRepository: SettingsRepository,
    private val errorNotificationService: ErrorNotificationService
) {
    suspend operator fun invoke(): Result<Unit> {
        val settings = settingsRepository.getSettings()
        
        if (settings.apiKey.isBlank()) {
            val error = IllegalStateException("API key not configured")
            errorNotificationService.showError(error, "start_recording")
            return Result.Error(error)
        }
        
        if (audioRepository.isRecording()) {
            val error = IllegalStateException("Already recording")
            errorNotificationService.showError(error, "start_recording")
            return Result.Error(error)
        }
        
        return try {
            audioRepository.startRecording()
        } catch (e: Exception) {
            errorNotificationService.showError(e, "start_recording")
            Result.Error(e)
        }
    }
}