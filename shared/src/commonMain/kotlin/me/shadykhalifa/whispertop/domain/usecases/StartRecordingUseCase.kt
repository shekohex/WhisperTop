package me.shadykhalifa.whispertop.domain.usecases

import me.shadykhalifa.whispertop.domain.repositories.AudioRepository
import me.shadykhalifa.whispertop.domain.repositories.SettingsRepository
import me.shadykhalifa.whispertop.utils.Result

class StartRecordingUseCase(
    private val audioRepository: AudioRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        val settings = settingsRepository.getSettings()
        
        if (settings.apiKey.isBlank()) {
            return Result.Error(IllegalStateException("API key not configured"))
        }
        
        if (audioRepository.isRecording()) {
            return Result.Error(IllegalStateException("Already recording"))
        }
        
        return audioRepository.startRecording()
    }
}