package me.shadykhalifa.whispertop.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import me.shadykhalifa.whispertop.domain.models.AudioFile
import me.shadykhalifa.whispertop.domain.models.RecordingState
import me.shadykhalifa.whispertop.domain.models.ServiceConnectionState
import me.shadykhalifa.whispertop.domain.repositories.PermissionRepository
import me.shadykhalifa.whispertop.domain.repositories.ServiceStateRepository
import me.shadykhalifa.whispertop.domain.usecases.PermissionManagementUseCase
import me.shadykhalifa.whispertop.domain.usecases.ServiceBindingUseCase
import me.shadykhalifa.whispertop.domain.usecases.ServiceInitializationUseCase
import me.shadykhalifa.whispertop.domain.usecases.ServiceManagementUseCase
import me.shadykhalifa.whispertop.domain.usecases.TranscriptionWorkflowUseCase
import me.shadykhalifa.whispertop.domain.usecases.UserFeedbackUseCase
import me.shadykhalifa.whispertop.domain.usecases.WorkflowState
import me.shadykhalifa.whispertop.utils.Result

class AudioRecordingViewModel(
    private val serviceManagementUseCase: ServiceManagementUseCase,
    private val serviceInitializationUseCase: ServiceInitializationUseCase,
    private val permissionManagementUseCase: PermissionManagementUseCase,
    private val serviceBindingUseCase: ServiceBindingUseCase,
    private val transcriptionWorkflowUseCase: TranscriptionWorkflowUseCase,
    private val userFeedbackUseCase: UserFeedbackUseCase
) : ViewModel() {
    
    companion object {
        private const val TAG = "AudioRecordingViewModel"
    }
    
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
                serviceManagementUseCase.connectionState,
                serviceManagementUseCase.recordingState,
                permissionManagementUseCase.permissionState
            ) { connectionState, recordingState, permissionState ->
                val isServiceReady = connectionState == ServiceConnectionState.CONNECTED &&
                        permissionState
                
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
            serviceManagementUseCase.errorEvents.collect { error ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = error,
                    isLoading = false
                )
            }
        }
        
        // Observe recording completion events
        viewModelScope.launch {
            serviceManagementUseCase.recordingCompleteEvents.collect { audioFile ->
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
            serviceManagementUseCase.recordingState.collect { state ->
                if (state == ServiceStateRepository.RecordingState.RECORDING ||
                    state == ServiceStateRepository.RecordingState.PAUSED) {
                    startDurationTimer()
                } else {
                    _recordingDuration.value = 0L
                }
            }
        }
    }
    
    private fun observeWorkflowState() {
        viewModelScope.launch {
            transcriptionWorkflowUseCase.workflowState.collect { workflowState ->
                _uiState.value = mapWorkflowStateToUiState(
                    workflowState, 
                    _uiState.value
                )
                
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
            while (serviceManagementUseCase.getCurrentRecordingState() == ServiceStateRepository.RecordingState.RECORDING ||
                   serviceManagementUseCase.getCurrentRecordingState() == ServiceStateRepository.RecordingState.PAUSED) {
                _recordingDuration.value = serviceManagementUseCase.getRecordingDuration()
                kotlinx.coroutines.delay(100) // Update every 100ms
            }
        }
    }
    
    suspend fun initializeService() {
        Log.d(TAG, "initializeService: starting service initialization")
        _uiState.value = _uiState.value.copy(isLoading = true)
        
        when (val result = serviceInitializationUseCase()) {
            is Result.Success -> {
                val connectionStatus = result.data
                Log.d(TAG, "initializeService: $connectionStatus")
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
            is Result.Error -> {
                Log.e(TAG, "initializeService: error", result.exception)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Service initialization error: ${result.exception.message}"
                )
            }
            is Result.Loading -> {
                // Keep loading state
            }
        }
    }
    
    suspend fun requestPermissions() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        
        when (val result = permissionManagementUseCase()) {
            is Result.Success -> {
                val permissionStatus = result.data
                when (permissionStatus) {
                    is me.shadykhalifa.whispertop.domain.models.PermissionStatus.AllGranted -> {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    }
                    is me.shadykhalifa.whispertop.domain.models.PermissionStatus.SomeDenied -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Required permissions denied: ${permissionStatus.deniedPermissions.joinToString()}"
                        )
                    }
                    is me.shadykhalifa.whispertop.domain.models.PermissionStatus.ShowRationale -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            showPermissionRationale = true,
                            rationalePermissions = permissionStatus.permissions
                        )
                    }
                }
            }
            is Result.Error -> {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Permission request error: ${result.exception.message}"
                )
            }
            is Result.Loading -> {
                // Keep loading state
            }
        }
    }
    
    suspend fun ensureServiceReady() {
        Log.d(TAG, "ensureServiceReady: checking service readiness")
        _uiState.value = _uiState.value.copy(isLoading = true)
        
        when (val result = serviceBindingUseCase()) {
            is Result.Success -> {
                val readinessState = result.data
                Log.d(TAG, "ensureServiceReady: $readinessState")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = readinessState.errorMessage
                )
            }
            is Result.Error -> {
                Log.e(TAG, "ensureServiceReady: error", result.exception)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Service readiness check failed: ${result.exception.message}"
                )
            }
            is Result.Loading -> {
                // Keep loading state
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
    
    fun dismissPermissionRationale() {
        _uiState.value = _uiState.value.copy(
            showPermissionRationale = false,
            rationalePermissions = emptyList()
        )
    }
    

    
    override fun onCleared() {
        super.onCleared()
        serviceManagementUseCase.cleanup()
        transcriptionWorkflowUseCase.cleanup()
    }
}

data class AudioRecordingUiState(
    val serviceConnectionState: ServiceConnectionState = ServiceConnectionState.DISCONNECTED,
    val recordingState: RecordingState = RecordingState.Idle,
    val permissionState: Boolean = false,
    val isServiceReady: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val lastRecording: AudioFile? = null,
    val transcriptionResult: String? = null,
    val transcriptionLanguage: String? = null,
    val showPermissionRationale: Boolean = false,
    val rationalePermissions: List<String> = emptyList()
)