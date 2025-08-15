package me.shadykhalifa.whispertop.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import me.shadykhalifa.whispertop.domain.models.AudioFile
import me.shadykhalifa.whispertop.managers.AudioServiceManager
import me.shadykhalifa.whispertop.managers.PermissionHandler
import me.shadykhalifa.whispertop.service.AudioRecordingService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AudioRecordingViewModel : ViewModel(), KoinComponent {
    
    private val audioServiceManager: AudioServiceManager by inject()
    private val permissionHandler: PermissionHandler by inject()
    
    private val _uiState = MutableStateFlow(AudioRecordingUiState())
    val uiState: StateFlow<AudioRecordingUiState> = _uiState.asStateFlow()
    
    private val _recordingDuration = MutableStateFlow(0L)
    val recordingDuration: StateFlow<Long> = _recordingDuration.asStateFlow()
    
    init {
        observeServiceState()
        observeServiceEvents()
    }
    
    private fun observeServiceState() {
        viewModelScope.launch {
            combine(
                audioServiceManager.connectionState,
                audioServiceManager.recordingState,
                permissionHandler.permissionState
            ) { connectionState, recordingState, permissionState ->
                _uiState.value = _uiState.value.copy(
                    serviceConnectionState = connectionState,
                    recordingState = recordingState,
                    permissionState = permissionState,
                    isServiceReady = connectionState == AudioServiceManager.ServiceConnectionState.CONNECTED &&
                            permissionState == PermissionHandler.PermissionState.GRANTED
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
                _uiState.value = _uiState.value.copy(
                    lastRecording = audioFile,
                    isLoading = false
                )
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
        _uiState.value = _uiState.value.copy(isLoading = true)
        
        when (val result = audioServiceManager.bindService()) {
            is AudioServiceManager.ServiceBindResult.SUCCESS -> {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
            is AudioServiceManager.ServiceBindResult.ALREADY_BOUND -> {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
            is AudioServiceManager.ServiceBindResult.FAILED -> {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to bind to audio service"
                )
            }
            is AudioServiceManager.ServiceBindResult.ERROR -> {
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
        if (!_uiState.value.isServiceReady) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Service not ready for recording"
            )
            return
        }
        
        when (val result = audioServiceManager.startRecording()) {
            is AudioServiceManager.RecordingActionResult.SUCCESS -> {
                // Recording started successfully
            }
            is AudioServiceManager.RecordingActionResult.SERVICE_NOT_BOUND -> {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Audio service not connected"
                )
            }
            is AudioServiceManager.RecordingActionResult.ERROR -> {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to start recording: ${result.exception.message}"
                )
            }
        }
    }
    
    fun stopRecording() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        
        when (val result = audioServiceManager.stopRecording()) {
            is AudioServiceManager.RecordingActionResult.SUCCESS -> {
                // Recording will stop, completion will be handled by event observer
            }
            is AudioServiceManager.RecordingActionResult.SERVICE_NOT_BOUND -> {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Audio service not connected"
                )
            }
            is AudioServiceManager.RecordingActionResult.ERROR -> {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to stop recording: ${result.exception.message}"
                )
            }
        }
    }
    
    fun pauseRecording() {
        when (val result = audioServiceManager.pauseRecording()) {
            is AudioServiceManager.RecordingActionResult.SUCCESS -> {
                // Recording paused successfully
            }
            is AudioServiceManager.RecordingActionResult.SERVICE_NOT_BOUND -> {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Audio service not connected"
                )
            }
            is AudioServiceManager.RecordingActionResult.ERROR -> {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to pause recording: ${result.exception.message}"
                )
            }
        }
    }
    
    fun resumeRecording() {
        when (val result = audioServiceManager.resumeRecording()) {
            is AudioServiceManager.RecordingActionResult.SUCCESS -> {
                // Recording resumed successfully
            }
            is AudioServiceManager.RecordingActionResult.SERVICE_NOT_BOUND -> {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Audio service not connected"
                )
            }
            is AudioServiceManager.RecordingActionResult.ERROR -> {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to resume recording: ${result.exception.message}"
                )
            }
        }
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
        audioServiceManager.cleanup()
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
    val showPermissionRationale: Boolean = false,
    val rationalePermissions: List<String> = emptyList()
)