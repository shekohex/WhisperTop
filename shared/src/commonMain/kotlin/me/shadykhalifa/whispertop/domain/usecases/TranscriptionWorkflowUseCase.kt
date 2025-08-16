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
import me.shadykhalifa.whispertop.domain.repositories.SettingsRepository
import me.shadykhalifa.whispertop.domain.repositories.TranscriptionRepository
import me.shadykhalifa.whispertop.domain.services.TextInsertionService
import me.shadykhalifa.whispertop.domain.services.RetryService
import me.shadykhalifa.whispertop.domain.services.ErrorLoggingService
import me.shadykhalifa.whispertop.domain.services.ConnectionStatusService
import me.shadykhalifa.whispertop.domain.models.ErrorNotificationService
import me.shadykhalifa.whispertop.domain.models.ErrorClassifier
import me.shadykhalifa.whispertop.domain.services.RetryPredicates
import me.shadykhalifa.whispertop.utils.Result

private fun Boolean?.orFalse(): Boolean = this ?: false

sealed class WorkflowState {
    data object Idle : WorkflowState()
    data object Recording : WorkflowState()
    data class Processing(val progress: Float = 0f) : WorkflowState()
    data object InsertingText : WorkflowState()
    data class Success(val transcription: String, val textInserted: Boolean) : WorkflowState()
    data class Error(val error: TranscriptionError, val retryable: Boolean = true) : WorkflowState()
}

class TranscriptionWorkflowUseCase(
    private val recordingManager: RecordingManager,
    private val transcriptionRepository: TranscriptionRepository,
    private val settingsRepository: SettingsRepository,
    private val textInsertionService: TextInsertionService,
    private val retryService: RetryService,
    private val errorLoggingService: ErrorLoggingService,
    private val connectionStatusService: ConnectionStatusService,
    private val errorNotificationService: ErrorNotificationService
) {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    private val _workflowState = MutableStateFlow<WorkflowState>(WorkflowState.Idle)
    val workflowState: StateFlow<WorkflowState> = _workflowState.asStateFlow()
    
    val recordingState: StateFlow<RecordingState> = recordingManager.recordingState
    
    init {
        scope.launch {
            recordingManager.recordingState.collect { recordingState ->
                handleRecordingStateChange(recordingState)
            }
        }
        
        // Start connection monitoring
        connectionStatusService.startMonitoring(scope)
    }
    
    suspend fun startRecording(): Result<Unit> {
        return try {
            retryService.withRetry(
                maxRetries = 2,
                initialDelay = 1000L,
                retryPredicate = RetryPredicates.nonRetryableErrors
            ) {
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
        recordingManager.retryFromError()
        _workflowState.value = WorkflowState.Idle
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
        _workflowState.value = WorkflowState.InsertingText
        
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
    
    fun cleanup() {
        connectionStatusService.stopMonitoring()
        recordingManager.cleanup()
        scope.cancel()
    }
}