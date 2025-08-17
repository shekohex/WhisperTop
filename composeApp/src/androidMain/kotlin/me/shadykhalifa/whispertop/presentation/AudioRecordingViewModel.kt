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
import me.shadykhalifa.whispertop.domain.models.TranscriptionRequest
import me.shadykhalifa.whispertop.domain.usecases.TranscribeWithLanguageDetectionUseCase
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
    private val transcriptionUseCase: TranscribeWithLanguageDetectionUseCase by inject()
    private val context: Context by inject()
    
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
                
                // Start transcription immediately after recording completes
                audioFile?.let { file ->
                    showToast("Recording complete, transcribing...")
                    startTranscription(file)
                } ?: run {
                    Log.w(TAG, "Recording completed but no audio file received")
                    showToast("Recording failed - no audio file created", isError = true)
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
        val currentState = _uiState.value
        Log.d(TAG, "startRecording: isServiceReady=${currentState.isServiceReady}, connection=${currentState.serviceConnectionState}, permission=${currentState.permissionState}")
        
        if (!currentState.isServiceReady) {
            val errorMsg = "Service not ready for recording (connection=${currentState.serviceConnectionState}, permission=${currentState.permissionState})"
            Log.w(TAG, errorMsg)
            _uiState.value = _uiState.value.copy(
                errorMessage = errorMsg
            )
            return
        }
        
        Log.d(TAG, "Attempting to start recording...")
        when (val result = audioServiceManager.startRecording()) {
            is AudioServiceManager.RecordingActionResult.SUCCESS -> {
                Log.d(TAG, "Recording started successfully")
                showToast("Recording started")
            }
            is AudioServiceManager.RecordingActionResult.SERVICE_NOT_BOUND -> {
                Log.w(TAG, "Failed to start recording: service not bound")
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Audio service not connected"
                )
            }
            is AudioServiceManager.RecordingActionResult.ERROR -> {
                Log.e(TAG, "Failed to start recording", result.exception)
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
    
    private fun startTranscription(audioFile: AudioFile) {
        viewModelScope.launch {
            Log.d(TAG, "Starting transcription for audio file: ${audioFile.path}")
            
            try {
                val transcriptionRequest = TranscriptionRequest(
                    audioFile = audioFile,
                    model = "whisper-1", // Default model
                    language = null // Auto-detect language
                )
                
                Log.d(TAG, "Transcription request created: model=${transcriptionRequest.model}, file=${transcriptionRequest.audioFile.path}")
                
                when (val result = transcriptionUseCase.execute(transcriptionRequest)) {
                    is Result.Success -> {
                        Log.d(TAG, "Transcription successful: '${result.data.text}' (${result.data.text.length} chars)")
                        Log.d(TAG, "Detected language: ${result.data.language}")
                        
                        // Show success toast with transcription preview
                        val previewText = if (result.data.text.length > 50) {
                            "${result.data.text.take(47)}..."
                        } else {
                            result.data.text
                        }
                        showToast("Transcribed: $previewText")
                        
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            transcriptionResult = result.data.text,
                            transcriptionLanguage = result.data.language
                        )
                    }
                    is Result.Error -> {
                        Log.e(TAG, "Transcription failed", result.exception)
                        
                        // Show error toast with brief error message
                        val errorMsg = result.exception.message?.let { msg ->
                            when {
                                msg.contains("network", ignoreCase = true) -> "Network error - check connection"
                                msg.contains("api key", ignoreCase = true) -> "API key issue - check settings"
                                msg.contains("rate limit", ignoreCase = true) -> "Rate limited - try again later"
                                msg.contains("authentication", ignoreCase = true) -> "Authentication failed"
                                else -> "Transcription failed"
                            }
                        } ?: "Transcription failed"
                        showToast(errorMsg, isError = true)
                        
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Transcription failed: ${result.exception.message}"
                        )
                    }
                    is Result.Loading -> {
                        Log.d(TAG, "Transcription is loading...")
                        // Keep loading state as is
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during transcription", e)
                showToast("Transcription error occurred", isError = true)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Transcription error: ${e.message}"
                )
            }
        }
    }
    
    private fun showToast(message: String, isError: Boolean = false) {
        val duration = if (isError) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
        Toast.makeText(context, message, duration).show()
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
    val transcriptionResult: String? = null,
    val transcriptionLanguage: String? = null,
    val showPermissionRationale: Boolean = false,
    val rationalePermissions: List<String> = emptyList()
)