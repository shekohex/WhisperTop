package me.shadykhalifa.whispertop.data.repositories

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.shadykhalifa.whispertop.domain.models.AudioFile
import me.shadykhalifa.whispertop.domain.repositories.ServiceStateRepository
import me.shadykhalifa.whispertop.managers.AudioServiceManager
import me.shadykhalifa.whispertop.service.AudioRecordingService

class AndroidServiceStateRepository(
    private val audioServiceManager: AudioServiceManager
) : ServiceStateRepository {
    
    override val connectionState: Flow<ServiceStateRepository.ServiceConnectionState> = 
        audioServiceManager.connectionState.map { state ->
            when (state) {
                AudioServiceManager.ServiceConnectionState.DISCONNECTED -> 
                    ServiceStateRepository.ServiceConnectionState.DISCONNECTED
                AudioServiceManager.ServiceConnectionState.CONNECTING -> 
                    ServiceStateRepository.ServiceConnectionState.CONNECTING
                AudioServiceManager.ServiceConnectionState.CONNECTED -> 
                    ServiceStateRepository.ServiceConnectionState.CONNECTED
            }
        }
    
    override val recordingState: Flow<ServiceStateRepository.RecordingState> = 
        audioServiceManager.recordingState.map { state ->
            when (state) {
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
    
    override val errorEvents: Flow<String> = audioServiceManager.errorEvents
    
    override val recordingCompleteEvents: Flow<AudioFile?> = audioServiceManager.recordingCompleteEvents
    
    override suspend fun bindService(): ServiceStateRepository.ServiceBindResult {
        return when (val result = audioServiceManager.bindService()) {
            is AudioServiceManager.ServiceBindResult.SUCCESS -> 
                ServiceStateRepository.ServiceBindResult.SUCCESS
            is AudioServiceManager.ServiceBindResult.ALREADY_BOUND -> 
                ServiceStateRepository.ServiceBindResult.ALREADY_BOUND
            is AudioServiceManager.ServiceBindResult.FAILED -> 
                ServiceStateRepository.ServiceBindResult.FAILED
            is AudioServiceManager.ServiceBindResult.ERROR -> 
                ServiceStateRepository.ServiceBindResult.ERROR(result.exception)
        }
    }
    
    override fun getCurrentRecordingState(): ServiceStateRepository.RecordingState {
        return when (audioServiceManager.getCurrentRecordingState()) {
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
        return audioServiceManager.getRecordingDuration()
    }
    
    override fun cleanup() {
        audioServiceManager.cleanup()
    }
}