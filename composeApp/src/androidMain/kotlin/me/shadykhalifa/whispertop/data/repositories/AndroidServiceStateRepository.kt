package me.shadykhalifa.whispertop.data.repositories

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.shadykhalifa.whispertop.domain.models.AudioFile
import me.shadykhalifa.whispertop.domain.repositories.ServiceStateRepository
import me.shadykhalifa.whispertop.managers.AudioRecordingServiceManager
import me.shadykhalifa.whispertop.service.AudioRecordingService
import me.shadykhalifa.whispertop.domain.services.IAudioServiceManager

class AndroidServiceStateRepository(
    private val audioRecordingServiceManager: AudioRecordingServiceManager
) : ServiceStateRepository {
    
    override val connectionState: Flow<ServiceStateRepository.ServiceConnectionState> = 
        audioRecordingServiceManager.connectionState.map { state ->
            when (state) {
                IAudioServiceManager.ServiceConnectionState.DISCONNECTED -> 
                    ServiceStateRepository.ServiceConnectionState.DISCONNECTED
                IAudioServiceManager.ServiceConnectionState.CONNECTING -> 
                    ServiceStateRepository.ServiceConnectionState.CONNECTING
                IAudioServiceManager.ServiceConnectionState.CONNECTED -> 
                    ServiceStateRepository.ServiceConnectionState.CONNECTED
            }
        }
    
    override val recordingState: Flow<ServiceStateRepository.RecordingState> = 
        audioRecordingServiceManager.recordingState.map { domainState ->
            when (domainState) {
                is me.shadykhalifa.whispertop.domain.models.RecordingState.Idle -> 
                    ServiceStateRepository.RecordingState.IDLE
                is me.shadykhalifa.whispertop.domain.models.RecordingState.Recording -> 
                    ServiceStateRepository.RecordingState.RECORDING
                is me.shadykhalifa.whispertop.domain.models.RecordingState.Processing -> 
                    ServiceStateRepository.RecordingState.PROCESSING
                is me.shadykhalifa.whispertop.domain.models.RecordingState.Success -> 
                    ServiceStateRepository.RecordingState.IDLE
                is me.shadykhalifa.whispertop.domain.models.RecordingState.Error -> 
                    ServiceStateRepository.RecordingState.IDLE
            }
        }
    
    override val errorEvents: Flow<String> = audioRecordingServiceManager.errorEvents
    
    override val recordingCompleteEvents: Flow<AudioFile?> = audioRecordingServiceManager.recordingCompleteEvents
    
    override suspend fun bindService(): ServiceStateRepository.ServiceBindResult {
        // For now, we need to access the underlying AudioServiceManager through the manager
        // This method should ideally use a use case, but keeping legacy API for compatibility
        return ServiceStateRepository.ServiceBindResult.SUCCESS
    }
    
    override fun getCurrentRecordingState(): ServiceStateRepository.RecordingState {
        return when (audioRecordingServiceManager.getCurrentRecordingState()) {
            AudioRecordingService.RecordingState.IDLE -> 
                ServiceStateRepository.RecordingState.IDLE
            AudioRecordingService.RecordingState.RECORDING -> 
                ServiceStateRepository.RecordingState.RECORDING
            AudioRecordingService.RecordingState.PAUSED -> 
                ServiceStateRepository.RecordingState.PAUSED
            AudioRecordingService.RecordingState.PROCESSING -> 
                ServiceStateRepository.RecordingState.PROCESSING
        }
    }
    
    override fun getRecordingDuration(): Long {
        return audioRecordingServiceManager.getRecordingDuration()
    }
    
    override fun cleanup() {
        audioRecordingServiceManager.cleanup()
    }
}