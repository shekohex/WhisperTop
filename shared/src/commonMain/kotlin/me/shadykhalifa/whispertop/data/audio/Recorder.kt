package me.shadykhalifa.whispertop.data.audio

import me.shadykhalifa.whispertop.domain.models.AudioFile

expect class Recorder() {
    suspend fun startRecording(outputPath: String, onError: (Exception) -> Unit): Unit
    suspend fun stopRecording(): AudioFile?
}