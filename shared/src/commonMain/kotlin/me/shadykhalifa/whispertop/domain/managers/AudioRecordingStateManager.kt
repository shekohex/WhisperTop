package me.shadykhalifa.whispertop.domain.managers

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import me.shadykhalifa.whispertop.domain.models.AudioFile
import me.shadykhalifa.whispertop.domain.models.RecordingState
import me.shadykhalifa.whispertop.domain.services.ErrorLoggingService
import me.shadykhalifa.whispertop.domain.services.IAudioServiceManager

class AudioRecordingStateManager(
    private val errorLoggingService: ErrorLoggingService
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    
    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()
    
    private val _recordingCompleteEvents = Channel<AudioFile?>(Channel.BUFFERED)
    val recordingCompleteEvents = _recordingCompleteEvents.receiveAsFlow()
    
    private val _errorEvents = Channel<String>(Channel.BUFFERED)
    val errorEvents = _errorEvents.receiveAsFlow()
    
    fun updateRecordingState(state: RecordingState) {
        _recordingState.value = state
    }
    
    fun notifyRecordingComplete(audioFile: AudioFile?) {
        scope.launch {
            _recordingCompleteEvents.send(audioFile)
        }
    }
    
    fun notifyRecordingError(error: String, exception: Exception? = null) {
        scope.launch {
            _errorEvents.send(error)
        }
        
        // Log error through the domain error logging service
        exception?.let { 
            errorLoggingService.logError(
                error = it,
                context = mapOf("component" to "AudioRecordingStateManager"),
                additionalInfo = error
            )
        } ?: run {
            errorLoggingService.logWarning(
                message = error,
                context = mapOf("component" to "AudioRecordingStateManager")
            )
        }
    }
    
    fun getCurrentState(): RecordingState = _recordingState.value
    
    fun resetToIdle() {
        _recordingState.value = RecordingState.Idle
    }
    
    fun cleanup() {
        scope.cancel()
    }
}