package me.shadykhalifa.whispertop.domain.models

import me.shadykhalifa.whispertop.data.audio.AudioRecordingError
import me.shadykhalifa.whispertop.data.models.OpenAIException

data class ErrorInfo(
    val title: String,
    val message: String,
    val actionText: String? = null,
    val isRetryable: Boolean = false,
    val retryDelay: Long = 0L,
    val severity: ErrorSeverity = ErrorSeverity.ERROR
)

enum class ErrorSeverity {
    WARNING,
    ERROR,
    CRITICAL
}

enum class ErrorCategory {
    NETWORK,
    AUTHENTICATION,
    PERMISSION,
    AUDIO,
    STORAGE,
    API,
    ACCESSIBILITY,
    CONFIGURATION,
    UNKNOWN
}

object ErrorClassifier {
    
    fun classifyError(error: Throwable): ErrorInfo {
        return when (error) {
            is TranscriptionError -> classifyTranscriptionError(error)
            is AudioRecordingError -> classifyAudioError(error)
            is OpenAIException -> classifyOpenAIError(error)
            is PlatformSecurityException -> classifyPermissionError(error)
            is SecurityException -> classifySecurityError(error)
            else -> classifyGenericError(error)
        }
    }
    
    private fun classifyTranscriptionError(error: TranscriptionError): ErrorInfo {
        return when (error) {
            is TranscriptionError.ApiKeyMissing -> ErrorInfo(
                title = "API Key Required",
                message = "Please configure your OpenAI API key in settings to use transcription.",
                actionText = "Open Settings",
                severity = ErrorSeverity.CRITICAL
            )
            
            is TranscriptionError.AuthenticationError -> ErrorInfo(
                title = "Invalid API Key",
                message = "Your OpenAI API key appears to be invalid. Please check and update it in settings.",
                actionText = "Update API Key",
                severity = ErrorSeverity.CRITICAL
            )
            
            is TranscriptionError.NetworkError -> ErrorInfo(
                title = "Connection Error",
                message = "Unable to connect to transcription service. Please check your internet connection.",
                actionText = "Retry",
                isRetryable = true,
                retryDelay = 3000L,
                severity = ErrorSeverity.ERROR
            )
            
            is TranscriptionError.RateLimitError -> ErrorInfo(
                title = "Rate Limit Exceeded",
                message = "Too many requests to the API. Please wait a moment before trying again.",
                actionText = "Retry",
                isRetryable = true,
                retryDelay = 10000L,
                severity = ErrorSeverity.WARNING
            )
            
            is TranscriptionError.AccessibilityServiceNotEnabled -> ErrorInfo(
                title = "Accessibility Required",
                message = "Enable WhisperTop accessibility service to automatically insert transcribed text.",
                actionText = "Enable Service",
                severity = ErrorSeverity.ERROR
            )
            
            is TranscriptionError.PermissionDenied -> ErrorInfo(
                title = "Permission Required",
                message = "WhisperTop needs ${error.permission.lowercase()} permission to function properly.",
                actionText = "Grant Permission",
                severity = ErrorSeverity.CRITICAL
            )
            
            is TranscriptionError.AudioTooShort -> ErrorInfo(
                title = "Recording Too Short",
                message = "Please record for at least 1 second for better transcription accuracy.",
                actionText = "Try Again",
                isRetryable = true,
                severity = ErrorSeverity.WARNING
            )
            
            is TranscriptionError.AudioTooLarge -> ErrorInfo(
                title = "Recording Too Large",
                message = "Audio file exceeds 25MB limit. Try recording shorter clips.",
                actionText = "Try Again",
                isRetryable = true,
                severity = ErrorSeverity.ERROR
            )
            
            is TranscriptionError.StorageError -> ErrorInfo(
                title = "Storage Error",
                message = "Not enough storage space available. Please free up space and try again.",
                actionText = "Retry",
                isRetryable = true,
                retryDelay = 1000L,
                severity = ErrorSeverity.ERROR
            )
            
            is TranscriptionError.TextInsertionFailed -> ErrorInfo(
                title = "Text Insertion Failed",
                message = "Transcription completed but couldn't insert text automatically. You can copy it manually.",
                actionText = "Copy Text",
                severity = ErrorSeverity.WARNING
            )
            
            is TranscriptionError.RecordingInProgress -> ErrorInfo(
                title = "Recording Active",
                message = "Please stop the current recording before starting a new one.",
                actionText = "Stop Recording",
                severity = ErrorSeverity.WARNING
            )
            
            else -> ErrorInfo(
                title = "Transcription Error",
                message = error.message ?: "An unexpected error occurred during transcription.",
                actionText = "Retry",
                isRetryable = true,
                retryDelay = 2000L,
                severity = ErrorSeverity.ERROR
            )
        }
    }
    
    private fun classifyAudioError(error: AudioRecordingError): ErrorInfo {
        return when (error) {
            is AudioRecordingError.PermissionDenied -> ErrorInfo(
                title = "Microphone Permission Required",
                message = "WhisperTop needs microphone access to record audio for transcription.",
                actionText = "Grant Permission",
                severity = ErrorSeverity.CRITICAL
            )
            
            is AudioRecordingError.DeviceUnavailable -> ErrorInfo(
                title = "Microphone Unavailable",
                message = "Microphone is being used by another app. Please close other audio apps and try again.",
                actionText = "Retry",
                isRetryable = true,
                retryDelay = 2000L,
                severity = ErrorSeverity.ERROR
            )
            
            is AudioRecordingError.ConfigurationError -> ErrorInfo(
                title = "Audio Configuration Error",
                message = "Failed to configure audio recording. This may be a device compatibility issue.",
                actionText = "Retry",
                isRetryable = true,
                retryDelay = 1000L,
                severity = ErrorSeverity.ERROR
            )
            
            is AudioRecordingError.IOError -> ErrorInfo(
                title = "Audio I/O Error",
                message = "Error reading or writing audio data. Check storage space and try again.",
                actionText = "Retry",
                isRetryable = true,
                retryDelay = 1000L,
                severity = ErrorSeverity.ERROR
            )
            
            is AudioRecordingError.Unknown -> ErrorInfo(
                title = "Recording Error",
                message = "An unexpected audio recording error occurred. Please try again.",
                actionText = "Retry",
                isRetryable = true,
                retryDelay = 2000L,
                severity = ErrorSeverity.ERROR
            )
        }
    }
    
    private fun classifyOpenAIError(error: OpenAIException): ErrorInfo {
        return when (error) {
            is OpenAIException.AuthenticationException -> ErrorInfo(
                title = "API Authentication Failed",
                message = "Invalid OpenAI API key. Please check your API key in settings.",
                actionText = "Update API Key",
                severity = ErrorSeverity.CRITICAL
            )
            
            is OpenAIException.RateLimitException -> ErrorInfo(
                title = "Rate Limit Exceeded",
                message = "API rate limit reached. Please wait before making more requests.",
                actionText = "Retry",
                isRetryable = true,
                retryDelay = 15000L,
                severity = ErrorSeverity.WARNING
            )
            
            is OpenAIException.NetworkException -> ErrorInfo(
                title = "Network Error",
                message = "Failed to connect to OpenAI servers. Check your internet connection.",
                actionText = "Retry",
                isRetryable = true,
                retryDelay = 3000L,
                severity = ErrorSeverity.ERROR
            )
            
            is OpenAIException.ServerException -> ErrorInfo(
                title = "Server Error",
                message = "OpenAI servers are experiencing issues. Please try again later.",
                actionText = "Retry",
                isRetryable = true,
                retryDelay = 5000L,
                severity = ErrorSeverity.ERROR
            )
            
            is OpenAIException.InvalidRequestException -> ErrorInfo(
                title = "Invalid Request",
                message = "The audio format or request is not supported. Try recording again.",
                actionText = "Retry",
                isRetryable = true,
                retryDelay = 1000L,
                severity = ErrorSeverity.ERROR
            )
            
            is OpenAIException.UnknownException -> ErrorInfo(
                title = "API Error",
                message = "An unexpected API error occurred. Please try again.",
                actionText = "Retry",
                isRetryable = true,
                retryDelay = 3000L,
                severity = ErrorSeverity.ERROR
            )
        }
    }
    
    private fun classifyPermissionError(error: PlatformSecurityException): ErrorInfo {
        return ErrorInfo(
            title = "Permission Required",
            message = "WhisperTop needs additional permissions to function properly.",
            actionText = "Grant Permission",
            severity = ErrorSeverity.CRITICAL
        )
    }
    
    private fun classifySecurityError(error: SecurityException): ErrorInfo {
        return ErrorInfo(
            title = "Permission Required",
            message = error.message ?: "Permission denied. Please grant the required permissions.",
            actionText = "Grant Permission",
            severity = ErrorSeverity.CRITICAL
        )
    }
    
    private fun classifyGenericError(error: Throwable): ErrorInfo {
        return ErrorInfo(
            title = "Unexpected Error",
            message = error.message ?: "An unexpected error occurred. Please try again.",
            actionText = "Retry",
            isRetryable = true,
            retryDelay = 2000L,
            severity = ErrorSeverity.ERROR
        )
    }
    
    fun getCategory(error: Throwable): ErrorCategory {
        return when (error) {
            is TranscriptionError.NetworkError,
            is OpenAIException.NetworkException -> ErrorCategory.NETWORK
            
            is TranscriptionError.AuthenticationError,
            is TranscriptionError.ApiKeyMissing,
            is OpenAIException.AuthenticationException -> ErrorCategory.AUTHENTICATION
            
            is TranscriptionError.PermissionDenied,
            is AudioRecordingError.PermissionDenied,
            is PlatformSecurityException,
            is SecurityException -> ErrorCategory.PERMISSION
            
            is AudioRecordingError -> ErrorCategory.AUDIO
            
            is TranscriptionError.StorageError -> ErrorCategory.STORAGE
            
            is OpenAIException,
            is TranscriptionError.ApiError,
            is TranscriptionError.RateLimitError -> ErrorCategory.API
            
            is TranscriptionError.AccessibilityServiceNotEnabled,
            is TranscriptionError.TextInsertionFailed -> ErrorCategory.ACCESSIBILITY
            
            is TranscriptionError.ServiceNotConfigured -> ErrorCategory.CONFIGURATION
            
            else -> ErrorCategory.UNKNOWN
        }
    }
}