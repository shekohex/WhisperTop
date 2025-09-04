package me.shadykhalifa.whispertop.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.impl.WorkManagerImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


import me.shadykhalifa.whispertop.domain.usecases.TranscriptionWorkflowUseCase
import me.shadykhalifa.whispertop.domain.usecases.UserFeedbackUseCase
import me.shadykhalifa.whispertop.domain.usecases.WorkflowState
import me.shadykhalifa.whispertop.domain.usecases.ServiceInitializationUseCase
import me.shadykhalifa.whispertop.domain.usecases.PermissionManagementUseCase
import me.shadykhalifa.whispertop.domain.usecases.ServiceBindingUseCase
import me.shadykhalifa.whispertop.domain.models.ServiceConnectionStatus
import me.shadykhalifa.whispertop.domain.models.PermissionStatus
import me.shadykhalifa.whispertop.domain.models.ServiceReadinessState
import me.shadykhalifa.whispertop.presentation.models.AudioFilePresentationModel
import me.shadykhalifa.whispertop.presentation.models.RecordingStatus
import me.shadykhalifa.whispertop.presentation.models.TranscriptionDisplayModel
import me.shadykhalifa.whispertop.presentation.utils.ViewModelErrorHandler
import me.shadykhalifa.whispertop.domain.models.ErrorContext

import me.shadykhalifa.whispertop.presentation.models.toUiState
import me.shadykhalifa.whispertop.utils.Result


class AudioRecordingViewModel(
    private val transcriptionWorkflowUseCase: TranscriptionWorkflowUseCase,
    private val userFeedbackUseCase: UserFeedbackUseCase,
    private val serviceInitializationUseCase: ServiceInitializationUseCase,
    private val permissionManagementUseCase: PermissionManagementUseCase,
    private val serviceBindingUseCase: ServiceBindingUseCase,
    private val errorHandler: ViewModelErrorHandler
) : ViewModel() {
    
    companion object {
        private const val TAG = "AudioRecordingViewModel"
    }
    
    private val _uiState = MutableStateFlow(AudioRecordingUiState())
    val uiState: StateFlow<AudioRecordingUiState> = _uiState.asStateFlow()

    private val _serviceReadinessState = MutableStateFlow<ServiceReadinessState?>(null)
    val serviceReadinessState: StateFlow<ServiceReadinessState?> = _serviceReadinessState.asStateFlow()

    private val _serviceConnectionStatus = MutableStateFlow<ServiceConnectionStatus?>(null)
    val serviceConnectionStatus: StateFlow<ServiceConnectionStatus?> = _serviceConnectionStatus.asStateFlow()

    private val _permissionStatus = MutableStateFlow<PermissionStatus?>(null)
    val permissionStatus: StateFlow<PermissionStatus?> = _permissionStatus.asStateFlow()
    
    init {
        observeWorkflowState()
        initializeServices()
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
                        val errorContext = ErrorContext(
                            operationName = "transcription_workflow",
                            additionalData = mapOf(
                                "workflow_step" to "error_handling",
                                "original_error_type" to (workflowState.error::class.simpleName ?: "unknown")
                            )
                        )
                        
                        val errorInfo = errorHandler.handleErrorWithContext(
                            error = workflowState.error,
                            errorContext = errorContext,
                            showNotification = false
                        )
                        
                        userFeedbackUseCase.showFeedback(errorInfo.message, isError = true)
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

    private fun initializeServices() {
        viewModelScope.launch {
            checkServiceReadiness()
        }
    }

    suspend fun checkServiceReadiness() {
        val serviceBindingResult = serviceBindingUseCase()
        when (serviceBindingResult) {
            is Result.Success -> {
                _serviceReadinessState.value = serviceBindingResult.data
            }
            is Result.Error -> {
                Log.e(TAG, "Failed to check service readiness: ${serviceBindingResult.exception.message}")
                _serviceReadinessState.value = ServiceReadinessState(
                    serviceConnected = false,
                    permissionsGranted = false,
                    errorMessage = serviceBindingResult.exception.message
                )
            }
            is Result.Loading -> {
                // Loading state handled by UI
            }
        }
    }

    suspend fun initializeServiceConnection(): Result<ServiceConnectionStatus> {
        return serviceInitializationUseCase().also { result ->
            when (result) {
                is Result.Success -> {
                    _serviceConnectionStatus.value = result.data
                }
                is Result.Error -> {
                    Log.e(TAG, "Service initialization failed: ${result.exception.message}")
                }
                is Result.Loading -> {
                    // Loading state handled by UI
                }
            }
        }
    }

    suspend fun checkPermissions(): Result<PermissionStatus> {
        return permissionManagementUseCase().also { result ->
            when (result) {
                is Result.Success -> {
                    _permissionStatus.value = result.data
                }
                is Result.Error -> {
                    Log.e(TAG, "Permission check failed: ${result.exception.message}")
                }
                is Result.Loading -> {
                    // Loading state handled by UI
                }
            }
        }
    }

    fun isServiceReady(): Boolean {
        val readiness = _serviceReadinessState.value
        return readiness?.isReady == true
    }

    fun arePermissionsGranted(): Boolean {
        val permissionStatus = _permissionStatus.value
        return permissionStatus is PermissionStatus.AllGranted
    }

    fun isServiceConnected(): Boolean {
        val connectionStatus = _serviceConnectionStatus.value
        return connectionStatus is ServiceConnectionStatus.Connected || 
               connectionStatus is ServiceConnectionStatus.AlreadyBound
    }

    fun refreshServiceState() {
        viewModelScope.launch {
            initializeServiceConnection()
            checkPermissions()
            checkServiceReadiness()
        }
    }

    
    fun startRecording() {
        Log.d(TAG, "Starting recording via workflow...")
        viewModelScope.launch {
            // Ensure services are ready before starting recording
            if (!isServiceReady()) {
                refreshServiceState()
                if (!isServiceReady()) {
                    val readiness = _serviceReadinessState.value
                    val errorMessage = readiness?.errorMessage ?: "Services not ready"
                    userFeedbackUseCase.showFeedback(errorMessage, isError = true)
                    return@launch
                }
            }
            
            val result = transcriptionWorkflowUseCase.startRecording()
            when (result) {
                is Result.Success -> {
                    Log.d(TAG, "Recording started successfully")
                }
                is Result.Error -> {
                    Log.e(TAG, "Failed to start recording: ${result.exception.message}")
                }
                is Result.Loading -> {
                    Log.d(TAG, "Recording start is loading...")
                }
            }
        }
    }
    
    fun stopRecording() {
        Log.d(TAG, "Stopping recording via workflow...")
        viewModelScope.launch {
            val result = transcriptionWorkflowUseCase.stopRecording()
            when (result) {
                is Result.Success -> {
                    Log.d(TAG, "Recording stopped successfully")
                }
                is Result.Error -> {
                    Log.e(TAG, "Failed to stop recording: ${result.exception.message}")
                }
                is Result.Loading -> {
                    Log.d(TAG, "Recording stop is loading...")
                }
            }
        }
    }
    
    fun cancelRecording() {
        Log.d(TAG, "Canceling recording via workflow...")
        transcriptionWorkflowUseCase.cancelRecording()
    }
    
    fun retryFromError() {
        Log.d(TAG, "Retrying from error via workflow...")
        // Refresh service state before retrying
        refreshServiceState()
        transcriptionWorkflowUseCase.retryFromError()
    }
    
    fun resetToIdle() {
        Log.d(TAG, "Manually resetting to idle state")
        transcriptionWorkflowUseCase.resetToIdle()
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