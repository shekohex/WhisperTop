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
import me.shadykhalifa.whispertop.domain.repositories.SettingsRepository
import me.shadykhalifa.whispertop.domain.models.PermissionStatus
import me.shadykhalifa.whispertop.domain.models.ServiceConnectionStatus
import me.shadykhalifa.whispertop.domain.models.ServiceReadinessState
import me.shadykhalifa.whispertop.domain.services.TextInsertionService
import me.shadykhalifa.whispertop.domain.usecases.PermissionManagementUseCase
import me.shadykhalifa.whispertop.domain.usecases.ServiceBindingUseCase
import me.shadykhalifa.whispertop.domain.usecases.ServiceInitializationUseCase
import me.shadykhalifa.whispertop.domain.usecases.TranscribeWithLanguageDetectionUseCase
import me.shadykhalifa.whispertop.managers.AudioServiceManager
import me.shadykhalifa.whispertop.service.AudioRecordingService
import me.shadykhalifa.whispertop.utils.Result
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AudioRecordingViewModel : ViewModel(), KoinComponent {
    
    companion object {
        private const val TAG = "AudioRecordingViewModel"
    }
    
    private val audioServiceManager: AudioServiceManager by inject()
    private val serviceInitializationUseCase: ServiceInitializationUseCase by inject()
    private val permissionManagementUseCase: PermissionManagementUseCase by inject()
    private val serviceBindingUseCase: ServiceBindingUseCase by inject()
    private val transcriptionUseCase: TranscribeWithLanguageDetectionUseCase by inject()
    private val settingsRepository: SettingsRepository by inject()
    private val textInsertionService: TextInsertionService by inject()
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
            // Only observe recording state changes from the service manager
            // Service readiness is now managed through use cases
            audioServiceManager.recordingState.collect { recordingState ->
                Log.d(TAG, "Recording state update: $recordingState")
                
                _uiState.value = _uiState.value.copy(
                    recordingState = recordingState
                )
            }
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
        
        serviceInitializationUseCase().fold(
            onSuccess = { connectionStatus ->
                Log.d(TAG, "initializeService: service initialization completed: $connectionStatus")
                
                val currentState = _uiState.value
                val updatedReadinessState = ServiceReadinessState(
                    serviceConnectionStatus = connectionStatus,
                    permissionStatus = currentState.serviceReadinessState.permissionStatus
                )
                
                _uiState.value = currentState.copy(
                    isLoading = false,
                    serviceReadinessState = updatedReadinessState,
                    isServiceReady = updatedReadinessState.isReady,
                    errorMessage = if (connectionStatus is ServiceConnectionStatus.Failed || 
                                        connectionStatus is ServiceConnectionStatus.Error) {
                        when (connectionStatus) {
                            is ServiceConnectionStatus.Failed -> "Failed to bind to audio service"
                            is ServiceConnectionStatus.Error -> connectionStatus.message
                            else -> null
                        }
                    } else null
                )
            },
            onFailure = { exception ->
                Log.e(TAG, "initializeService: use case failed", exception)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Service initialization failed: ${exception.message}"
                )
            }
        )
    }
    
    suspend fun requestPermissions() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        
        permissionManagementUseCase().fold(
            onSuccess = { permissionStatus ->
                Log.d(TAG, "requestPermissions: permission request completed: $permissionStatus")
                
                val currentState = _uiState.value
                val updatedReadinessState = ServiceReadinessState(
                    serviceConnectionStatus = currentState.serviceReadinessState.serviceConnectionStatus,
                    permissionStatus = permissionStatus
                )
                
                _uiState.value = currentState.copy(
                    isLoading = false,
                    serviceReadinessState = updatedReadinessState,
                    isServiceReady = updatedReadinessState.isReady,
                    showPermissionRationale = permissionStatus is PermissionStatus.RequiresRationale,
                    rationalePermissions = when (permissionStatus) {
                        is PermissionStatus.RequiresRationale -> permissionStatus.permissions
                        else -> emptyList()
                    },
                    errorMessage = when (permissionStatus) {
                        is PermissionStatus.Denied -> 
                            "Required permissions denied: ${permissionStatus.deniedPermissions.joinToString()}"
                        else -> null
                    }
                )
            },
            onFailure = { exception ->
                Log.e(TAG, "requestPermissions: use case failed", exception)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Permission request failed: ${exception.message}"
                )
            }
        )
    }
    
    /**
     * Coordinates both service initialization and permission requests for unified readiness.
     * This is the recommended method to use for complete setup.
     */
    suspend fun initializeServiceBinding() {
        Log.d(TAG, "initializeServiceBinding: starting unified service binding")
        _uiState.value = _uiState.value.copy(isLoading = true)
        
        serviceBindingUseCase().fold(
            onSuccess = { readinessState ->
                Log.d(TAG, "initializeServiceBinding: completed with readiness: ${readinessState.isReady}")
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    serviceReadinessState = readinessState,
                    isServiceReady = readinessState.isReady,
                    showPermissionRationale = readinessState.permissionStatus is PermissionStatus.RequiresRationale,
                    rationalePermissions = when (readinessState.permissionStatus) {
                        is PermissionStatus.RequiresRationale -> readinessState.permissionStatus.permissions
                        else -> emptyList()
                    },
                    errorMessage = readinessState.errorMessage
                )
            },
            onFailure = { exception ->
                Log.e(TAG, "initializeServiceBinding: use case failed", exception)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Service binding failed: ${exception.message}"
                )
            }
        )
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
                // Get settings to use configured model
                val settings = settingsRepository.getSettings()
                
                val transcriptionRequest = TranscriptionRequest(
                    audioFile = audioFile,
                    model = settings.selectedModel,
                    language = settings.language,
                    customPrompt = settings.customPrompt,
                    temperature = settings.temperature
                )
                
                Log.d(TAG, "Transcription request created: model=${transcriptionRequest.model}, file=${transcriptionRequest.audioFile.path}")
                
                when (val result = transcriptionUseCase.execute(transcriptionRequest)) {
                    is Result.Success -> {
                        Log.d(TAG, "Transcription successful: '${result.data.text}' (${result.data.text.length} chars)")
                        Log.d(TAG, "Detected language: ${result.data.language}")
                        
                        // Attempt text insertion via accessibility service
                        val textInserted = try {
                            Log.d(TAG, "Attempting text insertion via accessibility service")
                            textInsertionService.insertText(result.data.text)
                        } catch (e: Exception) {
                            Log.e(TAG, "Text insertion failed", e)
                            false
                        }
                        
                        Log.d(TAG, "Text insertion result: $textInserted")
                        
                        // Show success toast with transcription preview
                        val previewText = if (result.data.text.length > 50) {
                            "${result.data.text.take(47)}..."
                        } else {
                            result.data.text
                        }
                        
                        if (textInserted) {
                            showToast("Text inserted: $previewText")
                        } else {
                            showToast("Transcribed: $previewText (insertion failed)")
                        }
                        
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
    val serviceReadinessState: ServiceReadinessState = ServiceReadinessState.notReady(
        serviceConnectionStatus = ServiceConnectionStatus.Failed,
        permissionStatus = PermissionStatus.Denied(emptyList()),
        errorMessage = "Services not initialized"
    ),
    val recordingState: AudioRecordingService.RecordingState = AudioRecordingService.RecordingState.IDLE,
    val isServiceReady: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val lastRecording: AudioFile? = null,
    val transcriptionResult: String? = null,
    val transcriptionLanguage: String? = null,
    val showPermissionRationale: Boolean = false,
    val rationalePermissions: List<String> = emptyList()
) {
    // Computed properties for backward compatibility during transition
    val serviceConnectionState: ServiceConnectionStatus get() = serviceReadinessState.serviceConnectionStatus
    val permissionState: PermissionStatus get() = serviceReadinessState.permissionStatus
}