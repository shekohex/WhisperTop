package me.shadykhalifa.whispertop.domain.usecases

import me.shadykhalifa.whispertop.domain.services.ErrorLoggingService
import me.shadykhalifa.whispertop.domain.services.IAudioServiceManager
import me.shadykhalifa.whispertop.utils.Result

class BindAudioServiceUseCase(
    private val audioServiceManager: IAudioServiceManager,
    private val errorLoggingService: ErrorLoggingService
) {
    suspend operator fun invoke(): Result<IAudioServiceManager.ServiceBindResult> {
        return try {
            if (audioServiceManager.isServiceBound()) {
                Result.Success(IAudioServiceManager.ServiceBindResult.AlreadyBound)
            } else {
                val result = audioServiceManager.bindService()
                when (result) {
                    is Result.Success -> {
                        when (result.data) {
                            is IAudioServiceManager.ServiceBindResult.Success -> {
                                logSuccess()
                                Result.Success(result.data)
                            }
                            is IAudioServiceManager.ServiceBindResult.Failed -> {
                                logError("Service binding failed", null)
                                Result.Success(result.data)
                            }
                            is IAudioServiceManager.ServiceBindResult.Error -> {
                                logError("Service binding error", result.data.exception)
                                Result.Success(result.data)
                            }
                            is IAudioServiceManager.ServiceBindResult.AlreadyBound -> {
                                Result.Success(result.data)
                            }
                        }
                    }
                    is Result.Error -> {
                        logError("Use case execution failed", result.exception)
                        Result.Error(result.exception)
                    }
                    is Result.Loading -> {
                        // This shouldn't happen in a suspend function, but handle it anyway
                        Result.Error(IllegalStateException("Unexpected loading state"))
                    }
                }
            }
        } catch (e: Exception) {
            logError("Unexpected error in service binding use case", e)
            Result.Error(e)
        }
    }
    
    private fun logSuccess() {
        errorLoggingService.logWarning(
            message = "Audio service binding completed successfully",
            context = mapOf("useCase" to "BindAudioServiceUseCase")
        )
    }
    
    private fun logError(message: String, exception: Throwable?) {
        exception?.let {
            errorLoggingService.logError(
                error = it,
                context = mapOf("useCase" to "BindAudioServiceUseCase"),
                additionalInfo = message
            )
        } ?: run {
            errorLoggingService.logWarning(
                message = message,
                context = mapOf("useCase" to "BindAudioServiceUseCase")
            )
        }
    }
}