package me.shadykhalifa.whispertop.domain.managers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.shadykhalifa.whispertop.data.audio.getCurrentTimeMillis
import me.shadykhalifa.whispertop.domain.models.AudioFile
import me.shadykhalifa.whispertop.domain.models.RecordingState
import me.shadykhalifa.whispertop.domain.models.TranscriptionRequest
import me.shadykhalifa.whispertop.domain.repositories.AudioRepository
import me.shadykhalifa.whispertop.domain.repositories.TranscriptionRepository
import me.shadykhalifa.whispertop.domain.repositories.SettingsRepository
import me.shadykhalifa.whispertop.domain.services.DebugLoggingService
import me.shadykhalifa.whispertop.domain.services.LoggingManager
import me.shadykhalifa.whispertop.utils.Result
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject



class RecordingManager(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val loggingManager: LoggingManager,
) : KoinComponent {

    companion object {
        const val TAG = "RecordingManager";
    }
    private val audioRepository: AudioRepository by inject()
    private val transcriptionRepository: TranscriptionRepository by inject()
    private val settingsRepository: SettingsRepository by inject()
    
    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()
    
    private var recordingJob: Job? = null
    private var timeoutJob: Job? = null
    private var stateResetJob: Job? = null
    
    private val stateMutex = Mutex()

    
    private val maxRecordingDurationMs = 25 * 60 * 1000L // 25 minutes for 25MB limit
    private val retryAttempts = 3
    private val retryDelayMs = 1000L
    private val successDisplayDelayMs = 1500L // Show success state for 1.5 seconds
    
    private suspend fun updateState(newState: RecordingState, reason: String = "") {
        stateMutex.withLock {
            val oldState = _recordingState.value
            _recordingState.value = newState
            loggingManager.debug(
                TAG,
                "State transition: ${oldState::class.simpleName} → ${newState::class.simpleName}${if (reason.isNotEmpty()) " ($reason)" else ""}"
            )
        }
    }
    
    fun startRecording() {
        if (_recordingState.value !is RecordingState.Idle) {
            loggingManager.debug(
                TAG,
                "Recording state is not Idle, returning",
            );
            return
        }
        
        recordingJob = scope.launch {
            try {
                val startTime = getCurrentTimeMillis()
                updateState(RecordingState.Recording(startTime = startTime), "start recording")
                

                startTimeoutJob()
                
                val startResult = audioRepository.startRecording()
                when (startResult) {
                    is Result.Error -> {
                        handleRecordingError(startResult.exception, retryable = true)
                    }
                    is Result.Success -> {
                        // Recording started successfully, continue with transcription
                    }
                    is Result.Loading -> {
                        // Continue - loading state not relevant for start recording
                    }
                }
                
            } catch (exception: Throwable) {
                handleRecordingError(exception, retryable = true)
            }
        }
    }
    
    fun stopRecording() {
        val currentState = _recordingState.value
        if (currentState !is RecordingState.Recording) {
            return
        }
        
        recordingJob?.cancel()
        timeoutJob?.cancel()
        
        recordingJob = scope.launch {
            try {
                updateState(RecordingState.Processing(), "stop recording")
                
                val stopResult = audioRepository.stopRecording()
                when (stopResult) {
                    is Result.Success -> {
                        processTranscription(stopResult.data)
                    }
                    is Result.Error -> {
                        handleRecordingError(stopResult.exception, retryable = true)
                    }
                    is Result.Loading -> {
                        // Continue processing - loading state handled elsewhere
                    }
                }
                
            } catch (exception: Throwable) {
                handleRecordingError(exception, retryable = true)
            }
        }
    }
    
    fun cancelRecording() {
        recordingJob?.cancel()
        timeoutJob?.cancel()
        stateResetJob?.cancel()
        
        scope.launch {
            try {
                audioRepository.cancelRecording()
            } catch (exception: Throwable) {
                // Ignore errors during cancellation cleanup
            } finally {
                updateState(RecordingState.Idle, "cancel recording")
            }
        }
    }
    
    fun retryFromError() {
        scope.launch {
            val currentState = _recordingState.value
            when (currentState) {
                is RecordingState.Error -> {
                    if (currentState.retryable) {
                        updateState(RecordingState.Idle, "retry from error")
                    }
                }
                is RecordingState.Success -> {
                    updateState(RecordingState.Idle, "reset from success")
                }
                else -> {
                    loggingManager.debug(TAG, "retryFromError called but state is ${currentState::class.simpleName}, ignoring")
                }
            }
        }
    }
    
    fun resetToIdle() {
        stateResetJob?.cancel()
        cancelRecording()
    }
    
    private fun startTimeoutJob() {
        timeoutJob = scope.launch {
            delay(maxRecordingDurationMs)
            val currentState = _recordingState.value
            if (currentState is RecordingState.Recording) {
                handleRecordingError(
                    RuntimeException("Recording exceeded maximum duration"),
                    retryable = false
                )
            }
        }
    }
    
    private suspend fun processTranscription(audioFile: AudioFile) {
        try {
            for (progress in 0..100 step 10) {
                updateState(RecordingState.Processing(progress / 100f), "processing progress")
                delay(50) // Simulate progress updates
            }
            
            val settings = settingsRepository.getSettings()
            val transcriptionRequest = TranscriptionRequest(
                audioFile = audioFile,
                language = if (settings.autoDetectLanguage) null else settings.language,
                model = settings.selectedModel,
                customPrompt = settings.customPrompt,
                temperature = settings.temperature
            )
            val transcriptionResult = transcriptionRepository.transcribe(transcriptionRequest)
            
            when (transcriptionResult) {
                is Result.Success -> {
                    updateState(
                        RecordingState.Success(
                            audioFile = audioFile,
                            transcription = transcriptionResult.data.text
                        ),
                        "transcription success"
                    )
                    
                    // Auto-transition back to Idle after displaying success
                    stateResetJob = scope.launch {
                        delay(successDisplayDelayMs)
                        updateState(RecordingState.Idle, "auto-reset after success")
                    }
                }
                is Result.Error -> {
                    handleRecordingError(transcriptionResult.exception, retryable = true)
                }
                is Result.Loading -> {
                    // Continue processing - loading handled by progress updates
                }
            }
            
        } catch (exception: Throwable) {
            handleRecordingError(exception, retryable = true)
        }
    }
    
    private suspend fun handleRecordingError(throwable: Throwable, retryable: Boolean) {
        recordingJob?.cancel()
        timeoutJob?.cancel()
        stateResetJob?.cancel()
        
        try {
            audioRepository.cancelRecording()
        } catch (cleanupException: Throwable) {
            // Ignore cleanup errors
        }
        
        scope.launch {
            updateState(
                RecordingState.Error(
                    throwable = throwable,
                    retryable = retryable
                ),
                "recording error: ${throwable.message}"
            )
        }
    }
    
    fun cleanup() {
        recordingJob?.cancel()
        timeoutJob?.cancel()
        stateResetJob?.cancel()
        scope.cancel()
    }
}