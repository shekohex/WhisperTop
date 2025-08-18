package me.shadykhalifa.whispertop.data.audio

import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

actual class WAVFileWriter {
    companion object {
        private const val TAG = "WAVFileWriter"
    }
    
    actual fun encodeWaveFile(outputPath: String, data: ShortArray) {
        val file = File(outputPath)
        
        // Validate input data
        if (data.isEmpty()) {
            Log.w(TAG, "encodeWaveFile: empty audio data")
            return
        }
        
        val dataSize = data.size * 2 // 2 bytes per sample
        val totalFileSize = dataSize + 44 // 44 byte header
        
        Log.d(TAG, "encodeWaveFile: samples=${data.size}, dataSize=${dataSize}B, totalSize=${totalFileSize}B, path=$outputPath")
        
        // Calculate audio statistics for validation
        var maxAmplitude = 0
        var minAmplitude = 0
        var sumSquares = 0.0
        for (sample in data) {
            if (sample > maxAmplitude) maxAmplitude = sample.toInt()
            if (sample < minAmplitude) minAmplitude = sample.toInt()
            sumSquares += (sample.toDouble() * sample.toDouble())
        }
        val rms = kotlin.math.sqrt(sumSquares / data.size)
        
        Log.d(TAG, "encodeWaveFile: audio stats - max=$maxAmplitude, min=$minAmplitude, rms=${rms.toInt()}")
        
        file.outputStream().use {
            val header = headerBytes(dataSize)
            it.write(header)
            
            val buffer = ByteBuffer.allocate(dataSize)
            buffer.order(ByteOrder.LITTLE_ENDIAN)
            buffer.asShortBuffer().put(data)
            buffer.rewind()
            val bytes = ByteArray(buffer.limit())
            buffer.get(bytes)
            it.write(bytes)
        }
        
        // Verify file was created successfully
        if (file.exists() && file.length() == totalFileSize.toLong()) {
            Log.d(TAG, "encodeWaveFile: WAV file created successfully, size=${file.length()}B")
        } else {
            Log.e(TAG, "encodeWaveFile: file creation failed or size mismatch. Expected: $totalFileSize, Actual: ${file.length()}")
        }
    }
    
    actual fun decodeWaveFile(filePath: String): FloatArray {
        val file = File(filePath)
        val baos = ByteArrayOutputStream()
        file.inputStream().use { it.copyTo(baos) }
        val buffer = ByteBuffer.wrap(baos.toByteArray())
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        val channel = buffer.getShort(22).toInt()
        buffer.position(44)
        val shortBuffer = buffer.asShortBuffer()
        val shortArray = ShortArray(shortBuffer.limit())
        shortBuffer.get(shortArray)
        return FloatArray(shortArray.size / channel) { index ->
            when (channel) {
                1 -> (shortArray[index] / 32767.0f).coerceIn(-1f..1f)
                else -> ((shortArray[2*index] + shortArray[2*index + 1])/ 32767.0f / 2.0f).coerceIn(-1f..1f)
            }
        }
    }
    
    private fun headerBytes(dataSize: Int): ByteArray {
        require(dataSize > 0) { "Data size must be positive" }
        
        val sampleRate = RecordingConstraints.SAMPLE_RATE
        val channels = RecordingConstraints.CHANNELS
        val bitsPerSample = RecordingConstraints.BITS_PER_SAMPLE
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        val chunkSize = dataSize + 36 // File size minus 8 bytes for RIFF header
        
        Log.d(TAG, "headerBytes: sampleRate=$sampleRate, channels=$channels, bitsPerSample=$bitsPerSample")
        Log.d(TAG, "headerBytes: byteRate=$byteRate, blockAlign=$blockAlign, dataSize=$dataSize, chunkSize=$chunkSize")
        
        return ByteBuffer.allocate(44).apply {
            order(ByteOrder.LITTLE_ENDIAN)

            // RIFF header
            put('R'.code.toByte())
            put('I'.code.toByte())
            put('F'.code.toByte())
            put('F'.code.toByte())
            putInt(chunkSize) // File size - 8 bytes

            // WAVE header
            put('W'.code.toByte())
            put('A'.code.toByte())
            put('V'.code.toByte())
            put('E'.code.toByte())

            // Format chunk
            put('f'.code.toByte())
            put('m'.code.toByte())
            put('t'.code.toByte())
            put(' '.code.toByte())
            putInt(16) // Format chunk size (PCM = 16)
            putShort(1.toShort()) // Audio format (1 = PCM)
            putShort(channels.toShort()) // Number of channels
            putInt(sampleRate) // Sample rate
            putInt(byteRate) // Byte rate (sampleRate * channels * bitsPerSample/8)
            putShort(blockAlign.toShort()) // Block align (channels * bitsPerSample/8)
            putShort(bitsPerSample.toShort()) // Bits per sample

            // Data chunk
            put('d'.code.toByte())
            put('a'.code.toByte())
            put('t'.code.toByte())
            put('a'.code.toByte())
            putInt(dataSize) // Data chunk size

            position(0)
        }.let {
            val bytes = ByteArray(it.limit())
            it.get(bytes)
            bytes
        }
    }
}