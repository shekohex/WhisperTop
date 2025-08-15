package me.shadykhalifa.whispertop.data.repositories

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.shadykhalifa.whispertop.domain.models.AudioFile
import java.io.File
import java.io.IOException

actual class AudioRecorder(
    private val context: Context
) {
    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startTime: Long = 0

    actual suspend fun startRecording() = withContext(Dispatchers.IO) {
        if (mediaRecorder != null) {
            throw IllegalStateException("Recording already in progress")
        }

        val cacheDir = File(context.cacheDir, "recordings")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }

        outputFile = File(cacheDir, "recording_${System.currentTimeMillis()}.wav")
        
        try {
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(outputFile!!.absolutePath)
                
                prepare()
                start()
                startTime = System.currentTimeMillis()
            }
        } catch (e: IOException) {
            cleanup()
            throw Exception("Failed to start recording", e)
        }
    }

    actual suspend fun stopRecording(): AudioFile = withContext(Dispatchers.IO) {
        val recorder = mediaRecorder ?: throw IllegalStateException("No recording in progress")
        val file = outputFile ?: throw IllegalStateException("No output file")
        
        try {
            recorder.stop()
            recorder.release()
            
            val duration = System.currentTimeMillis() - startTime
            val size = file.length()
            
            AudioFile(
                path = file.absolutePath,
                durationMs = duration,
                sizeBytes = size
            )
        } catch (e: Exception) {
            throw Exception("Failed to stop recording", e)
        } finally {
            cleanup()
        }
    }

    actual suspend fun cancelRecording(): Unit = withContext(Dispatchers.IO) {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            outputFile?.delete()
        } catch (e: Exception) {
            // Ignore errors during cancellation
        } finally {
            cleanup()
        }
    }

    private fun cleanup() {
        mediaRecorder = null
        outputFile = null
        startTime = 0
    }
}