package me.shadykhalifa.whispertop.domain.usecases

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.shadykhalifa.whispertop.domain.models.AudioFile
import me.shadykhalifa.whispertop.domain.models.RecordingState
import me.shadykhalifa.whispertop.domain.models.ServiceConnectionState
import me.shadykhalifa.whispertop.domain.repositories.ServiceStateRepository
import me.shadykhalifa.whispertop.utils.TimeUtils

class ServiceManagementUseCase(
    private val serviceStateRepository: ServiceStateRepository
) {
    val connectionState: Flow<ServiceConnectionState> = 
        serviceStateRepository.connectionState.map { repoState ->
            when (repoState) {
                ServiceStateRepository.ServiceConnectionState.CONNECTED -> ServiceConnectionState.CONNECTED
                ServiceStateRepository.ServiceConnectionState.CONNECTING -> ServiceConnectionState.CONNECTING
                ServiceStateRepository.ServiceConnectionState.DISCONNECTED -> ServiceConnectionState.DISCONNECTED
            }
        }
    
    val recordingState: Flow<RecordingState> = 
        serviceStateRepository.recordingState.map { repoState ->
            when (repoState) {
                ServiceStateRepository.RecordingState.IDLE -> RecordingState.Idle
                ServiceStateRepository.RecordingState.RECORDING -> RecordingState.Recording(
                    startTime = TimeUtils.currentTimeMillis(),
                    duration = 0L
                )
                ServiceStateRepository.RecordingState.PROCESSING -> RecordingState.Processing(0f)
                ServiceStateRepository.RecordingState.PAUSED -> RecordingState.Processing(0f)
            }
        }
    
    val errorEvents: Flow<String> = 
        serviceStateRepository.errorEvents
    
    val recordingCompleteEvents: Flow<AudioFile?> = 
        serviceStateRepository.recordingCompleteEvents
    
    suspend fun bindService(): ServiceStateRepository.ServiceBindResult {
        return serviceStateRepository.bindService()
    }
    
    fun getCurrentRecordingState(): RecordingState {
        val repoState = serviceStateRepository.getCurrentRecordingState()
        return when (repoState) {
            ServiceStateRepository.RecordingState.IDLE -> RecordingState.Idle
            ServiceStateRepository.RecordingState.RECORDING -> RecordingState.Recording(
                startTime = TimeUtils.currentTimeMillis(),
                duration = 0L
            )
            ServiceStateRepository.RecordingState.PROCESSING -> RecordingState.Processing(0f)
            ServiceStateRepository.RecordingState.PAUSED -> RecordingState.Processing(0f)
        }
    }
    
    fun getRecordingDuration(): Long {
        return serviceStateRepository.getRecordingDuration()
    }
    
    fun cleanup() {
        serviceStateRepository.cleanup()
    }
}