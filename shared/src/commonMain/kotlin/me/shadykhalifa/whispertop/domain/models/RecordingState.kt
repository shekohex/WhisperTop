package me.shadykhalifa.whispertop.domain.models

sealed class RecordingState {
    data object Idle : RecordingState()
    
    data class Recording(
        val startTime: Long,
        val duration: Long = 0L
    ) : RecordingState()
    
    data class Processing(
        val progress: Float = 0f
    ) : RecordingState()
    
    data class Success(
        val audioFile: AudioFile,
        val transcription: String
    ) : RecordingState()
    
    data class Error(
        val throwable: Throwable,
        val retryable: Boolean = true
    ) : RecordingState()
}