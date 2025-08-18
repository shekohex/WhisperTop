package me.shadykhalifa.whispertop.presentation

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import me.shadykhalifa.whispertop.domain.models.AudioFile
import me.shadykhalifa.whispertop.domain.usecases.TranscriptionWorkflowUseCase
import me.shadykhalifa.whispertop.domain.usecases.WorkflowState
import me.shadykhalifa.whispertop.managers.AudioServiceManager
import me.shadykhalifa.whispertop.managers.PermissionHandler
import me.shadykhalifa.whispertop.service.AudioRecordingService
import me.shadykhalifa.whispertop.utils.Result
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AudioRecordingViewModel : ViewModel(), KoinComponent {
    
    companion object {
        private const val TAG = "AudioRecordingViewModel"
    }
    
    private val audioServiceManager: AudioServiceManager by inject()
    private val permissionHandler: PermissionHandler by inject()
    private val transcriptionWorkflow: TranscriptionWorkflowUseCase by inject()
    private val context: Context by inject()
    
    private val _uiState = MutableStateFlow(AudioRecordingUiState())
    val uiState: StateFlow<AudioRecordingUiState> = _uiState.asStateFlow()
    
    private val _recordingDuration = MutableStateFlow(0L)
    val recordingDuration: StateFlow<Long> = _recordingDuration.asStateFlow()
    
    init {
        observeServiceState()
        observeServiceEvents()
        observeWorkflowState()
    }
    
    private fun observeServiceState() {
        viewModelScope.launch {
            combine(
                audioServiceManager.connectionState,
                audioServiceManager.recordingState,
                permissionHandler.permissionState
            ) { connectionState, recordingState, permissionState ->
                val isServiceReady = connectionState == AudioServiceManager.ServiceConnectionState.CONNECTED &&
                        permissionState == PermissionHandler.PermissionState.GRANTED
                
                Log.d(TAG, "Service state update: connection=$connectionState, recording=$recordingState, permission=$permissionState, isReady=$isServiceReady")
                
                _uiState.value = _uiState.value.copy(
                    serviceConnectionState = connectionState,
                    recordingState = recordingState,
                    permissionState = permissionState,
                    isServiceReady = isServiceReady
                )
            }.collect { }
        }
    }
    
    private fun observeServiceEvents() {
        // Observe error events
        viewModelScope.launch {
            audioServiceManager.errorEvents.collect { error ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = error,
                    isLoading = false
                )
            }
        }
        
        // Observe recording completion events
        viewModelScope.launch {
            audioServiceManager.recordingCompleteEvents.collect { audioFile ->
                Log.d(TAG, "Recording completed: ${audioFile?.path}, size: ${audioFile?.let { java.io.File(it.path).length() }} bytes")
                
                _uiState.value = _uiState.value.copy(
                    lastRecording = audioFile,
                    isLoading = true // Keep loading while transcribing
                )
                
                // Recording completion is now handled by the workflow
                audioFile?.let { file ->
                    Log.d(TAG, "Recording completed: ${file.path}, workflow will handle transcription")
                    _uiState.value = _uiState.value.copy(
                        lastRecording = file
                    )
                } ?: run {
                    Log.w(TAG, "Recording completed but no audio file received")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Recording completed but no audio file was created"
                    )
                }
            }
        }
        
        // Update recording duration
        viewModelScope.launch {
            audioServiceManager.recordingState.collect { state ->
                if (state == AudioRecordingService.RecordingState.RECORDING ||
                    state == AudioRecordingService.RecordingState.PAUSED) {
                    startDurationTimer()
                } else {
                    _recordingDuration.value = 0L
                }
            }
        }
    }
    
    private fun observeWorkflowState() {
        viewModelScope.launch {
            transcriptionWorkflow.workflowState.collect { workflowState ->
                _uiState.value = mapWorkflowStateToUiState(
                    workflowState, 
                    _uiState.value
                )
                
                // Handle toast notifications based on state
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
                        showToast(message)
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
                        showToast(errorMsg, isError = true)
                    }
                    is WorkflowState.Processing -> {
                        if (workflowState.progress == 0f) {
                            showToast("Recording complete, transcribing...")
                        }
                    }
                    else -> { /* No toast for other states */ }
                }
            }
        }
    }
    
    private fun mapWorkflowStateToUiState(
        workflowState: WorkflowState,
        currentUiState: AudioRecordingUiState
    ): AudioRecordingUiState {
        return when (workflowState) {
            is WorkflowState.Idle -> currentUiState.copy(
                isLoading = false,
                errorMessage = null
            )
            is WorkflowState.Recording -> currentUiState.copy(
                isLoading = false
            )
            is WorkflowState.Processing -> currentUiState.copy(
                isLoading = true
            )
            is WorkflowState.InsertingText -> currentUiState.copy(
                isLoading = true
            )
            is WorkflowState.Success -> currentUiState.copy(
                isLoading = false,
                transcriptionResult = workflowState.transcription,
                errorMessage = null
            )
            is WorkflowState.Error -> currentUiState.copy(
                isLoading = false,
                errorMessage = workflowState.error.message
            )
        }
    }
    
    private fun startDurationTimer() {
        viewModelScope.launch {
            while (audioServiceManager.getCurrentRecordingState() == AudioRecordingService.RecordingState.RECORDING ||
                   audioServiceManager.getCurrentRecordingState() == AudioRecordingService.RecordingState.PAUSED) {
                _recordingDuration.value = audioServiceManager.getRecordingDuration()
                kotlinx.coroutines.delay(100) // Update every 100ms
            }
        }
    }
    
    suspend fun initializeService() {
        Log.d(TAG, "initializeService: starting service initialization")
        _uiState.value = _uiState.value.copy(isLoading = true)
        
        when (val result = audioServiceManager.bindService()) {
            is AudioServiceManager.ServiceBindResult.SUCCESS -> {
                Log.d(TAG, "initializeService: service bound successfully")
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
            is AudioServiceManager.ServiceBindResult.ALREADY_BOUND -> {
                Log.d(TAG, "initializeService: service already bound")
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
            is AudioServiceManager.ServiceBindResult.FAILED -> {
                Log.w(TAG, "initializeService: failed to bind service")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to bind to audio service"
                )
            }
            is AudioServiceManager.ServiceBindResult.ERROR -> {
                Log.e(TAG, "initializeService: service binding error", result.exception)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Service binding error: ${result.exception.message}"
                )
            }
        }
    }
    
    suspend fun requestPermissions() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        
        when (val result = permissionHandler.requestAllPermissions()) {
            is PermissionHandler.PermissionResult.GRANTED -> {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
            is PermissionHandler.PermissionResult.DENIED -> {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Required permissions denied: ${result.deniedPermissions.joinToString()}"
                )
            }
            is PermissionHandler.PermissionResult.SHOW_RATIONALE -> {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    showPermissionRationale = true,
                    rationalePermissions = result.permissions
                )
            }
        }
    }
    
    fun startRecording() {
        Log.d(TAG, "Starting recording via workflow...")
        viewModelScope.launch {
            transcriptionWorkflow.startRecording()
        }
    }
    
    fun stopRecording() {
        Log.d(TAG, "Stopping recording via workflow...")
        viewModelScope.launch {
            transcriptionWorkflow.stopRecording()
        }
    }
    
    fun cancelRecording() {
        Log.d(TAG, "Canceling recording via workflow...")
        transcriptionWorkflow.cancelRecording()
    }
    
    fun retryFromError() {
        Log.d(TAG, "Retrying from error via workflow...")
        transcriptionWorkflow.retryFromError()
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    fun dismissPermissionRationale() {
        _uiState.value = _uiState.value.copy(
            showPermissionRationale = false,
            rationalePermissions = emptyList()
        )
    }
    

    
    private fun showToast(message: String, isError: Boolean = false) {
        val duration = if (isError) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
        Toast.makeText(context, message, duration).show()
    }
    
    override fun onCleared() {
        super.onCleared()
        audioServiceManager.cleanup()
        transcriptionWorkflow.cleanup()
    }
}

data class AudioRecordingUiState(
    val serviceConnectionState: AudioServiceManager.ServiceConnectionState = AudioServiceManager.ServiceConnectionState.DISCONNECTED,
    val recordingState: AudioRecordingService.RecordingState = AudioRecordingService.RecordingState.IDLE,
    val permissionState: PermissionHandler.PermissionState = PermissionHandler.PermissionState.UNKNOWN,
    val isServiceReady: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val lastRecording: AudioFile? = null,
    val transcriptionResult: String? = null,
    val transcriptionLanguage: String? = null,
    val showPermissionRationale: Boolean = false,
    val rationalePermissions: List<String> = emptyList()
)