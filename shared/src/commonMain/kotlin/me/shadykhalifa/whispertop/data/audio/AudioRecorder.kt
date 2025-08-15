package me.shadykhalifa.whispertop.data.audio

import me.shadykhalifa.whispertop.domain.models.AudioFile

interface AudioRecorder {
    suspend fun startRecording(outputPath: String): AudioRecordingResult
    suspend fun stopRecording(): AudioFile?
    suspend fun cancelRecording()
    fun isRecording(): Boolean
    fun addRecordingStateListener(listener: RecordingStateListener)
    fun removeRecordingStateListener(listener: RecordingStateListener)
}

interface AudioConfigurationProvider {
    val sampleRate: Int
    val channelConfig: Int
    val audioFormat: Int
    val bufferSizeMultiplier: Int
}

interface AudioFocusManager {
    fun requestAudioFocus(listener: AudioFocusChangeListener): Boolean
    fun abandonAudioFocus()
}

interface RecordingStateListener {
    fun onRecordingStarted()
    fun onRecordingPaused()
    fun onRecordingResumed()
    fun onRecordingStopped()
    fun onRecordingError(error: AudioRecordingError)
}

interface AudioFocusChangeListener {
    fun onAudioFocusChange(focusChange: AudioFocusChange)
}

sealed class AudioRecordingResult {
    object Success : AudioRecordingResult()
    data class Error(val error: AudioRecordingError) : AudioRecordingResult()
}

sealed class AudioRecordingError(val message: String, val cause: Throwable? = null) {
    class PermissionDenied(cause: Throwable? = null) : AudioRecordingError("Audio recording permission denied", cause)
    class DeviceUnavailable(cause: Throwable? = null) : AudioRecordingError("Audio recording device unavailable", cause)
    class ConfigurationError(cause: Throwable? = null) : AudioRecordingError("Audio configuration error", cause)
    class IOError(cause: Throwable? = null) : AudioRecordingError("Audio I/O error", cause)
    class Unknown(cause: Throwable? = null) : AudioRecordingError("Unknown recording error", cause)
}

enum class AudioFocusChange {
    GAIN,
    LOSS,
    LOSS_TRANSIENT,
    LOSS_TRANSIENT_CAN_DUCK
}

data class AudioConfiguration(
    override val sampleRate: Int = 16000,
    override val channelConfig: Int = 1, // mono
    override val audioFormat: Int = 16, // 16-bit PCM
    override val bufferSizeMultiplier: Int = 4
) : AudioConfigurationProvider

expect class AudioRecorderImpl() : AudioRecorder
expect class AudioFocusManagerImpl() : AudioFocusManager