package me.shadykhalifa.whispertop.domain.services

import me.shadykhalifa.whispertop.domain.models.AudioFile

interface AudioRecorderService {
    suspend fun startRecording()
    suspend fun stopRecording(): AudioFile
    suspend fun cancelRecording()
}