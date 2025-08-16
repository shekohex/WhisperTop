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
import me.shadykhalifa.whispertop.utils.Result

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
    private val textInsertionService: TextInsertionService
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
    }
    
    suspend fun startRecording(): Result<Unit> {
        return try {
            val settings = settingsRepository.getSettings()
            
            val error = when {
                settings.apiKey.isBlank() -> TranscriptionError.ApiKeyMissing()
                !transcriptionRepository.isConfigured() -> TranscriptionError.ServiceNotConfigured()
                !textInsertionService.isServiceAvailable() -> TranscriptionError.AccessibilityServiceNotEnabled()
                else -> null
            }
            
            if (error != null) {
                _workflowState.value = WorkflowState.Error(error, retryable = true)
                Result.Error(error)
            } else {
                recordingManager.startRecording()
                Result.Success(Unit)
            }
        } catch (e: Exception) {
            val error = TranscriptionError.fromThrowable(e)
            _workflowState.value = WorkflowState.Error(error)
            Result.Error(error)
        }
    }
    
    suspend fun stopRecording(): Result<Unit> {
        return try {
            recordingManager.stopRecording()
            Result.Success(Unit)
        } catch (e: Exception) {
            val error = TranscriptionError.fromThrowable(e)
            _workflowState.value = WorkflowState.Error(error)
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
                _workflowState.value = WorkflowState.Error(error, state.retryable)
            }
        }
    }
    
    private suspend fun handleTranscriptionSuccess(transcription: String) {
        _workflowState.value = WorkflowState.InsertingText
        
        try {
            val textInserted = textInsertionService.insertText(transcription)
            _workflowState.value = WorkflowState.Success(
                transcription = transcription,
                textInserted = textInserted
            )
            
            if (!textInserted) {
                val error = TranscriptionError.TextInsertionFailed(transcription)
                _workflowState.value = WorkflowState.Error(error, retryable = false)
            }
            
        } catch (e: Exception) {
            val error = TranscriptionError.fromThrowable(e)
            _workflowState.value = WorkflowState.Error(error, retryable = true)
        }
    }
    
    fun cleanup() {
        recordingManager.cleanup()
        scope.cancel()
    }
}