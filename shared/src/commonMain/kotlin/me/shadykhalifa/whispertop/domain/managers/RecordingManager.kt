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
import me.shadykhalifa.whispertop.data.audio.getCurrentTimeMillis
import me.shadykhalifa.whispertop.domain.models.AudioFile
import me.shadykhalifa.whispertop.domain.models.RecordingState
import me.shadykhalifa.whispertop.domain.models.TranscriptionRequest
import me.shadykhalifa.whispertop.domain.repositories.AudioRepository
import me.shadykhalifa.whispertop.domain.repositories.TranscriptionRepository
import me.shadykhalifa.whispertop.utils.Result
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class RecordingManager(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : KoinComponent {
    private val audioRepository: AudioRepository by inject()
    private val transcriptionRepository: TranscriptionRepository by inject()
    
    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()
    
    private var recordingJob: Job? = null
    private var timeoutJob: Job? = null
    private var durationUpdateJob: Job? = null
    
    private val maxRecordingDurationMs = 25 * 60 * 1000L // 25 minutes for 25MB limit
    private val retryAttempts = 3
    private val retryDelayMs = 1000L
    
    fun startRecording() {
        if (_recordingState.value !is RecordingState.Idle) {
            return
        }
        
        recordingJob = scope.launch {
            try {
                val startTime = getCurrentTimeMillis()
                _recordingState.value = RecordingState.Recording(startTime = startTime)
                
                startDurationUpdates(startTime)
                startTimeoutJob()
                
                val startResult = audioRepository.startRecording()
                when (startResult) {
                    is Result.Error -> {
                        handleRecordingError(startResult.exception, retryable = true)
                    }
                    is Result.Success -> {
                        // Recording started successfully
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
        durationUpdateJob?.cancel()
        timeoutJob?.cancel()
        
        recordingJob = scope.launch {
            try {
                _recordingState.value = RecordingState.Processing()
                
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
        durationUpdateJob?.cancel()
        timeoutJob?.cancel()
        
        scope.launch {
            try {
                audioRepository.cancelRecording()
            } catch (exception: Throwable) {
                // Ignore errors during cancellation cleanup
            } finally {
                _recordingState.value = RecordingState.Idle
            }
        }
    }
    
    fun retryFromError() {
        val currentState = _recordingState.value
        if (currentState is RecordingState.Error && currentState.retryable) {
            _recordingState.value = RecordingState.Idle
        }
    }
    
    fun resetToIdle() {
        cancelRecording()
    }
    
    private fun startDurationUpdates(startTime: Long) {
        durationUpdateJob = scope.launch {
            while (true) {
                delay(100) // Update every 100ms
                val currentState = _recordingState.value
                if (currentState is RecordingState.Recording) {
                    val duration = getCurrentTimeMillis() - startTime
                    _recordingState.value = currentState.copy(duration = duration)
                } else {
                    break
                }
            }
        }
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
                _recordingState.value = RecordingState.Processing(progress / 100f)
                delay(50) // Simulate progress updates
            }
            
            val transcriptionRequest = TranscriptionRequest(audioFile = audioFile)
            val transcriptionResult = transcriptionRepository.transcribe(transcriptionRequest)
            
            when (transcriptionResult) {
                is Result.Success -> {
                    _recordingState.value = RecordingState.Success(
                        audioFile = audioFile,
                        transcription = transcriptionResult.data.text
                    )
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
        durationUpdateJob?.cancel()
        timeoutJob?.cancel()
        
        try {
            audioRepository.cancelRecording()
        } catch (cleanupException: Throwable) {
            // Ignore cleanup errors
        }
        
        _recordingState.value = RecordingState.Error(
            throwable = throwable,
            retryable = retryable
        )
    }
    
    fun cleanup() {
        recordingJob?.cancel()
        durationUpdateJob?.cancel()
        timeoutJob?.cancel()
        scope.cancel()
    }
}