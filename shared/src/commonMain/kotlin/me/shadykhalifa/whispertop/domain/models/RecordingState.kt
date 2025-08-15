package me.shadykhalifa.whispertop.domain.models

sealed class RecordingState {
    data object Idle : RecordingState()
    data object Recording : RecordingState()
    data object Processing : RecordingState()
    data class Error(val message: String, val exception: Throwable? = null) : RecordingState()
    data class Success(val transcription: String) : RecordingState()
}