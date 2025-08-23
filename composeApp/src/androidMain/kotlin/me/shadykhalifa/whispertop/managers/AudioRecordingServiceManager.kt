package me.shadykhalifa.whispertop.managers

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import me.shadykhalifa.whispertop.domain.managers.AudioRecordingStateManager
import me.shadykhalifa.whispertop.domain.models.AudioFile
import me.shadykhalifa.whispertop.domain.models.RecordingState
import me.shadykhalifa.whispertop.service.AudioRecordingService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AudioRecordingServiceManager : KoinComponent {
    private val audioServiceManager: AudioServiceManager by inject()
    private val audioRecordingStateManager: AudioRecordingStateManager by inject()
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    
    // Expose domain state flows
    val connectionState = audioServiceManager.connectionState
    val recordingState: StateFlow<RecordingState> = audioRecordingStateManager.recordingState
    val recordingCompleteEvents = audioRecordingStateManager.recordingCompleteEvents
    val errorEvents = audioRecordingStateManager.errorEvents
    
    private var serviceStateListener: AudioRecordingService.RecordingStateListener? = null
    
    init {
        observeServiceConnection()
    }
    
    private fun observeServiceConnection() {
        scope.launch {
            connectionState.collect { state ->
                when (state) {
                    me.shadykhalifa.whispertop.domain.services.IAudioServiceManager.ServiceConnectionState.CONNECTED -> {
                        setupServiceStateListener()
                    }
                    me.shadykhalifa.whispertop.domain.services.IAudioServiceManager.ServiceConnectionState.DISCONNECTED -> {
                        removeServiceStateListener()
                        audioRecordingStateManager.resetToIdle()
                    }
                    me.shadykhalifa.whispertop.domain.services.IAudioServiceManager.ServiceConnectionState.CONNECTING -> {
                        /* CONNECTING - no action needed */
                    }
                }
            }
        }
    }
    
    private fun setupServiceStateListener() {
        val service = audioServiceManager.getServiceReference() ?: return
        
        serviceStateListener = object : AudioRecordingService.RecordingStateListener {
            override fun onStateChanged(state: AudioRecordingService.RecordingState) {
                val domainState = mapAndroidStateToDomainState(state, service)
                audioRecordingStateManager.updateRecordingState(domainState)
            }
            
            override fun onRecordingComplete(audioFile: AudioFile?) {
                audioRecordingStateManager.notifyRecordingComplete(audioFile)
            }
            
            override fun onRecordingError(error: String) {
                audioRecordingStateManager.notifyRecordingError(error)
            }
        }
        
        service.addStateListener(serviceStateListener!!)
        
        // Sync current state
        val currentServiceState = service.getCurrentState()
        val domainState = mapAndroidStateToDomainState(currentServiceState, service)
        audioRecordingStateManager.updateRecordingState(domainState)
    }
    
    private fun removeServiceStateListener() {
        serviceStateListener?.let { listener ->
            audioServiceManager.getServiceReference()?.removeStateListener(listener)
            serviceStateListener = null
        }
    }
    
    private fun mapAndroidStateToDomainState(
        androidState: AudioRecordingService.RecordingState,
        service: AudioRecordingService
    ): RecordingState {
        return when (androidState) {
            AudioRecordingService.RecordingState.IDLE -> RecordingState.Idle
            AudioRecordingService.RecordingState.RECORDING -> RecordingState.Recording(
                startTime = System.currentTimeMillis() - service.getRecordingDuration(),
                duration = service.getRecordingDuration()
            )
            AudioRecordingService.RecordingState.PAUSED -> RecordingState.Recording(
                startTime = System.currentTimeMillis() - service.getRecordingDuration(),
                duration = service.getRecordingDuration()
            )
            AudioRecordingService.RecordingState.PROCESSING -> RecordingState.Processing(progress = 0.5f)
        }
    }
    
    // Legacy API methods for backward compatibility
    fun startRecording(): RecordingActionResult {
        val service = audioServiceManager.getServiceReference()
        return if (service != null && audioServiceManager.isServiceBound()) {
            try {
                service.startRecording()
                RecordingActionResult.SUCCESS
            } catch (e: Exception) {
                audioRecordingStateManager.notifyRecordingError("Failed to start recording", e)
                RecordingActionResult.ERROR(e)
            }
        } else {
            RecordingActionResult.SERVICE_NOT_BOUND
        }
    }
    
    fun stopRecording(): RecordingActionResult {
        val service = audioServiceManager.getServiceReference()
        return if (service != null && audioServiceManager.isServiceBound()) {
            try {
                service.stopRecording()
                RecordingActionResult.SUCCESS
            } catch (e: Exception) {
                audioRecordingStateManager.notifyRecordingError("Failed to stop recording", e)
                RecordingActionResult.ERROR(e)
            }
        } else {
            RecordingActionResult.SERVICE_NOT_BOUND
        }
    }
    
    fun pauseRecording(): RecordingActionResult {
        val service = audioServiceManager.getServiceReference()
        return if (service != null && audioServiceManager.isServiceBound()) {
            try {
                service.pauseRecording()
                RecordingActionResult.SUCCESS
            } catch (e: Exception) {
                audioRecordingStateManager.notifyRecordingError("Failed to pause recording", e)
                RecordingActionResult.ERROR(e)
            }
        } else {
            RecordingActionResult.SERVICE_NOT_BOUND
        }
    }
    
    fun resumeRecording(): RecordingActionResult {
        val service = audioServiceManager.getServiceReference()
        return if (service != null && audioServiceManager.isServiceBound()) {
            try {
                service.resumeRecording()
                RecordingActionResult.SUCCESS
            } catch (e: Exception) {
                audioRecordingStateManager.notifyRecordingError("Failed to resume recording", e)
                RecordingActionResult.ERROR(e)
            }
        } else {
            RecordingActionResult.SERVICE_NOT_BOUND
        }
    }
    
    fun getRecordingDuration(): Long {
        return audioServiceManager.getServiceReference()?.getRecordingDuration() ?: 0L
    }
    
    fun getCurrentRecordingState(): AudioRecordingService.RecordingState {
        return audioServiceManager.getServiceReference()?.getCurrentState() 
            ?: AudioRecordingService.RecordingState.IDLE
    }
    
    fun cleanup() {
        scope.cancel()
        removeServiceStateListener()
        audioServiceManager.cleanup()
        audioRecordingStateManager.cleanup()
    }
    
    // Legacy enum for backward compatibility
    sealed class RecordingActionResult {
        object SUCCESS : RecordingActionResult()
        object SERVICE_NOT_BOUND : RecordingActionResult()
        data class ERROR(val exception: Exception) : RecordingActionResult()
    }
}