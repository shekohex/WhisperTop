package me.shadykhalifa.whispertop.data.audio

import kotlinx.cinterop.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.shadykhalifa.whispertop.domain.models.AudioFile
import platform.AVFAudio.*
import platform.Foundation.*
import platform.darwin.NSObject
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

@OptIn(ExperimentalForeignApi::class)
actual class AudioRecorderImpl : AudioRecorder {
    private val configuration: AudioConfigurationProvider = AudioConfiguration()
    private val audioFocusManager: AudioFocusManager = AudioFocusManagerImpl()
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mutex = Mutex()
    private val isRecording = AtomicBoolean(false)
    private val isPaused = AtomicBoolean(false)
    private val stateListeners = CopyOnWriteArrayList<RecordingStateListener>()
    
    private var audioRecorder: AVAudioRecorder? = null
    private var outputURL: NSURL? = null
    private var startTime = AtomicReference<Long>()
    private var pauseStartTime = AtomicReference<Long>()
    private var totalPauseDuration = AtomicReference(0L)
    
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
    
    override suspend fun startRecording(outputPath: String): AudioRecordingResult = withContext(Dispatchers.Main) {
        mutex.withLock {
            if (isRecording.get()) {
                return@withContext AudioRecordingResult.Error(
                    AudioRecordingError.ConfigurationError(IllegalStateException("Recording already in progress"))
                )
            }
            
            try {
                // Request audio focus
                if (!audioFocusManager.requestAudioFocus(audioFocusChangeListener)) {
                    return@withContext AudioRecordingResult.Error(
                        AudioRecordingError.DeviceUnavailable(Exception("Failed to acquire audio focus"))
                    )
                }
                
                // Configure audio session
                val session = AVAudioSession.sharedInstance()
                memScoped {
                    val error = alloc<ObjCObjectVar<NSError?>>()
                    session.setCategory(AVAudioSessionCategoryRecord, error.ptr)
                    error.value?.let { 
                        return@withContext AudioRecordingResult.Error(
                            AudioRecordingError.ConfigurationError(Exception("Failed to set audio session category: ${it.localizedDescription}"))
                        )
                    }
                    
                    session.setActive(true, error.ptr)
                    error.value?.let { 
                        return@withContext AudioRecordingResult.Error(
                            AudioRecordingError.ConfigurationError(Exception("Failed to activate audio session: ${it.localizedDescription}"))
                        )
                    }
                }
                
                outputURL = NSURL.fileURLWithPath(outputPath)
                
                // Audio recorder settings - PCM16 mono 16kHz for OpenAI API compatibility
                val settings = mapOf<Any?, Any?>(
                    AVFormatIDKey to kAudioFormatLinearPCM,
                    AVSampleRateKey to configuration.sampleRate.toDouble(),
                    AVNumberOfChannelsKey to 1, // mono
                    AVLinearPCMBitDepthKey to 16,
                    AVLinearPCMIsFloatKey to false,
                    AVLinearPCMIsBigEndianKey to false,
                    AVLinearPCMIsNonInterleaved to false
                )
                
                memScoped {
                    val error = alloc<ObjCObjectVar<NSError?>>()
                    audioRecorder = AVAudioRecorder(outputURL!!, settings, error.ptr)
                    error.value?.let { 
                        cleanupRecording()
                        return@withContext AudioRecordingResult.Error(
                            AudioRecordingError.ConfigurationError(Exception("Failed to create audio recorder: ${it.localizedDescription}"))
                        )
                    }
                    
                    val recorder = audioRecorder!!
                    if (!recorder.prepareToRecord()) {
                        cleanupRecording()
                        return@withContext AudioRecordingResult.Error(
                            AudioRecordingError.ConfigurationError(Exception("Failed to prepare audio recorder"))
                        )
                    }
                    
                    if (!recorder.record()) {
                        cleanupRecording()
                        return@withContext AudioRecordingResult.Error(
                            AudioRecordingError.ConfigurationError(Exception("Failed to start recording"))
                        )
                    }
                    
                    startTime.set((NSDate().timeIntervalSince1970 * 1000).toLong())
                    isRecording.set(true)
                    
                    notifyStateListeners { onRecordingStarted() }
                    AudioRecordingResult.Success
                }
                
            } catch (e: Exception) {
                cleanupRecording()
                AudioRecordingResult.Error(AudioRecordingError.Unknown(e))
            }
        }
    }
    
    override suspend fun stopRecording(): AudioFile? = withContext(Dispatchers.Main) {
        mutex.withLock {
            if (!isRecording.get()) {
                return@withLock null
            }
            
            try {
                val recorder = audioRecorder
                val url = outputURL
                
                if (recorder != null && url != null) {
                    recorder.stop()
                    
                    val duration = getDuration()
                    
                    // Get file size
                    val fileManager = NSFileManager.defaultManager
                    memScoped {
                        val error = alloc<ObjCObjectVar<NSError?>>()
                        val attributes = fileManager.attributesOfItemAtPath(url.path!!, error.ptr)
                        error.value?.let { 
                            cleanupRecording()
                            notifyStateListeners { onRecordingError(AudioRecordingError.IOError(Exception("Failed to get file attributes: ${it.localizedDescription}"))) }
                            return@withLock null
                        }
                        
                        val size = (attributes?.get(NSFileSize) as? NSNumber)?.longLongValue ?: 0L
                        
                        val audioFile = AudioFile(
                            path = url.path!!,
                            durationMs = duration,
                            sizeBytes = size
                        )
                        
                        cleanupRecording()
                        notifyStateListeners { onRecordingStopped() }
                        
                        return@withLock audioFile
                    }
                } else {
                    cleanupRecording()
                    notifyStateListeners { onRecordingError(AudioRecordingError.IOError(Exception("Invalid recorder or output URL"))) }
                    return@withLock null
                }
                
            } catch (e: Exception) {
                cleanupRecording()
                notifyStateListeners { onRecordingError(AudioRecordingError.IOError(e)) }
                return@withLock null
            }
        }
    }
    
    override suspend fun cancelRecording() = withContext(Dispatchers.Main) {
        mutex.withLock {
            if (!isRecording.get()) return@withLock
            
            try {
                audioRecorder?.stop()
                
                // Delete output file
                outputURL?.let { url ->
                    memScoped {
                        val error = alloc<ObjCObjectVar<NSError?>>()
                        NSFileManager.defaultManager.removeItemAtURL(url, error.ptr)
                        // Ignore deletion errors
                    }
                }
                
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
            pauseStartTime.set((NSDate().timeIntervalSince1970 * 1000).toLong())
            audioRecorder?.pause()
            notifyStateListeners { onRecordingPaused() }
        }
    }
    
    private fun resumeRecording() {
        if (isRecording.get() && isPaused.get()) {
            val pauseStart = pauseStartTime.get()
            if (pauseStart != null) {
                val pauseDuration = (NSDate().timeIntervalSince1970 * 1000).toLong() - pauseStart
                totalPauseDuration.set(totalPauseDuration.get() + pauseDuration)
            }
            
            isPaused.set(false)
            pauseStartTime.set(null)
            audioRecorder?.record()
            notifyStateListeners { onRecordingResumed() }
        }
    }
    
    private fun getDuration(): Long {
        val start = startTime.get() ?: return 0L
        val end = (NSDate().timeIntervalSince1970 * 1000).toLong()
        val totalPause = totalPauseDuration.get()
        return maxOf(0L, end - start - totalPause)
    }
    
    private fun cleanupRecording() {
        audioRecorder = null
        outputURL = null
        startTime.set(null)
        pauseStartTime.set(null)
        totalPauseDuration.set(0L)
        isRecording.set(false)
        isPaused.set(false)
        audioFocusManager.abandonAudioFocus()
        
        // Deactivate audio session
        memScoped {
            val error = alloc<ObjCObjectVar<NSError?>>()
            AVAudioSession.sharedInstance().setActive(false, error.ptr)
            // Ignore deactivation errors
        }
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
    
    fun cleanup() {
        scope.cancel()
        cleanupRecording()
    }
}

actual class AudioFocusManagerImpl : AudioFocusManager {
    
    private var currentListener: AudioFocusChangeListener? = null
    private var audioSessionObserver: NSObject? = null
    
    @OptIn(ExperimentalForeignApi::class)
    override fun requestAudioFocus(listener: AudioFocusChangeListener): Boolean {
        currentListener = listener
        
        // Set up audio session interruption notifications
        val notificationCenter = NSNotificationCenter.defaultCenter
        
        audioSessionObserver = notificationCenter.addObserverForName(
            name = AVAudioSessionInterruptionNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue
        ) { notification ->
            handleAudioSessionInterruption(notification)
        }
        
        return true // iOS doesn't require explicit audio focus request for recording
    }
    
    override fun abandonAudioFocus() {
        audioSessionObserver?.let { observer ->
            NSNotificationCenter.defaultCenter.removeObserver(observer)
        }
        audioSessionObserver = null
        currentListener = null
    }
    
    @OptIn(ExperimentalForeignApi::class)
    private fun handleAudioSessionInterruption(notification: NSNotification) {
        val userInfo = notification.userInfo ?: return
        val interruptionTypeNumber = userInfo[AVAudioSessionInterruptionTypeKey] as? NSNumber ?: return
        val interruptionType = interruptionTypeNumber.unsignedIntegerValue
        
        when (interruptionType.toULong()) {
            AVAudioSessionInterruptionTypeBegan -> {
                currentListener?.onAudioFocusChange(AudioFocusChange.LOSS_TRANSIENT)
            }
            AVAudioSessionInterruptionTypeEnded -> {
                val optionsNumber = userInfo[AVAudioSessionInterruptionOptionKey] as? NSNumber
                val shouldResume = optionsNumber?.unsignedIntegerValue?.and(AVAudioSessionInterruptionOptionShouldResume) != 0UL
                
                if (shouldResume) {
                    currentListener?.onAudioFocusChange(AudioFocusChange.GAIN)
                } else {
                    currentListener?.onAudioFocusChange(AudioFocusChange.LOSS)
                }
            }
        }
    }
}