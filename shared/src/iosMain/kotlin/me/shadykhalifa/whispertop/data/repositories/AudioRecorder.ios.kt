package me.shadykhalifa.whispertop.data.repositories

import kotlinx.cinterop.*
import me.shadykhalifa.whispertop.domain.models.AudioFile
import platform.AVFAudio.*
import platform.Foundation.*

@OptIn(ExperimentalForeignApi::class)
actual class AudioRecorder {
    private var audioRecorder: AVAudioRecorder? = null
    private var outputURL: NSURL? = null
    private var startTime: Long = 0

    actual suspend fun startRecording() {
        if (audioRecorder != null) {
            throw IllegalStateException("Recording already in progress")
        }

        // Configure audio session
        val session = AVAudioSession.sharedInstance()
        memScoped {
            val error = alloc<ObjCObjectVar<NSError?>>()
            session.setCategory(AVAudioSessionCategoryRecord, error.ptr)
            error.value?.let { throw Exception("Failed to set audio session category: ${it.localizedDescription}") }
            
            session.setActive(true, error.ptr)
            error.value?.let { throw Exception("Failed to activate audio session: ${it.localizedDescription}") }
        }

        // Create output file URL
        val documentsPath = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory, 
            NSUserDomainMask, 
            true
        ).firstOrNull() as? String
            ?: throw Exception("Cannot access documents directory")
        
        val fileName = "recording_${kotlin.random.Random.nextLong()}.wav"
        val filePath = "$documentsPath/$fileName"
        outputURL = NSURL.fileURLWithPath(filePath)

        // Audio recorder settings
        val settings = mapOf<Any?, Any?>(
            AVFormatIDKey to 1819304813u, // kAudioFormatLinearPCM
            AVSampleRateKey to 44100.0,
            AVNumberOfChannelsKey to 1,
            AVLinearPCMBitDepthKey to 16,
            AVLinearPCMIsFloatKey to false,
            AVLinearPCMIsBigEndianKey to false
        )

        memScoped {
            val error = alloc<ObjCObjectVar<NSError?>>()
            try {
                audioRecorder = AVAudioRecorder(outputURL!!, settings, error.ptr)
                error.value?.let { throw Exception("Failed to create audio recorder: ${it.localizedDescription}") }
                
                val recorder = audioRecorder!!
                if (!recorder.prepareToRecord()) {
                    throw Exception("Failed to prepare audio recorder")
                }
                
                if (!recorder.record()) {
                    throw Exception("Failed to start recording")
                }
                
                startTime = (NSDate().timeIntervalSince1970 * 1000).toLong()
            } catch (e: Exception) {
                cleanup()
                throw Exception("Failed to start recording", e)
            }
        }
    }

    actual suspend fun stopRecording(): AudioFile {
        val recorder = audioRecorder ?: throw IllegalStateException("No recording in progress")
        val url = outputURL ?: throw IllegalStateException("No output file")
        
        try {
            recorder.stop()
            
            val duration = (NSDate().timeIntervalSince1970 * 1000).toLong() - startTime
            
            // Get file size
            val fileManager = NSFileManager.defaultManager
            memScoped {
                val error = alloc<ObjCObjectVar<NSError?>>()
                val attributes = fileManager.attributesOfItemAtPath(url.path!!, error.ptr)
                error.value?.let { throw Exception("Failed to get file attributes: ${it.localizedDescription}") }
                
                val size = (attributes?.get(NSFileSize) as? NSNumber)?.longLongValue ?: 0L
                
                return AudioFile(
                    path = url.path!!,
                    durationMs = duration,
                    sizeBytes = size
                )
            }
        } catch (e: Exception) {
            throw Exception("Failed to stop recording", e)
        } finally {
            cleanup()
        }
    }

    actual suspend fun cancelRecording() {
        try {
            audioRecorder?.stop()
            outputURL?.path?.let { path ->
                memScoped {
                    val error = alloc<ObjCObjectVar<NSError?>>()
                    NSFileManager.defaultManager.removeItemAtPath(path, error.ptr)
                    // Ignore removal errors during cancellation
                }
            }
        } catch (e: Exception) {
            // Ignore errors during cancellation
        } finally {
            cleanup()
        }
    }

    private fun cleanup() {
        audioRecorder = null
        outputURL = null
        startTime = 0
        
        // Deactivate audio session
        memScoped {
            val error = alloc<ObjCObjectVar<NSError?>>()
            AVAudioSession.sharedInstance().setActive(false, error.ptr)
            // Ignore deactivation errors
        }
    }
}