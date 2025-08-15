package me.shadykhalifa.whispertop.data.services

import me.shadykhalifa.whispertop.data.repositories.AudioRecorder
import me.shadykhalifa.whispertop.domain.models.AudioFile
import me.shadykhalifa.whispertop.domain.services.AudioRecorderService

class AudioRecorderServiceImpl(
    private val audioRecorder: AudioRecorder
) : AudioRecorderService {
    
    override suspend fun startRecording() {
        audioRecorder.startRecording()
    }
    
    override suspend fun stopRecording(): AudioFile {
        return audioRecorder.stopRecording()
    }
    
    override suspend fun cancelRecording() {
        audioRecorder.cancelRecording()
    }
}