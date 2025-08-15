package me.shadykhalifa.whispertop.domain.repositories

import kotlinx.coroutines.flow.Flow
import me.shadykhalifa.whispertop.domain.models.AudioFile
import me.shadykhalifa.whispertop.domain.models.RecordingState
import me.shadykhalifa.whispertop.utils.Result

interface AudioRepository {
    val recordingState: Flow<RecordingState>
    
    suspend fun startRecording(): Result<Unit>
    suspend fun stopRecording(): Result<AudioFile>
    suspend fun cancelRecording(): Result<Unit>
    fun isRecording(): Boolean
    suspend fun getLastRecording(): AudioFile?
}