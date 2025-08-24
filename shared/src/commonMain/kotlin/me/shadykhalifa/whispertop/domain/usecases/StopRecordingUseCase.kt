package me.shadykhalifa.whispertop.domain.usecases

import me.shadykhalifa.whispertop.domain.models.TranscriptionRequest
import me.shadykhalifa.whispertop.domain.repositories.AudioRepository
import me.shadykhalifa.whispertop.domain.repositories.SettingsRepository
import me.shadykhalifa.whispertop.domain.repositories.TranscriptionRepository
import me.shadykhalifa.whispertop.domain.models.ErrorNotificationService
import me.shadykhalifa.whispertop.utils.Result

class StopRecordingUseCase(
    private val audioRepository: AudioRepository,
    private val transcriptionRepository: TranscriptionRepository,
    private val settingsRepository: SettingsRepository,
    private val errorNotificationService: ErrorNotificationService
) {
    suspend operator fun invoke(): Result<String> {
        return try {
            if (!audioRepository.isRecording()) {
                val error = IllegalStateException("Not currently recording")
                errorNotificationService.showError(error, "stop_recording")
                return Result.Error(error)
            }
            
            val audioFileResult = audioRepository.stopRecording()
            
            when (audioFileResult) {
                is Result.Success -> {
                    val settings = settingsRepository.getSettings()
                    val request = TranscriptionRequest(
                        audioFile = audioFileResult.data,
                        language = if (settings.autoDetectLanguage) null else settings.language,
                        model = settings.selectedModel,
                        customPrompt = settings.customPrompt,
                        temperature = settings.temperature
                    )
                    
                    when (val transcriptionResult = transcriptionRepository.transcribe(request)) {
                        is Result.Success -> Result.Success(transcriptionResult.data.text)
                        is Result.Error -> {
                            errorNotificationService.showError(transcriptionResult.exception, "stop_recording_transcription")
                            Result.Error(transcriptionResult.exception)
                        }
                        is Result.Loading -> Result.Loading
                    }
                }
                is Result.Error -> {
                    errorNotificationService.showError(audioFileResult.exception, "stop_recording_audio")
                    Result.Error(audioFileResult.exception)
                }
                is Result.Loading -> Result.Loading
            }
        } catch (e: Exception) {
            errorNotificationService.showError(e, "stop_recording")
            Result.Error(e)
        }
    }
}