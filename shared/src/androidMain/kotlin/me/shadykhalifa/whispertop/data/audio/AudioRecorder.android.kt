package me.shadykhalifa.whispertop.data.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.shadykhalifa.whispertop.domain.models.AudioFile
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

actual class AudioRecorderImpl : AudioRecorder, KoinComponent {
    companion object {
        private const val TAG = "AudioRecorderImpl"
    }
    
    private val context: Context by inject()
    private val configuration: AudioConfigurationProvider = AudioConfiguration()
    private val audioFocusManager: AudioFocusManager = AudioFocusManagerImpl()
    private val qualityManager: AudioQualityManager = AudioQualityManager()
    private val audioProcessor: AudioProcessor = AudioProcessor(gainFactor = 2.5f)
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val recordingExecutor = Executors.newSingleThreadExecutor { Thread(it, "AudioRecorder") }
    private val recordingDispatcher = recordingExecutor.asCoroutineDispatcher()
    
    private val mutex = Mutex()
    private val isRecording = AtomicBoolean(false)
    private val isPaused = AtomicBoolean(false)
    private val stateListeners = CopyOnWriteArrayList<RecordingStateListener>()
    
    private var audioRecord: AudioRecord? = null
    private var recordingThread: AudioRecordingThread? = null
    private var currentOutputPath: String? = null
    
    private val audioFocusChangeListener = object : AudioFocusChangeListener {
        override fun onAudioFocusChange(focusChange: AudioFocusChange) {
            when (focusChange) {
                AudioFocusChange.LOSS -> {
                    scope.launch { 
                        stopRecording()
                        notifyStateListeners { onRecordingError(AudioRecordingError.DeviceUnavailable()) }
                    }
                }
                AudioFocusChange.LOSS_TRANSIENT -> {
                    pauseRecording()
                }
                AudioFocusChange.LOSS_TRANSIENT_CAN_DUCK -> {
                    // Continue recording at lower volume (handled by system)
                }
                AudioFocusChange.GAIN -> {
                    resumeRecording()
                }
            }
        }
    }
    
    override suspend fun startRecording(outputPath: String): AudioRecordingResult = withContext(recordingDispatcher) {
        mutex.withLock {
            Log.d(TAG, "startRecording: outputPath=$outputPath")
            
            if (isRecording.get()) {
                Log.w(TAG, "startRecording: recording already in progress")
                return@withContext AudioRecordingResult.Error(
                    AudioRecordingError.ConfigurationError(IllegalStateException("Recording already in progress"))
                )
            }
            
            try {
                // Request audio focus
                Log.d(TAG, "startRecording: requesting audio focus")
                if (!audioFocusManager.requestAudioFocus(audioFocusChangeListener)) {
                    Log.w(TAG, "startRecording: failed to acquire audio focus")
                    return@withContext AudioRecordingResult.Error(
                        AudioRecordingError.DeviceUnavailable(Exception("Failed to acquire audio focus"))
                    )
                }
                Log.d(TAG, "startRecording: audio focus acquired")
                
                // Create AudioRecord instance
                val bufferSize = getOptimalBufferSize()
                Log.d(TAG, "startRecording: creating AudioRecord with bufferSize=$bufferSize")
                audioRecord = createAudioRecord(bufferSize)
                    ?: run {
                        Log.e(TAG, "startRecording: failed to create AudioRecord")
                        return@withContext AudioRecordingResult.Error(
                            AudioRecordingError.ConfigurationError(Exception("Failed to create AudioRecord"))
                        )
                    }
                Log.d(TAG, "startRecording: AudioRecord created successfully")
                
                // Prepare recording thread
                recordingThread = AudioRecordingThread(
                    audioRecord = audioRecord!!,
                    outputPath = outputPath,
                    bufferSize = bufferSize,
                    qualityManager = qualityManager,
                    audioProcessor = audioProcessor,
                    onError = { error ->
                        scope.launch {
                            cleanupRecording()
                            notifyStateListeners { onRecordingError(error) }
                        }
                    },
                    onFileSizeLimit = {
                        scope.launch {
                            stopRecording()
                            notifyStateListeners { 
                                onRecordingError(
                                    AudioRecordingError.IOError(
                                        Exception("Recording stopped: Maximum file size reached (25MB)")
                                    )
                                )
                            }
                        }
                    }
                )
                
                currentOutputPath = outputPath
                isRecording.set(true)
                
                // Start recording
                Log.d(TAG, "startRecording: starting AudioRecord")
                audioRecord!!.startRecording()
                recordingThread!!.start()
                qualityManager.startMonitoring()
                
                notifyStateListeners { onRecordingStarted() }
                Log.d(TAG, "startRecording: recording started successfully")
                AudioRecordingResult.Success
                
            } catch (e: SecurityException) {
                Log.e(TAG, "startRecording: permission denied", e)
                cleanupRecording()
                AudioRecordingResult.Error(AudioRecordingError.PermissionDenied(e))
            } catch (e: Exception) {
                Log.e(TAG, "startRecording: unknown error", e)
                cleanupRecording()
                AudioRecordingResult.Error(AudioRecordingError.Unknown(e))
            }
        }
    }
    
    override suspend fun stopRecording(): AudioFile? = withContext(recordingDispatcher) {
        mutex.withLock {
            if (!isRecording.get()) {
                return@withLock null
            }
            
            try {
                isRecording.set(false)
                isPaused.set(false)
                
                audioRecord?.stop()
                recordingThread?.stopRecording()
                recordingThread?.join()
                
                val outputPath = currentOutputPath
                val audioFile = if (outputPath != null && File(outputPath).exists()) {
                    val duration = recordingThread?.getDuration() ?: 0L
                    val file = File(outputPath)
                    AudioFile(
                        path = file.absolutePath,
                        durationMs = duration,
                        sizeBytes = file.length()
                    )
                } else null
                
                cleanupRecording()
                notifyStateListeners { onRecordingStopped() }
                
                audioFile
            } catch (e: Exception) {
                cleanupRecording()
                notifyStateListeners { onRecordingError(AudioRecordingError.IOError(e)) }
                null
            }
        }
    }
    
    override suspend fun cancelRecording() = withContext(recordingDispatcher) {
        mutex.withLock {
            if (!isRecording.get()) return@withLock
            
            try {
                isRecording.set(false)
                isPaused.set(false)
                
                audioRecord?.stop()
                recordingThread?.stopRecording()
                recordingThread?.join()
                
                // Delete output file
                currentOutputPath?.let { File(it).delete() }
                
            } catch (e: Exception) {
                // Ignore errors during cancellation
            } finally {
                cleanupRecording()
                notifyStateListeners { onRecordingStopped() }
            }
        }
    }
    
    override fun isRecording(): Boolean = isRecording.get() && !isPaused.get()
    
    override fun addRecordingStateListener(listener: RecordingStateListener) {
        stateListeners.add(listener)
    }
    
    override fun removeRecordingStateListener(listener: RecordingStateListener) {
        stateListeners.remove(listener)
    }
    
    private fun pauseRecording() {
        if (isRecording.get() && !isPaused.get()) {
            isPaused.set(true)
            recordingThread?.pauseRecording()
            notifyStateListeners { onRecordingPaused() }
        }
    }
    
    private fun resumeRecording() {
        if (isRecording.get() && isPaused.get()) {
            isPaused.set(false)
            recordingThread?.resumeRecording()
            notifyStateListeners { onRecordingResumed() }
        }
    }
    
    @SuppressLint("MissingPermission")
    private fun createAudioRecord(bufferSize: Int): AudioRecord? {
        return try {
            Log.d(TAG, "createAudioRecord: sampleRate=${configuration.sampleRate}, bufferSize=$bufferSize")
            
            val audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                configuration.sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )
            
            Log.d(TAG, "createAudioRecord: AudioRecord state=${audioRecord.state}, recordingState=${audioRecord.recordingState}")
            
            if (audioRecord.state == AudioRecord.STATE_INITIALIZED) {
                Log.d(TAG, "createAudioRecord: AudioRecord initialized successfully")
                audioRecord
            } else {
                Log.w(TAG, "createAudioRecord: AudioRecord not initialized (state=${audioRecord.state})")
                audioRecord.release()
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "createAudioRecord: exception occurred", e)
            null
        }
    }
    
    private fun getOptimalBufferSize(): Int {
        val minBufferSize = AudioRecord.getMinBufferSize(
            configuration.sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        
        Log.d(TAG, "getOptimalBufferSize: minBufferSize=$minBufferSize, multiplier=${configuration.bufferSizeMultiplier}")
        
        return if (minBufferSize != AudioRecord.ERROR_BAD_VALUE) {
            val optimalSize = minBufferSize * configuration.bufferSizeMultiplier
            Log.d(TAG, "getOptimalBufferSize: using optimal size=$optimalSize")
            optimalSize
        } else {
            Log.w(TAG, "getOptimalBufferSize: minBufferSize is ERROR_BAD_VALUE, using fallback")
            8192 // Fallback buffer size
        }
    }
    
    private fun cleanupRecording() {
        audioRecord?.release()
        audioRecord = null
        recordingThread = null
        currentOutputPath = null
        audioFocusManager.abandonAudioFocus()
    }
    
    private fun notifyStateListeners(action: RecordingStateListener.() -> Unit) {
        stateListeners.forEach { listener ->
            try {
                listener.action()
            } catch (e: Exception) {
                // Ignore listener errors
            }
        }
    }
    
    fun getQualityReport(): QualityReport = qualityManager.getQualityReport()
    
    fun getCurrentMetrics(): AudioMetrics = qualityManager.currentMetrics.value
    
    fun getRecordingStatistics(): RecordingStatistics? = qualityManager.recordingStatistics.value
    
    fun isAudioLevelAcceptable(): Boolean {
        val metrics = qualityManager.currentMetrics.value
        val stats = qualityManager.recordingStatistics.value
        
        // Check if audio levels are sufficient for transcription
        val hasAcceptableRMS = metrics.rmsLevel > 0.02f // At least 2% of max amplitude
        val hasAcceptableDB = metrics.dbLevel > -50f    // Above -50dB
        val notTooQuiet = stats?.averageLevel ?: 0f > 0.01f
        
        val acceptable = hasAcceptableRMS && hasAcceptableDB && notTooQuiet
        
        Log.d(TAG, "isAudioLevelAcceptable: rms=${metrics.rmsLevel}, db=${metrics.dbLevel}, avg=${stats?.averageLevel}, acceptable=$acceptable")
        
        return acceptable
    }
    
    fun logAudioDiagnostics() {
        val metrics = getCurrentMetrics()
        val stats = getRecordingStatistics()
        val report = getQualityReport()
        
        Log.d(TAG, "=== AUDIO DIAGNOSTICS ===")
        Log.d(TAG, "Current RMS: ${metrics.rmsLevel} (${(metrics.rmsLevel * 100).toInt()}%)")
        Log.d(TAG, "Current Peak: ${metrics.peakLevel} (${(metrics.peakLevel * 100).toInt()}%)")
        Log.d(TAG, "Current dB: ${metrics.dbLevel}")
        Log.d(TAG, "Quality Score: ${metrics.qualityScore}/100")
        Log.d(TAG, "Is Silent: ${metrics.isSilent}")
        Log.d(TAG, "Is Clipping: ${metrics.isClipping}")
        
        stats?.let {
            Log.d(TAG, "Recording Duration: ${it.duration}ms")
            Log.d(TAG, "Average Level: ${it.averageLevel}")
            Log.d(TAG, "File Size: ${it.fileSize} bytes")
            Log.d(TAG, "Silence: ${it.silencePercentage}%")
        }
        
        if (report.issues.isNotEmpty()) {
            Log.w(TAG, "Quality Issues: ${report.issues}")
            Log.w(TAG, "Recommendations: ${report.recommendations}")
        }
        Log.d(TAG, "========================")
    }
    
    fun cleanup() {
        scope.cancel()
        recordingExecutor.shutdown()
        cleanupRecording()
    }
}

private class AudioRecordingThread(
    private val audioRecord: AudioRecord,
    private val outputPath: String,
    private val bufferSize: Int,
    private val qualityManager: AudioQualityManager,
    private val audioProcessor: AudioProcessor,
    private val onError: (AudioRecordingError) -> Unit,
    private val onFileSizeLimit: () -> Unit
) : Thread("AudioRecordingThread") {
    
    private val quit = AtomicBoolean(false)
    private val paused = AtomicBoolean(false)
    private val startTime = AtomicReference<Long>()
    private val endTime = AtomicReference<Long>()
    private val pauseStartTime = AtomicReference<Long>()
    private val totalPauseDuration = AtomicReference(0L)
    
    override fun run() {
        try {
            startTime.set(System.currentTimeMillis())
            val buffer = ShortArray(bufferSize / 2)
            val allData = mutableListOf<Short>()
            var totalBytes = 0L
            var bufferCount = 0
            
            Log.d("AudioRecordingThread", "Starting recording: sampleRate=${audioRecord.sampleRate}, bufferSize=$bufferSize, outputPath=$outputPath")
            
            while (!quit.get()) {
                if (paused.get()) {
                    Thread.sleep(10)
                    continue
                }
                
                val read = audioRecord.read(buffer, 0, buffer.size)
                if (read > 0) {
                    // Process buffer for quality metrics
                    val bufferSlice = if (read < buffer.size) {
                        buffer.sliceArray(0 until read)
                    } else {
                        buffer
                    }
                    
                    qualityManager.processAudioBuffer(bufferSlice)
                    
                    // Log audio levels every 50 buffers (~5 seconds at 100ms buffers)
                    bufferCount++
                    if (bufferCount % 50 == 0) {
                        val metrics = qualityManager.currentMetrics.value
                        Log.d("AudioRecordingThread", "Audio levels: RMS=${metrics.rmsLevel}, Peak=${metrics.peakLevel}, dB=${metrics.dbLevel}, Silent=${metrics.isSilent}")
                    }
                    
                    // Check if we should stop due to file size
                    totalBytes += read * 2 // 2 bytes per sample
                    if (totalBytes >= RecordingConstraints.MAX_FILE_SIZE_BYTES * 0.95) {
                        onFileSizeLimit()
                        break
                    }
                    
                    // Check if quality manager suggests stopping
                    if (qualityManager.shouldStopRecording()) {
                        break
                    }
                    
                    for (i in 0 until read) {
                        allData.add(buffer[i])
                    }
                } else if (read < 0) {
                    onError(AudioRecordingError.IOError(RuntimeException("AudioRecord.read returned $read")))
                    break
                }
            }
            
            endTime.set(System.currentTimeMillis())
            
            // Process audio before saving
            val processedData = audioProcessor.processAudio(allData.toShortArray())
            
            // Write WAV file
            WAVFileWriter().encodeWaveFile(outputPath, processedData)
            
        } catch (e: Exception) {
            endTime.set(System.currentTimeMillis())
            onError(AudioRecordingError.IOError(e))
        }
    }
    
    fun stopRecording() {
        quit.set(true)
    }
    
    fun pauseRecording() {
        if (!paused.get()) {
            paused.set(true)
            pauseStartTime.set(System.currentTimeMillis())
        }
    }
    
    fun resumeRecording() {
        if (paused.get()) {
            val pauseStart = pauseStartTime.get()
            if (pauseStart != null) {
                val pauseDuration = System.currentTimeMillis() - pauseStart
                totalPauseDuration.set(totalPauseDuration.get() + pauseDuration)
            }
            paused.set(false)
            pauseStartTime.set(null)
        }
    }
    
    fun getDuration(): Long {
        val start = startTime.get() ?: return 0L
        val end = endTime.get() ?: System.currentTimeMillis()
        val totalPause = totalPauseDuration.get()
        return maxOf(0L, end - start - totalPause)
    }
}

actual class AudioFocusManagerImpl : AudioFocusManager, KoinComponent {
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