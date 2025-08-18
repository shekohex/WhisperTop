package me.shadykhalifa.whispertop.data.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import me.shadykhalifa.whispertop.domain.models.AudioFile
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

actual class Recorder {
    private val scope: CoroutineScope = CoroutineScope(
        Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    )
    private var recorder: AudioRecordThread? = null

    actual suspend fun startRecording(outputPath: String, onError: (Exception) -> Unit): Unit = withContext(scope.coroutineContext) {
        recorder = AudioRecordThread(File(outputPath), onError)
        recorder?.start()
    }

    actual suspend fun stopRecording(): AudioFile? = withContext(scope.coroutineContext) {
        val thread = recorder ?: return@withContext null
        thread.stopRecording()
        @Suppress("BlockingMethodInNonBlockingContext")
        thread.join()
        val outputFile = thread.outputFile
        recorder = null
        
        if (outputFile.exists()) {
            val duration = thread.getDuration()
            AudioFile(
                path = outputFile.absolutePath,
                durationMs = duration,
                sizeBytes = outputFile.length()
            )
        } else {
            null
        }
    }
}

private class AudioRecordThread(
    val outputFile: File,
    private val onError: (Exception) -> Unit
) : Thread("AudioRecorder") {
    private var quit = AtomicBoolean(false)
    private var startTime: Long = 0
    private var endTime: Long = 0

    @SuppressLint("MissingPermission")
    override fun run() {
        try {
            startTime = System.currentTimeMillis()
            val bufferSize = AudioRecord.getMinBufferSize(
                16000,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ) * 4
            val buffer = ShortArray(bufferSize / 2)

            val audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                16000,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            try {
                audioRecord.startRecording()

                val allData = mutableListOf<Short>()

                while (!quit.get()) {
                    val read = audioRecord.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        for (i in 0 until read) {
                            allData.add(buffer[i])
                        }
                    } else {
                        throw RuntimeException("audioRecord.read returned $read")
                    }
                }

                audioRecord.stop()
                endTime = System.currentTimeMillis()
                
                // Use WAV format for compatibility with the common interface
                // MediaRecorder-based implementation is preferred via AudioRecorderImpl
                WAVFileWriter().encodeWaveFile(outputFile.absolutePath, allData.toShortArray())
            } finally {
                audioRecord.release()
            }
        } catch (e: Exception) {
            endTime = System.currentTimeMillis()
            onError(e)
        }
    }

    fun stopRecording() {
        quit.set(true)
    }
    
    fun getDuration(): Long {
        return if (endTime > startTime) endTime - startTime else 0
    }
}