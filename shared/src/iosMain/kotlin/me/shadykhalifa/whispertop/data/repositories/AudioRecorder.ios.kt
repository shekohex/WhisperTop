package me.shadykhalifa.whispertop.data.repositories

import me.shadykhalifa.whispertop.data.audio.AudioFileManager
import me.shadykhalifa.whispertop.data.audio.Recorder
import me.shadykhalifa.whispertop.domain.models.AudioFile
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSUserDomainMask

actual class AudioRecorder {
    private val recorder = Recorder()
    private val audioFileManager = AudioFileManager()
    private var outputPath: String? = null
    private var isRecording = false

    actual suspend fun startRecording(): Unit {
        if (isRecording) {
            throw IllegalStateException("Recording already in progress")
        }

        // Get documents directory
        val documentsPath = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory, 
            NSUserDomainMask, 
            true
        ).firstOrNull() as? String
            ?: throw Exception("Cannot access documents directory")
        
        val recordingsPath = audioFileManager.getRecordingsPath(documentsPath)
        val fileName = audioFileManager.generateFileName()
        outputPath = "$recordingsPath/$fileName"
        
        try {
            isRecording = true
            recorder.startRecording(outputPath!!) { error ->
                isRecording = false
                // Error will be thrown in stopRecording if recording failed
            }
        } catch (e: Exception) {
            cleanup()
            throw Exception("Failed to start recording", e)
        }
    }

    actual suspend fun stopRecording(): AudioFile {
        if (!isRecording) {
            throw IllegalStateException("No recording in progress")
        }
        
        try {
            isRecording = false
            val audioFile = recorder.stopRecording()
                ?: throw Exception("Failed to stop recording - no audio file generated")
            
            return audioFile
        } catch (e: Exception) {
            throw Exception("Failed to stop recording", e)
        } finally {
            cleanup()
        }
    }

    actual suspend fun cancelRecording(): Unit {
        try {
            isRecording = false
            // Stop recording (this will try to complete the recording)
            recorder.stopRecording()
            // Try to delete the output file
            outputPath?.let { path ->
                // Note: File deletion would be platform-specific
                // For now, we just clear the path reference
            }
        } catch (e: Exception) {
            // Ignore errors during cancellation
        } finally {
            cleanup()
        }
    }

    private fun cleanup() {
        outputPath = null
        isRecording = false
    }
}