package me.shadykhalifa.whispertop.managers

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.shadykhalifa.whispertop.domain.services.IAudioServiceManager
import me.shadykhalifa.whispertop.service.AudioRecordingService
import me.shadykhalifa.whispertop.utils.Result
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.atomic.AtomicBoolean

class AudioServiceManager : IAudioServiceManager, KoinComponent {
    private val context: Context by inject()
    
    private val _connectionState = MutableStateFlow(IAudioServiceManager.ServiceConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<IAudioServiceManager.ServiceConnectionState> = _connectionState.asStateFlow()
    
    private var audioService: AudioRecordingService? = null
    private val isServiceBound = AtomicBoolean(false)
    private val isBinding = AtomicBoolean(false)
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? AudioRecordingService.AudioRecordingBinder
            audioService = binder?.getService()
            isServiceBound.set(true)
            isBinding.set(false)
            _connectionState.value = IAudioServiceManager.ServiceConnectionState.CONNECTED
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            audioService = null
            isServiceBound.set(false)
            isBinding.set(false)
            _connectionState.value = IAudioServiceManager.ServiceConnectionState.DISCONNECTED
        }
    }
    
    override suspend fun bindService(): Result<IAudioServiceManager.ServiceBindResult> {
        if (isServiceBound.get() || isBinding.get()) {
            return Result.Success(IAudioServiceManager.ServiceBindResult.AlreadyBound)
        }
        
        return try {
            isBinding.set(true)
            _connectionState.value = IAudioServiceManager.ServiceConnectionState.CONNECTING
            
            val intent = Intent(context, AudioRecordingService::class.java)
            val bound = context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            
            if (bound) {
                Result.Success(IAudioServiceManager.ServiceBindResult.Success)
            } else {
                resetConnectionState()
                Result.Success(IAudioServiceManager.ServiceBindResult.Failed)
            }
        } catch (e: Exception) {
            resetConnectionState()
            Result.Success(IAudioServiceManager.ServiceBindResult.Error(e))
        }
    }
    
    override fun unbindService() {
        if (isServiceBound.get()) {
            try {
                context.unbindService(serviceConnection)
            } catch (e: Exception) {
                // Ignore unbind errors
            } finally {
                resetServiceState()
            }
        }
    }
    
    override fun isServiceBound(): Boolean = isServiceBound.get()
    
    override fun cleanup() {
        unbindService()
    }
    
    fun getServiceReference(): AudioRecordingService? = audioService
    
    private fun resetConnectionState() {
        isBinding.set(false)
        _connectionState.value = IAudioServiceManager.ServiceConnectionState.DISCONNECTED
    }
    
    private fun resetServiceState() {
        audioService = null
        isServiceBound.set(false)
        isBinding.set(false)
        _connectionState.value = IAudioServiceManager.ServiceConnectionState.DISCONNECTED
    }
}