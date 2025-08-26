package me.shadykhalifa.whispertop.domain.usecases

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.shadykhalifa.whispertop.domain.managers.RecordingManager
import me.shadykhalifa.whispertop.domain.models.RecordingState
import me.shadykhalifa.whispertop.domain.models.TranscriptionError
import me.shadykhalifa.whispertop.domain.models.PermissionStatus
import me.shadykhalifa.whispertop.domain.repositories.SettingsRepository
import me.shadykhalifa.whispertop.domain.repositories.TranscriptionRepository
import me.shadykhalifa.whispertop.domain.repositories.TranscriptionHistoryRepository
import me.shadykhalifa.whispertop.domain.repositories.SessionMetricsRepository
import me.shadykhalifa.whispertop.domain.services.TextInsertionService
import me.shadykhalifa.whispertop.domain.services.ToastService
import me.shadykhalifa.whispertop.domain.services.RetryService
import me.shadykhalifa.whispertop.domain.services.ErrorLoggingService
import me.shadykhalifa.whispertop.domain.services.ConnectionStatusService
import me.shadykhalifa.whispertop.domain.models.ErrorNotificationService
import me.shadykhalifa.whispertop.domain.models.ErrorClassifier
import me.shadykhalifa.whispertop.domain.models.TranscriptionHistoryItem
import me.shadykhalifa.whispertop.domain.models.AudioFile
import me.shadykhalifa.whispertop.domain.services.RetryPredicates
import me.shadykhalifa.whispertop.utils.Result
import kotlinx.datetime.Clock
import me.shadykhalifa.whispertop.utils.UuidGenerator

private fun Boolean?.orFalse(): Boolean = this ?: false

sealed class WorkflowState {
    data object Idle : WorkflowState()
    data object ServiceReady : WorkflowState()
    data class PermissionDenied(val deniedPermissions: List<String>) : WorkflowState()
    data object Recording : WorkflowState()
    data class Processing(val progress: Float = 0f) : WorkflowState()
    data object InsertingText : WorkflowState()
    data class Success(val transcription: String, val textInserted: Boolean) : WorkflowState()
    data class Error(val error: TranscriptionError, val retryable: Boolean = true) : WorkflowState()
}

class TranscriptionWorkflowUseCase(
    private val recordingManager: RecordingManager,
    private val transcriptionRepository: TranscriptionRepository,
    private val transcriptionHistoryRepository: TranscriptionHistoryRepository,
    private val sessionMetricsRepository: SessionMetricsRepository,
    private val settingsRepository: SettingsRepository,
    private val textInsertionService: TextInsertionService,
    private val toastService: ToastService,
    private val retryService: RetryService,
    private val errorLoggingService: ErrorLoggingService,
    private val connectionStatusService: ConnectionStatusService,
    private val errorNotificationService: ErrorNotificationService,
    private val serviceManagementUseCase: ServiceManagementUseCase
) {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    private val _workflowState = MutableStateFlow<WorkflowState>(WorkflowState.Idle)
    val workflowState: StateFlow<WorkflowState> = _workflowState.asStateFlow()
    
    val recordingState: StateFlow<RecordingState> = recordingManager.recordingState
    
    init {
        scope.launch {
            // Initialize services and check readiness
            initializeServices()
            
            recordingManager.recordingState.collect { recordingState ->
                handleRecordingStateChange(recordingState)
            }
        }
        
        // Start connection monitoring
        connectionStatusService.startMonitoring(scope)
    }
    
    private suspend fun initializeServices() {
        try {
            // Bind services first
            when (val bindResult = serviceManagementUseCase.bindServices()) {
                is Result.Success -> {
                    // Check permissions after services are bound
                    when (val permissionResult = serviceManagementUseCase.checkPermissions()) {
                        is Result.Success -> {
                            when (permissionResult.data) {
                                is PermissionStatus.AllGranted -> {
                                    _workflowState.value = WorkflowState.ServiceReady
                                }
                                is PermissionStatus.SomeDenied -> {
                                    _workflowState.value = WorkflowState.PermissionDenied(
                                        permissionResult.data.deniedPermissions
                                    )
                                }
                                is PermissionStatus.ShowRationale -> {
                                    _workflowState.value = WorkflowState.PermissionDenied(
                                        permissionResult.data.permissions
                                    )
                                }
                            }
                        }
                        is Result.Error -> {
                            _workflowState.value = WorkflowState.Error(
                                TranscriptionError.fromThrowable(permissionResult.exception),
                                retryable = true
                            )
                        }
                        is Result.Loading -> {
                            // Stay in current state during permission loading
                        }
                    }
                }
                is Result.Error -> {
                    _workflowState.value = WorkflowState.Error(
                        TranscriptionError.ServiceNotConfigured(),
                        retryable = true
                    )
                }
                is Result.Loading -> {
                    // Stay in current state during service binding
                }
            }
        } catch (e: Exception) {
            _workflowState.value = WorkflowState.Error(
                TranscriptionError.fromThrowable(e),
                retryable = true
            )
        }
    }
    
    suspend fun startRecording(): Result<Unit> {
        return try {
            retryService.withRetry(
                maxRetries = 2,
                initialDelay = 1000L,
                retryPredicate = RetryPredicates.nonRetryableErrors
            ) {
                // Check service readiness first
                val serviceReadiness = serviceManagementUseCase.getServiceReadiness()
                when (serviceReadiness) {
                    is Result.Success -> {
                        if (!serviceReadiness.data.serviceConnected) {
                            val error = TranscriptionError.ServiceNotConfigured()
                            errorLoggingService.logError(error, mapOf("context" to "startRecording"))
                            errorNotificationService.showError(error, "startRecording")
                            _workflowState.value = WorkflowState.Error(error, retryable = true)
                            throw error
                        }
                        if (!serviceReadiness.data.permissionsGranted) {
                            val error = TranscriptionError.AccessibilityServiceNotEnabled()
                            errorLoggingService.logError(error, mapOf("context" to "startRecording"))
                            errorNotificationService.showError(error, "startRecording")
                            _workflowState.value = WorkflowState.PermissionDenied(emptyList())
                            throw error
                        }
                    }
                    is Result.Error -> {
                        val error = TranscriptionError.ServiceNotConfigured()
                        errorLoggingService.logError(error, mapOf("context" to "startRecording"))
                        errorNotificationService.showError(error, "startRecording")
                        _workflowState.value = WorkflowState.Error(error, retryable = true)
                        throw error
                    }
                    is Result.Loading -> {
                        val error = TranscriptionError.ServiceNotConfigured()
                        errorLoggingService.logError(error, mapOf("context" to "startRecording"))
                        errorNotificationService.showError(error, "startRecording")
                        _workflowState.value = WorkflowState.Error(error, retryable = true)
                        throw error
                    }
                }
                
                val settings = settingsRepository.getSettings()
                
                val error = when {
                    settings.apiKey.isBlank() -> TranscriptionError.ApiKeyMissing()
                    !transcriptionRepository.isConfigured() -> TranscriptionError.ServiceNotConfigured()
                    !textInsertionService.isServiceAvailable() -> TranscriptionError.AccessibilityServiceNotEnabled()
                    else -> null
                }
                
                if (error != null) {
                    errorLoggingService.logError(error, mapOf("context" to "startRecording"))
                    errorNotificationService.showError(error, "startRecording")
                    _workflowState.value = WorkflowState.Error(error, retryable = true)
                    throw error
                } else {
                    recordingManager.startRecording()
                }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            val error = TranscriptionError.fromThrowable(e)
            val errorInfo = ErrorClassifier.classifyError(error)
            
            errorLoggingService.logError(error, mapOf("context" to "startRecording"))
            errorNotificationService.showError(error, "startRecording")
            
            _workflowState.value = WorkflowState.Error(error, errorInfo.isRetryable)
            Result.Error(error)
        }
    }
    
    suspend fun stopRecording(): Result<Unit> {
        return try {
            retryService.withRetry(
                maxRetries = 1,
                initialDelay = 500L,
                retryPredicate = RetryPredicates.transientAudioErrors
            ) {
                recordingManager.stopRecording()
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            val error = TranscriptionError.fromThrowable(e)
            val errorInfo = ErrorClassifier.classifyError(error)
            
            errorLoggingService.logError(error, mapOf("context" to "stopRecording"))
            errorNotificationService.showError(error, "stopRecording")
            
            _workflowState.value = WorkflowState.Error(error, errorInfo.isRetryable)
            Result.Error(error)
        }
    }
    
    fun cancelRecording() {
        recordingManager.cancelRecording()
        _workflowState.value = WorkflowState.Idle
    }
    
    fun retryFromError() {
        scope.launch {
            recordingManager.retryFromError()
            _workflowState.value = WorkflowState.Idle
            // Re-initialize services in case the error was due to service initialization
            initializeServices()
        }
    }
    
    fun resetToIdle() {
        recordingManager.resetToIdle()
        _workflowState.value = WorkflowState.Idle
    }
    
    private suspend fun handleRecordingStateChange(state: RecordingState) {
        when (state) {
            is RecordingState.Idle -> {
                _workflowState.value = WorkflowState.Idle
            }
            is RecordingState.Recording -> {
                _workflowState.value = WorkflowState.Recording
            }
            is RecordingState.Processing -> {
                _workflowState.value = WorkflowState.Processing(state.progress)
            }
            is RecordingState.Success -> {
                handleTranscriptionSuccess(state.transcription)
            }
            is RecordingState.Error -> {
                val error = TranscriptionError.fromThrowable(state.throwable)
                
                errorLoggingService.logError(state.throwable, mapOf(
                    "context" to "handleRecordingStateChange",
                    "retryable" to state.retryable.toString()
                ))
                errorNotificationService.showError(state.throwable, "recording")
                
                _workflowState.value = WorkflowState.Error(error, state.retryable)
            }
        }
    }
    
    private suspend fun handleTranscriptionSuccess(transcription: String) {
        // Check for empty transcription and show toast
        if (transcription.isBlank()) {
            toastService.showToast("No speech detected in recording", isLong = true)
            _workflowState.value = WorkflowState.Success(
                transcription = transcription,
                textInserted = false
            )
            return
        }
        
        _workflowState.value = WorkflowState.InsertingText
        
        // Persist transcription history to database
        val transcriptionHistoryId = try {
            persistTranscriptionHistory(transcription)
        } catch (e: Exception) {
            errorLoggingService.logWarning("Failed to persist transcription history", mapOf(
                "error" to (e.message ?: "Unknown error"),
                "transcription_length" to transcription.length.toString()
            ))
            null
        }
        
        try {
            val textInserted = retryService.withRetry(
                maxRetries = 2,
                initialDelay = 500L,
                retryPredicate = { e ->
                    // Retry text insertion failures that might be temporary
                    !e.message?.contains("permission", ignoreCase = true).orFalse() &&
                    !e.message?.contains("accessibility service", ignoreCase = true).orFalse()
                }
            ) {
                textInsertionService.insertText(transcription)
            }
            
            _workflowState.value = WorkflowState.Success(
                transcription = transcription,
                textInserted = textInserted
            )
            
            if (!textInserted) {
                val error = TranscriptionError.TextInsertionFailed(transcription)
                errorLoggingService.logWarning("Text insertion failed", mapOf(
                    "transcription_length" to transcription.length.toString(),
                    "context" to "handleTranscriptionSuccess"
                ))
                errorNotificationService.showError(error, "textInsertion")
                _workflowState.value = WorkflowState.Error(error, retryable = false)
            }
            
        } catch (e: Exception) {
            val error = TranscriptionError.fromThrowable(e)
            val errorInfo = ErrorClassifier.classifyError(error)
            
            errorLoggingService.logError(error, mapOf(
                "context" to "handleTranscriptionSuccess",
                "transcription_length" to transcription.length.toString()
            ))
            errorNotificationService.showError(error, "textInsertion")
            
            _workflowState.value = WorkflowState.Error(error, errorInfo.isRetryable)
        }
    }
    
    private suspend fun persistTranscriptionHistory(transcription: String): String? {
        // Get current recording session info if available
        val currentRecordingState = recordingState.value
        val settings = settingsRepository.getSettings()
        
        // Calculate audio metrics
        val audioFile = when (currentRecordingState) {
            is RecordingState.Success -> currentRecordingState.audioFile
            else -> null
        }
        
        val audioDurationMs = audioFile?.duration ?: 0L
        val audioDurationSeconds = audioDurationMs / 1000f
        val wordCount = countWords(transcription)
        val speakingRate = calculateSpeakingRate(wordCount, audioDurationSeconds)
        
        val transcriptionHistoryItem = TranscriptionHistoryItem(
            id = UuidGenerator.randomUUID(),
            text = transcription,
            timestamp = Clock.System.now().toEpochMilliseconds(),
            duration = audioDurationSeconds,
            audioFilePath = audioFile?.path,
            confidence = null, // OpenAI doesn't provide confidence scores
            customPrompt = settings.customPrompt?.takeIf { it.isNotBlank() },
            temperature = settings.temperature,
            language = settings.language?.takeIf { it.isNotBlank() },
            model = settings.selectedModel
        )
        
        return when (val result = transcriptionHistoryRepository.saveTranscription(
            text = transcriptionHistoryItem.text,
            duration = transcriptionHistoryItem.duration,
            audioFilePath = transcriptionHistoryItem.audioFilePath,
            confidence = transcriptionHistoryItem.confidence,
            customPrompt = transcriptionHistoryItem.customPrompt,
            temperature = transcriptionHistoryItem.temperature,
            language = transcriptionHistoryItem.language,
            model = transcriptionHistoryItem.model
        )) {
            is Result.Success -> {
                // Log success for analytics
                errorLoggingService.logWarning("Transcription history saved successfully", mapOf(
                    "transcription_id" to result.data,
                    "word_count" to wordCount.toString(),
                    "speaking_rate" to speakingRate.toString(),
                    "audio_duration" to audioDurationSeconds.toString()
                ))
                result.data
            }
            is Result.Error -> {
                errorLoggingService.logError(result.exception, mapOf(
                    "context" to "persistTranscriptionHistory",
                    "transcription_length" to transcription.length.toString()
                ))
                throw result.exception
            }
            is Result.Loading -> {
                // This shouldn't happen for database operations, but handle it gracefully
                errorLoggingService.logWarning("Unexpected loading state in persistTranscriptionHistory", mapOf(
                    "context" to "persistTranscriptionHistory"
                ))
                null
            }
        }
    }
    
    private fun countWords(text: String): Int {
        if (text.isBlank()) return 0
        return text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size
    }
    
    private fun calculateSpeakingRate(wordCount: Int, audioDurationSeconds: Float): Double {
        if (audioDurationSeconds <= 0) return 0.0
        return (wordCount.toDouble() / (audioDurationSeconds / 60.0))
    }
    
    fun cleanup() {
        connectionStatusService.stopMonitoring()
        recordingManager.cleanup()
        serviceManagementUseCase.cleanup()
        scope.cancel()
    }
}