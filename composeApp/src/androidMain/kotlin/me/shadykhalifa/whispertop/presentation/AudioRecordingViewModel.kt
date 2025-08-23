package me.shadykhalifa.whispertop.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


import me.shadykhalifa.whispertop.domain.usecases.TranscriptionWorkflowUseCase
import me.shadykhalifa.whispertop.domain.usecases.UserFeedbackUseCase
import me.shadykhalifa.whispertop.domain.usecases.WorkflowState
import me.shadykhalifa.whispertop.presentation.models.AudioFilePresentationModel
import me.shadykhalifa.whispertop.presentation.models.RecordingStatus
import me.shadykhalifa.whispertop.presentation.models.TranscriptionDisplayModel

import me.shadykhalifa.whispertop.presentation.models.toUiState


class AudioRecordingViewModel(
    private val transcriptionWorkflowUseCase: TranscriptionWorkflowUseCase,
    private val userFeedbackUseCase: UserFeedbackUseCase
) : ViewModel() {
    
    companion object {
        private const val TAG = "AudioRecordingViewModel"
    }
    
    private val _uiState = MutableStateFlow(AudioRecordingUiState())
    val uiState: StateFlow<AudioRecordingUiState> = _uiState.asStateFlow()

    
    init {
        observeWorkflowState()
    }

    
    private fun observeWorkflowState() {
        viewModelScope.launch {
            transcriptionWorkflowUseCase.workflowState.collect { workflowState ->
                _uiState.value = workflowState.toUiState(_uiState.value)
                
                // Handle feedback notifications based on state
                when (workflowState) {
                    is WorkflowState.Success -> {
                        val previewText = if (workflowState.transcription.length > 47) {
                            "${workflowState.transcription.take(47)}..."
                        } else {
                            workflowState.transcription
                        }
                        
                        val message = if (workflowState.textInserted) {
                            "Text inserted: $previewText"
                        } else {
                            "Transcribed: $previewText (insertion failed)"
                        }
                        userFeedbackUseCase.showFeedback(message)
                    }
                    is WorkflowState.Error -> {
                        val errorMsg = when {
                            workflowState.error.message?.contains("network", ignoreCase = true) == true -> 
                                "Network error - check connection"
                            workflowState.error.message?.contains("api key", ignoreCase = true) == true -> 
                                "API key issue - check settings"
                            workflowState.error.message?.contains("rate limit", ignoreCase = true) == true -> 
                                "Rate limited - try again later"
                            workflowState.error.message?.contains("authentication", ignoreCase = true) == true -> 
                                "Authentication failed"
                            else -> "Transcription failed"
                        }
                        userFeedbackUseCase.showFeedback(errorMsg, isError = true)
                    }
                    is WorkflowState.Processing -> {
                        if (workflowState.progress == 0f) {
                            userFeedbackUseCase.showFeedback("Recording complete, transcribing...")
                        }
                    }
                    else -> { /* No feedback for other states */ }
                }
            }
        }
    }


    

    
    fun startRecording() {
        Log.d(TAG, "Starting recording via workflow...")
        viewModelScope.launch {
            transcriptionWorkflowUseCase.startRecording()
        }
    }
    
    fun stopRecording() {
        Log.d(TAG, "Stopping recording via workflow...")
        viewModelScope.launch {
            transcriptionWorkflowUseCase.stopRecording()
        }
    }
    
    fun cancelRecording() {
        Log.d(TAG, "Canceling recording via workflow...")
        transcriptionWorkflowUseCase.cancelRecording()
    }
    
    fun retryFromError() {
        Log.d(TAG, "Retrying from error via workflow...")
        transcriptionWorkflowUseCase.retryFromError()
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    

    

    
    override fun onCleared() {
        super.onCleared()
        transcriptionWorkflowUseCase.cleanup()
    }
}

data class AudioRecordingUiState(
    val status: RecordingStatus = RecordingStatus.Idle,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val lastRecording: AudioFilePresentationModel? = null,
    val transcription: TranscriptionDisplayModel? = null
)