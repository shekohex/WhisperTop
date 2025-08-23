package me.shadykhalifa.whispertop.domain.usecases

import me.shadykhalifa.whispertop.domain.managers.AudioRecordingStateManager
import me.shadykhalifa.whispertop.domain.services.ErrorLoggingService
import me.shadykhalifa.whispertop.domain.services.IAudioServiceManager
import me.shadykhalifa.whispertop.utils.Result

class UnbindAudioServiceUseCase(
    private val audioServiceManager: IAudioServiceManager,
    private val audioRecordingStateManager: AudioRecordingStateManager,
    private val errorLoggingService: ErrorLoggingService
) {
    operator fun invoke(): Result<Unit> {
        return try {
            if (!audioServiceManager.isServiceBound()) {
                logWarning("Attempted to unbind service that was not bound")
                return Result.Success(Unit)
            }
            
            // Reset recording state to idle before unbinding
            audioRecordingStateManager.resetToIdle()
            
            // Unbind the service
            audioServiceManager.unbindService()
            
            logSuccess()
            Result.Success(Unit)
            
        } catch (e: Exception) {
            logError("Unexpected error during service unbinding", e)
            Result.Error(e)
        }
    }
    
    private fun logSuccess() {
        errorLoggingService.logWarning(
            message = "Audio service unbinding completed successfully",
            context = mapOf("useCase" to "UnbindAudioServiceUseCase")
        )
    }
    
    private fun logWarning(message: String) {
        errorLoggingService.logWarning(
            message = message,
            context = mapOf("useCase" to "UnbindAudioServiceUseCase")
        )
    }
    
    private fun logError(message: String, exception: Exception) {
        errorLoggingService.logError(
            error = exception,
            context = mapOf("useCase" to "UnbindAudioServiceUseCase"),
            additionalInfo = message
        )
    }
}