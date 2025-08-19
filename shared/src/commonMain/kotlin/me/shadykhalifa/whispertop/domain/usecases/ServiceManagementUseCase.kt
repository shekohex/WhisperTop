package me.shadykhalifa.whispertop.domain.usecases

import kotlinx.coroutines.flow.Flow
import me.shadykhalifa.whispertop.domain.models.AudioFile
import me.shadykhalifa.whispertop.domain.repositories.ServiceStateRepository

class ServiceManagementUseCase(
    private val serviceStateRepository: ServiceStateRepository
) {
    val connectionState: Flow<ServiceStateRepository.ServiceConnectionState> = 
        serviceStateRepository.connectionState
    
    val recordingState: Flow<ServiceStateRepository.RecordingState> = 
        serviceStateRepository.recordingState
    
    val errorEvents: Flow<String> = 
        serviceStateRepository.errorEvents
    
    val recordingCompleteEvents: Flow<AudioFile?> = 
        serviceStateRepository.recordingCompleteEvents
    
    suspend fun bindService(): ServiceStateRepository.ServiceBindResult {
        return serviceStateRepository.bindService()
    }
    
    fun getCurrentRecordingState(): ServiceStateRepository.RecordingState {
        return serviceStateRepository.getCurrentRecordingState()
    }
    
    fun getRecordingDuration(): Long {
        return serviceStateRepository.getRecordingDuration()
    }
    
    fun cleanup() {
        serviceStateRepository.cleanup()
    }
}