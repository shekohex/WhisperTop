package me.shadykhalifa.whispertop.managers

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
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
import me.shadykhalifa.whispertop.service.AudioRecordingService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.atomic.AtomicBoolean

class AudioServiceManager : KoinComponent {
    private val context: Context by inject()
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    
    private val _connectionState = MutableStateFlow(ServiceConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ServiceConnectionState> = _connectionState.asStateFlow()
    
    private val _recordingState = MutableStateFlow(AudioRecordingService.RecordingState.IDLE)
    val recordingState: StateFlow<AudioRecordingService.RecordingState> = _recordingState.asStateFlow()
    
    private val _errorEvents = Channel<String>(Channel.BUFFERED)
    val errorEvents = _errorEvents.receiveAsFlow()
    
    private val _recordingCompleteEvents = Channel<AudioFile?>(Channel.BUFFERED)
    val recordingCompleteEvents = _recordingCompleteEvents.receiveAsFlow()
    
    private var audioService: AudioRecordingService? = null
    private val isServiceBound = AtomicBoolean(false)
    private val isBinding = AtomicBoolean(false)
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? AudioRecordingService.AudioRecordingBinder
            audioService = binder?.getService()
            
            audioService?.let { audioService ->
                audioService.addStateListener(serviceStateListener)
                _recordingState.value = audioService.getCurrentState()
                isServiceBound.set(true)
                isBinding.set(false)
                _connectionState.value = ServiceConnectionState.CONNECTED
            }
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            audioService?.removeStateListener(serviceStateListener)
            audioService = null
            isServiceBound.set(false)
            isBinding.set(false)
            _connectionState.value = ServiceConnectionState.DISCONNECTED
            _recordingState.value = AudioRecordingService.RecordingState.IDLE
        }
    }
    
    private val serviceStateListener = object : AudioRecordingService.RecordingStateListener {
        override fun onStateChanged(state: AudioRecordingService.RecordingState) {
            _recordingState.value = state
        }
        
        override fun onRecordingComplete(audioFile: AudioFile?) {
            scope.launch {
                _recordingCompleteEvents.send(audioFile)
            }
        }
        
        override fun onRecordingError(error: String) {
            scope.launch {
                _errorEvents.send(error)
            }
        }
    }
    
    suspend fun bindService(): ServiceBindResult {
        if (isServiceBound.get() || isBinding.get()) {
            return ServiceBindResult.ALREADY_BOUND
        }
        
        return try {
            isBinding.set(true)
            _connectionState.value = ServiceConnectionState.CONNECTING
            
            val intent = Intent(context, AudioRecordingService::class.java)
            val bound = context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            
            if (bound) {
                ServiceBindResult.SUCCESS
            } else {
                isBinding.set(false)
                _connectionState.value = ServiceConnectionState.DISCONNECTED
                ServiceBindResult.FAILED
            }
        } catch (e: Exception) {
            isBinding.set(false)
            _connectionState.value = ServiceConnectionState.DISCONNECTED
            ServiceBindResult.ERROR(e)
        }
    }
    
    fun unbindService() {
        if (isServiceBound.get()) {
            try {
                audioService?.removeStateListener(serviceStateListener)
                context.unbindService(serviceConnection)
            } catch (e: Exception) {
                // Ignore unbind errors
            } finally {
                audioService = null
                isServiceBound.set(false)
                isBinding.set(false)
                _connectionState.value = ServiceConnectionState.DISCONNECTED
                _recordingState.value = AudioRecordingService.RecordingState.IDLE
            }
        }
    }
    
    fun startRecording(): RecordingActionResult {
        val service = audioService
        return if (service != null && isServiceBound.get()) {
            try {
                service.startRecording()
                RecordingActionResult.SUCCESS
            } catch (e: Exception) {
                RecordingActionResult.ERROR(e)
            }
        } else {
            RecordingActionResult.SERVICE_NOT_BOUND
        }
    }
    
    fun stopRecording(): RecordingActionResult {
        val service = audioService
        return if (service != null && isServiceBound.get()) {
            try {
                service.stopRecording()
                RecordingActionResult.SUCCESS
            } catch (e: Exception) {
                RecordingActionResult.ERROR(e)
            }
        } else {
            RecordingActionResult.SERVICE_NOT_BOUND
        }
    }
    
    fun pauseRecording(): RecordingActionResult {
        val service = audioService
        return if (service != null && isServiceBound.get()) {
            try {
                service.pauseRecording()
                RecordingActionResult.SUCCESS
            } catch (e: Exception) {
                RecordingActionResult.ERROR(e)
            }
        } else {
            RecordingActionResult.SERVICE_NOT_BOUND
        }
    }
    
    fun resumeRecording(): RecordingActionResult {
        val service = audioService
        return if (service != null && isServiceBound.get()) {
            try {
                service.resumeRecording()
                RecordingActionResult.SUCCESS
            } catch (e: Exception) {
                RecordingActionResult.ERROR(e)
            }
        } else {
            RecordingActionResult.SERVICE_NOT_BOUND
        }
    }
    
    fun getRecordingDuration(): Long {
        return audioService?.getRecordingDuration() ?: 0L
    }
    
    fun getCurrentRecordingState(): AudioRecordingService.RecordingState {
        return audioService?.getCurrentState() ?: AudioRecordingService.RecordingState.IDLE
    }
    
    fun cleanup() {
        scope.cancel()
        unbindService()
    }
    
    enum class ServiceConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }
    
    sealed class ServiceBindResult {
        object SUCCESS : ServiceBindResult()
        object FAILED : ServiceBindResult()
        object ALREADY_BOUND : ServiceBindResult()
        data class ERROR(val exception: Exception) : ServiceBindResult()
    }
    
    sealed class RecordingActionResult {
        object SUCCESS : RecordingActionResult()
        object SERVICE_NOT_BOUND : RecordingActionResult()
        data class ERROR(val exception: Exception) : RecordingActionResult()
    }
}