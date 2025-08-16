package me.shadykhalifa.whispertop.presentation.viewmodels

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.shadykhalifa.whispertop.data.audio.getCurrentTimeMillis
import me.shadykhalifa.whispertop.domain.models.AudioFile
import me.shadykhalifa.whispertop.domain.models.RecordingState
import me.shadykhalifa.whispertop.domain.repositories.AudioRepository
import me.shadykhalifa.whispertop.domain.usecases.StartRecordingUseCase
import me.shadykhalifa.whispertop.domain.usecases.StopRecordingUseCase
import me.shadykhalifa.whispertop.utils.Result

class RecordingViewModel(
    private val audioRepository: AudioRepository,
    private val startRecordingUseCase: StartRecordingUseCase,
    private val stopRecordingUseCase: StopRecordingUseCase
) : BaseViewModel() {
    
    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()
    
    fun startRecording() {
        launchSafely { 
            when (val result = startRecordingUseCase()) {
                is Result.Success -> {
                    _recordingState.value = RecordingState.Recording(startTime = getCurrentTimeMillis())
                }
                is Result.Error -> {
                    _recordingState.value = RecordingState.Error(
                        throwable = result.exception,
                        retryable = true
                    )
                }
                is Result.Loading -> {
                    // Handle loading state if needed
                }
            }
        }
    }
    
    fun stopRecording() {
        launchSafely {
            _recordingState.value = RecordingState.Processing()
            
            when (val result = stopRecordingUseCase()) {
                is Result.Success -> {
                    // Get the last recorded file from the repository
                    val audioFile = audioRepository.getLastRecording()
                    if (audioFile != null) {
                        _recordingState.value = RecordingState.Success(
                            audioFile = audioFile,
                            transcription = result.data
                        )
                    } else {
                        _recordingState.value = RecordingState.Error(
                            throwable = IllegalStateException("Could not retrieve recorded audio file"),
                            retryable = false
                        )
                    }
                }
                is Result.Error -> {
                    _recordingState.value = RecordingState.Error(
                        throwable = result.exception,
                        retryable = true
                    )
                }
                is Result.Loading -> {
                    // Already in processing state
                }
            }
        }
    }
    
    fun resetState() {
        _recordingState.value = RecordingState.Idle
    }
    
    fun cancelRecording() {
        launchSafely {
            audioRepository.cancelRecording()
            _recordingState.value = RecordingState.Idle
        }
    }
}