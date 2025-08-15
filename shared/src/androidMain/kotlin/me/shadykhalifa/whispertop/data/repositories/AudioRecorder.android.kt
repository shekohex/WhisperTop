package me.shadykhalifa.whispertop.data.repositories

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.shadykhalifa.whispertop.data.audio.AudioFileManager
import me.shadykhalifa.whispertop.data.audio.Recorder
import me.shadykhalifa.whispertop.domain.models.AudioFile
import java.io.File

actual class AudioRecorder(
    private val context: Context
) {
    private val recorder = Recorder()
    private val audioFileManager = AudioFileManager()
    private var outputFile: File? = null
    private var isRecording = false

    actual suspend fun startRecording(): Unit = withContext(Dispatchers.IO) {
        if (isRecording) {
            throw IllegalStateException("Recording already in progress")
        }

        val recordingsPath = audioFileManager.getRecordingsPath(context.cacheDir.absolutePath)
        val recordingsDir = File(recordingsPath)
        if (!recordingsDir.exists()) {
            recordingsDir.mkdirs()
        }

        val fileName = audioFileManager.generateFileName()
        outputFile = File(recordingsDir, fileName)
        
        try {
            isRecording = true
            recorder.startRecording(outputFile!!.absolutePath) { error ->
                isRecording = false
                // Error will be thrown in stopRecording if recording failed
            }
        } catch (e: Exception) {
            cleanup()
            throw Exception("Failed to start recording", e)
        }
    }

    actual suspend fun stopRecording(): AudioFile = withContext(Dispatchers.IO) {
        if (!isRecording) {
            throw IllegalStateException("No recording in progress")
        }
        
        try {
            isRecording = false
            val audioFile = recorder.stopRecording()
                ?: throw Exception("Failed to stop recording - no audio file generated")
            
            return@withContext audioFile
        } catch (e: Exception) {
            throw Exception("Failed to stop recording", e)
        } finally {
            cleanup()
        }
    }

    actual suspend fun cancelRecording(): Unit = withContext(Dispatchers.IO) {
        try {
            isRecording = false
            // Stop recording (this will try to complete the recording)
            recorder.stopRecording()
            // Delete the output file
            outputFile?.delete()
        } catch (e: Exception) {
            // Ignore errors during cancellation
        } finally {
            cleanup()
        }
    }

    private fun cleanup() {
        outputFile = null
        isRecording = false
    }
}