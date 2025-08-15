package me.shadykhalifa.whispertop.data.audio

import kotlinx.cinterop.*
import platform.Foundation.*
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
actual class WAVFileWriter {
    
    actual fun encodeWaveFile(outputPath: String, data: ShortArray) {
        val headerBytes = generateWAVHeader(data.size * 2)
        val audioBytes = shortArrayToByteArray(data)
        val completeData = headerBytes + audioBytes
        
        val nsData = completeData.toNSData()
        val success = nsData.writeToFile(outputPath, atomically = true)
        if (!success) {
            throw Exception("Failed to write WAV file to path: $outputPath")
        }
    }
    
    actual fun decodeWaveFile(filePath: String): FloatArray {
        val nsData = NSData.dataWithContentsOfFile(filePath) 
            ?: throw Exception("Failed to read file: $filePath")
        
        val bytes = nsData.toByteArray()
        if (bytes.size < 44) {
            throw Exception("Invalid WAV file: too small")
        }
        
        // Extract channel info from header (byte 22-23, little-endian)
        val channels = (bytes[22].toInt() and 0xFF) or ((bytes[23].toInt() and 0xFF) shl 8)
        
        // Skip 44-byte header and convert to short array
        val audioBytes = bytes.sliceArray(44 until bytes.size)
        val shortArray = byteArrayToShortArray(audioBytes)
        
        return FloatArray(shortArray.size / channels) { index ->
            when (channels) {
                1 -> (shortArray[index] / 32767.0f).coerceIn(-1f..1f)
                else -> ((shortArray[2*index] + shortArray[2*index + 1]) / 32767.0f / 2.0f).coerceIn(-1f..1f)
            }
        }
    }
    
    private fun generateWAVHeader(dataSize: Int): ByteArray {
        val totalSize = dataSize + 36
        return byteArrayOf(
            // RIFF header
            'R'.code.toByte(), 'I'.code.toByte(), 'F'.code.toByte(), 'F'.code.toByte(),
            // File size - 8
            (totalSize and 0xFF).toByte(),
            ((totalSize shr 8) and 0xFF).toByte(),
            ((totalSize shr 16) and 0xFF).toByte(),
            ((totalSize shr 24) and 0xFF).toByte(),
            // WAVE
            'W'.code.toByte(), 'A'.code.toByte(), 'V'.code.toByte(), 'E'.code.toByte(),
            // fmt chunk
            'f'.code.toByte(), 'm'.code.toByte(), 't'.code.toByte(), ' '.code.toByte(),
            // fmt chunk size (16)
            16, 0, 0, 0,
            // Audio format (PCM = 1)
            1, 0,
            // Channels (1)
            1, 0,
            // Sample rate (16000)
            (16000 and 0xFF).toByte(),
            ((16000 shr 8) and 0xFF).toByte(),
            ((16000 shr 16) and 0xFF).toByte(),
            ((16000 shr 24) and 0xFF).toByte(),
            // Byte rate (32000)
            (32000 and 0xFF).toByte(),
            ((32000 shr 8) and 0xFF).toByte(),
            ((32000 shr 16) and 0xFF).toByte(),
            ((32000 shr 24) and 0xFF).toByte(),
            // Block align (2)
            2, 0,
            // Bits per sample (16)
            16, 0,
            // data chunk
            'd'.code.toByte(), 'a'.code.toByte(), 't'.code.toByte(), 'a'.code.toByte(),
            // Data size
            (dataSize and 0xFF).toByte(),
            ((dataSize shr 8) and 0xFF).toByte(),
            ((dataSize shr 16) and 0xFF).toByte(),
            ((dataSize shr 24) and 0xFF).toByte()
        )
    }
    
    private fun shortArrayToByteArray(shortArray: ShortArray): ByteArray {
        val byteArray = ByteArray(shortArray.size * 2)
        for (i in shortArray.indices) {
            val value = shortArray[i]
            byteArray[i * 2] = (value.toInt() and 0xFF).toByte()
            byteArray[i * 2 + 1] = ((value.toInt() shr 8) and 0xFF).toByte()
        }
        return byteArray
    }
    
    private fun byteArrayToShortArray(byteArray: ByteArray): ShortArray {
        val shortArray = ShortArray(byteArray.size / 2)
        for (i in shortArray.indices) {
            val low = byteArray[i * 2].toInt() and 0xFF
            val high = (byteArray[i * 2 + 1].toInt() and 0xFF) shl 8
            shortArray[i] = (low or high).toShort()
        }
        return shortArray
    }
    
    private fun ByteArray.toNSData(): NSData {
        return this.usePinned { pinned ->
            NSData.dataWithBytes(pinned.addressOf(0), this.size.toULong())
        }
    }
    
    private fun NSData.toByteArray(): ByteArray {
        return ByteArray(this.length.toInt()).apply {
            usePinned { pinned ->
                memcpy(pinned.addressOf(0), this@toByteArray.bytes, this@toByteArray.length)
            }
        }
    }
}