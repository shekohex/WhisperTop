package me.shadykhalifa.whispertop.data.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.io.files.Path
import me.shadykhalifa.whispertop.domain.models.AudioFile
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import kotlin.coroutines.resume

/**
 * Simple MediaRecorder-based audio recorder for AAC/M4A files
 */
class AudioRecorderImpl : AudioRecorder, KoinComponent {
    companion object {
        private const val TAG = "AudioRecorderImpl"
        private const val SAMPLE_RATE = 16000 // Optimal for Whisper
        private const val BIT_RATE = 64000 // 64kbps for good quality
    }
    
    private val context: Context by inject()
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var startTime = 0L
    private var outputPath: String? = null
    private val stateListeners = mutableListOf<RecordingStateListener>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    @SuppressLint("MissingPermission")
    override suspend fun startRecording(outputPath: String): AudioRecordingResult = 
        suspendCancellableCoroutine { continuation ->
            try {
                if (isRecording) {
                    continuation.resume(
                        AudioRecordingResult.Error(
                            AudioRecordingError.ConfigurationError(
                                IllegalStateException("Recording already in progress")
                            )
                        )
                    )
                    return@suspendCancellableCoroutine
                }
                
                Log.d(TAG, "Starting recording to: $outputPath")
                
                // Ensure parent directory exists
                File(outputPath).parentFile?.mkdirs()
                
                mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    MediaRecorder(context)
                } else {
                    @Suppress("DEPRECATION")
                    MediaRecorder()
                }.apply {
                    // Audio source optimized for voice recognition
                    setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
                    
                    // Output format: MPEG_4 container for M4A files
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    
                    // AAC encoder for good compression
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    
                    // Audio settings optimized for Whisper
                    setAudioSamplingRate(SAMPLE_RATE)
                    setAudioEncodingBitRate(BIT_RATE)
                    setAudioChannels(1) // Mono
                    
                    // Output file
                    setOutputFile(outputPath)
                    
                    setOnErrorListener { _, what, extra ->
                        Log.e(TAG, "MediaRecorder error: what=$what, extra=$extra")
                        val error = AudioRecordingError.ConfigurationError(
                            RuntimeException("MediaRecorder error: what=$what, extra=$extra")
                        )
                        notifyStateListeners { onRecordingError(error) }
                    }
                    
                    setOnInfoListener { _, what, extra ->
                        Log.i(TAG, "MediaRecorder info: what=$what, extra=$extra")
                        when (what) {
                            MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED,
                            MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED -> {
                                Log.w(TAG, "Recording limit reached")
                                // Auto-stop recording
                                scope.launch {
                                    try {
                                        if (isRecording) {
                                            stopRecording()
                                        }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error stopping recording on limit", e)
                                    }
                                }
                            }
                        }
                    }
                    
                    // Prepare and start
                    prepare()
                    start()
                }
                
                    this@AudioRecorderImpl.outputPath = outputPath
                startTime = System.currentTimeMillis()
                isRecording = true
                
                notifyStateListeners { onRecordingStarted() }
                Log.d(TAG, "Recording started successfully")
                
                continuation.resume(AudioRecordingResult.Success)
                
            } catch (e: SecurityException) {
                Log.e(TAG, "Permission denied", e)
                cleanupRecorder()
                continuation.resume(
                    AudioRecordingResult.Error(AudioRecordingError.PermissionDenied(e))
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start recording", e)
                cleanupRecorder()
                continuation.resume(
                    AudioRecordingResult.Error(AudioRecordingError.ConfigurationError(e))
                )
            }
        }
    
    override suspend fun stopRecording(): AudioFile? {
        if (!isRecording) return null
        
        return try {
            Log.d(TAG, "Stopping recording")
            
            isRecording = false
            mediaRecorder?.apply {
                stop()
                reset()
                release()
            }
            mediaRecorder = null
            
            val duration = System.currentTimeMillis() - startTime
            val filePath = outputPath
            
            val audioFile = if (filePath != null) {
                val file = File(filePath)
                if (file.exists() && file.length() > 0) {
                    AudioFile(
                        path = file.absolutePath,
                        durationMs = duration,
                        sizeBytes = file.length()
                    )
                } else {
                    Log.w(TAG, "Recording file is empty or doesn't exist")
                    null
                }
            } else {
                Log.w(TAG, "No output path set")
                null
            }
            
            notifyStateListeners { onRecordingStopped() }
            Log.d(TAG, "Recording stopped. Duration: ${duration}ms, File size: ${audioFile?.sizeBytes ?: 0} bytes")
            
            audioFile
            
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
            cleanupRecorder()
            notifyStateListeners { onRecordingError(AudioRecordingError.IOError(e)) }
            null
        }
    }
    
    override suspend fun cancelRecording() {
        if (!isRecording) return
        
        try {
            Log.d(TAG, "Cancelling recording")
            
            isRecording = false
            mediaRecorder?.apply {
                stop()
                reset()
                release()
            }
            mediaRecorder = null
            
            // Delete the file
            outputPath?.let { File(it).delete() }
            
            notifyStateListeners { onRecordingStopped() }
            Log.d(TAG, "Recording cancelled")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling recording", e)
            cleanupRecorder()
        }
    }
    
    override fun isRecording(): Boolean = isRecording
    
    override fun addRecordingStateListener(listener: RecordingStateListener) {
        stateListeners.add(listener)
    }
    
    override fun removeRecordingStateListener(listener: RecordingStateListener) {
        stateListeners.remove(listener)
    }
    
    private fun cleanupRecorder() {
        isRecording = false
        mediaRecorder?.apply {
            try {
                reset()
                release()
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing MediaRecorder", e)
            }
        }
        mediaRecorder = null
        outputPath = null
    }
    
    private fun notifyStateListeners(action: RecordingStateListener.() -> Unit) {
        stateListeners.forEach { listener ->
            try {
                listener.action()
            } catch (e: Exception) {
                Log.e(TAG, "Error in state listener", e)
            }
        }
    }
}

class AudioFocusManagerImpl : AudioFocusManager, KoinComponent {
    private val context: Context by inject()
    
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var currentListener: AudioFocusChangeListener? = null
    
    @SuppressLint("NewApi")
    override fun requestAudioFocus(listener: AudioFocusChangeListener): Boolean {
        currentListener = listener
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setOnAudioFocusChangeListener { focusChange ->
                    handleAudioFocusChange(focusChange)
                }
                .build()
            
            audioFocusRequest = focusRequest
            audioManager.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                { focusChange -> handleAudioFocusChange(focusChange) },
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }
    
    @SuppressLint("NewApi")
    override fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let {
                audioManager.abandonAudioFocusRequest(it)
            }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus { }
        }
        
        audioFocusRequest = null
        currentListener = null
    }
    
    private fun handleAudioFocusChange(focusChange: Int) {
        val change = when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> AudioFocusChange.GAIN
            AudioManager.AUDIOFOCUS_LOSS -> AudioFocusChange.LOSS
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> AudioFocusChange.LOSS_TRANSIENT
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> AudioFocusChange.LOSS_TRANSIENT_CAN_DUCK
            else -> return
        }
        
        currentListener?.onAudioFocusChange(change)
    }
}