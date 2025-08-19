package me.shadykhalifa.whispertop.domain.repositories

import kotlinx.coroutines.flow.Flow
import me.shadykhalifa.whispertop.domain.models.AudioFile

interface ServiceStateRepository {
    val connectionState: Flow<ServiceConnectionState>
    val recordingState: Flow<RecordingState>
    val errorEvents: Flow<String>
    val recordingCompleteEvents: Flow<AudioFile?>
    
    suspend fun bindService(): ServiceBindResult
    fun getCurrentRecordingState(): RecordingState
    fun getRecordingDuration(): Long
    fun cleanup()
    
    sealed class ServiceConnectionState {
        object DISCONNECTED : ServiceConnectionState()
        object CONNECTING : ServiceConnectionState()
        object CONNECTED : ServiceConnectionState()
    }
    
    sealed class ServiceBindResult {
        object SUCCESS : ServiceBindResult()
        object ALREADY_BOUND : ServiceBindResult()
        object FAILED : ServiceBindResult()
        data class ERROR(val exception: Exception) : ServiceBindResult()
    }
    
    sealed class RecordingState {
        object IDLE : RecordingState()
        object RECORDING : RecordingState()
        object PAUSED : RecordingState()
        object PROCESSING : RecordingState()
    }
}