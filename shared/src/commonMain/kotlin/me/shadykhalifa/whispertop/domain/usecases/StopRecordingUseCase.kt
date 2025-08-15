package me.shadykhalifa.whispertop.domain.usecases

import me.shadykhalifa.whispertop.domain.models.TranscriptionRequest
import me.shadykhalifa.whispertop.domain.repositories.AudioRepository
import me.shadykhalifa.whispertop.domain.repositories.SettingsRepository
import me.shadykhalifa.whispertop.domain.repositories.TranscriptionRepository
import me.shadykhalifa.whispertop.utils.Result

class StopRecordingUseCase(
    private val audioRepository: AudioRepository,
    private val transcriptionRepository: TranscriptionRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(): Result<String> {
        if (!audioRepository.isRecording()) {
            return Result.Error(IllegalStateException("Not currently recording"))
        }
        
        val audioFileResult = audioRepository.stopRecording()
        
        return when (audioFileResult) {
            is Result.Success -> {
                val settings = settingsRepository.getSettings()
                val request = TranscriptionRequest(
                    audioFile = audioFileResult.data,
                    language = if (settings.autoDetectLanguage) null else settings.language,
                    model = settings.selectedModel
                )
                
                when (val transcriptionResult = transcriptionRepository.transcribe(request)) {
                    is Result.Success -> Result.Success(transcriptionResult.data.text)
                    is Result.Error -> Result.Error(transcriptionResult.exception)
                    is Result.Loading -> Result.Loading
                }
            }
            is Result.Error -> Result.Error(audioFileResult.exception)
            is Result.Loading -> Result.Loading
        }
    }
}