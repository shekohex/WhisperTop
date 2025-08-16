package me.shadykhalifa.whispertop.domain.models

sealed class TranscriptionError : Exception() {
    data class ApiKeyMissing(override val message: String = "API key not configured. Please set your OpenAI API key in settings.") : TranscriptionError()
    
    data class ServiceNotConfigured(override val message: String = "Transcription service not configured properly.") : TranscriptionError()
    
    data class AccessibilityServiceNotEnabled(override val message: String = "Text insertion requires accessibility service. Please enable WhisperTop accessibility service in system settings.") : TranscriptionError()
    
    data class PermissionDenied(val permission: String, override val message: String = "Permission denied: $permission") : TranscriptionError()
    
    data class RecordingInProgress(override val message: String = "Recording is already in progress.") : TranscriptionError()
    
    data class NetworkError(override val cause: Throwable, override val message: String = "Network error occurred. Please check your internet connection.") : TranscriptionError()
    
    data class ApiError(val statusCode: Int, override val message: String) : TranscriptionError()
    
    data class AuthenticationError(override val message: String = "Invalid API key. Please check your OpenAI API key.") : TranscriptionError()
    
    data class RateLimitError(override val message: String = "API rate limit exceeded. Please try again later.") : TranscriptionError()
    
    data class AudioTooLarge(override val message: String = "Audio file too large. Maximum size is 25MB.") : TranscriptionError()
    
    data class AudioTooShort(override val message: String = "Audio recording too short. Please record for at least 1 second.") : TranscriptionError()
    
    data class StorageError(override val message: String = "Storage error. Please check available disk space.") : TranscriptionError()
    
    data class TextInsertionFailed(val transcription: String, override val message: String = "Failed to insert text automatically. You can copy the transcription manually.") : TranscriptionError()
    
    data class UnexpectedError(override val cause: Throwable, override val message: String = "An unexpected error occurred.") : TranscriptionError()
    
    companion object {
        fun fromThrowable(throwable: Throwable): TranscriptionError {
            return when (throwable) {
                is TranscriptionError -> throwable
                is IllegalStateException -> when {
                    throwable.message?.contains("API key") == true -> ApiKeyMissing()
                    throwable.message?.contains("Already recording") == true -> RecordingInProgress()
                    throwable.message?.contains("accessibility") == true -> AccessibilityServiceNotEnabled()
                    else -> UnexpectedError(throwable, throwable.message ?: "An unexpected error occurred")
                }
                else -> UnexpectedError(throwable, throwable.message ?: "An unexpected error occurred")
            }
        }
        
        fun fromOpenAIException(openAIException: Any): TranscriptionError {
            return when (openAIException::class.simpleName) {
                "AuthenticationException" -> AuthenticationError()
                "RateLimitException" -> RateLimitError()
                "NetworkException" -> NetworkError(openAIException as Throwable)
                "ServerException" -> UnexpectedError(openAIException as Throwable, "Server error occurred")
                "InvalidRequestException" -> UnexpectedError(openAIException as Throwable, "Invalid request")
                else -> UnexpectedError(openAIException as Throwable, "API error occurred")
            }
        }
    }
}