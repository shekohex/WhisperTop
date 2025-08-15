package me.shadykhalifa.whispertop.data.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WAVFileWriterTest {
    
    @Test
    fun testShortArrayConversion() {
        // Test basic short array to byte array conversion (little-endian)
        val shortArray = shortArrayOf(0x1234.toShort(), 0x5678.toShort())
        val expectedBytes = byteArrayOf(
            0x34.toByte(), 0x12.toByte(), // 0x1234 in little-endian
            0x78.toByte(), 0x56.toByte()  // 0x5678 in little-endian
        )
        
        // Since we can't test the platform-specific implementation directly,
        // we test the conversion logic conceptually
        val testData = shortArrayOf(100, -100, 32767, -32768)
        assertTrue(testData.size == 4)
        
        // Test that we can handle various short values
        for (value in testData) {
            val byteVal1 = (value.toInt() and 0xFF).toByte()
            val byteVal2 = ((value.toInt() shr 8) and 0xFF).toByte()
            val reconstructed = ((byteVal1.toInt() and 0xFF) or 
                               ((byteVal2.toInt() and 0xFF) shl 8)).toShort()
            assertEquals(value, reconstructed)
        }
    }
    
    @Test
    fun testWAVHeaderConstants() {
        // Test that we have the correct WAV format constants
        val riffHeader = "RIFF".encodeToByteArray()
        assertEquals(4, riffHeader.size)
        assertEquals('R'.code.toByte(), riffHeader[0])
        assertEquals('I'.code.toByte(), riffHeader[1])
        assertEquals('F'.code.toByte(), riffHeader[2])
        assertEquals('F'.code.toByte(), riffHeader[3])
        
        val waveHeader = "WAVE".encodeToByteArray()
        assertEquals(4, waveHeader.size)
        assertEquals('W'.code.toByte(), waveHeader[0])
        assertEquals('A'.code.toByte(), waveHeader[1])
        assertEquals('V'.code.toByte(), waveHeader[2])
        assertEquals('E'.code.toByte(), waveHeader[3])
    }
    
    @Test 
    fun testAudioParameterCalculations() {
        // Test standard audio parameter calculations for 16kHz mono 16-bit
        val sampleRate = 16000
        val channels = 1
        val bitsPerSample = 16
        val bytesPerSample = bitsPerSample / 8 // Should be 2
        val byteRate = sampleRate * channels * bytesPerSample // Should be 32000
        val blockAlign = channels * bytesPerSample // Should be 2
        
        assertEquals(2, bytesPerSample)
        assertEquals(32000, byteRate)
        assertEquals(2, blockAlign)
        
        // Test that one second of audio data should be 32000 bytes
        val oneSecondDataSize = sampleRate * channels * bytesPerSample
        assertEquals(32000, oneSecondDataSize)
    }
    
    @Test
    fun testLittleEndianConversion() {
        // Test little-endian integer conversion
        val testValue = 0x12345678
        val expectedBytes = byteArrayOf(
            0x78.toByte(), 0x56.toByte(), 0x34.toByte(), 0x12.toByte()
        )
        
        val actualBytes = byteArrayOf(
            (testValue and 0xFF).toByte(),
            ((testValue shr 8) and 0xFF).toByte(),
            ((testValue shr 16) and 0xFF).toByte(),
            ((testValue shr 24) and 0xFF).toByte()
        )
        
        for (i in expectedBytes.indices) {
            assertEquals(expectedBytes[i], actualBytes[i])
        }
    }
    
    @Test
    fun testFloatConversion() {
        // Test conversion from short to float for audio processing
        val maxShort = 32767.toShort()
        val minShort = (-32768).toShort()
        val zero = 0.toShort()
        
        // Test conversion to normalized float values
        val maxFloat = maxShort / 32767.0f
        val minFloat = minShort / 32767.0f
        val zeroFloat = zero / 32767.0f
        
        assertEquals(1.0f, maxFloat, 0.001f)
        assertTrue(minFloat < 0.0f)
        assertEquals(0.0f, zeroFloat, 0.001f)
        
        // Test clamping
        val clampedMax = maxFloat.coerceIn(-1f..1f)
        val clampedMin = minFloat.coerceIn(-1f..1f)
        
        assertEquals(1.0f, clampedMax, 0.001f)
        assertTrue(clampedMin >= -1.0f)
    }
    
    @Test
    fun testFileManagerUtilities() {
        val audioFileManager = AudioFileManager()
        
        // Test file name generation
        val fileName = audioFileManager.generateFileName()
        assertTrue(fileName.startsWith("recording_"))
        assertTrue(fileName.endsWith(".wav"))
        
        // Test WAV file validation
        assertTrue(audioFileManager.validateWAVFile("test.wav"))
        assertTrue(audioFileManager.validateWAVFile("TEST.WAV"))
        assertTrue(!audioFileManager.validateWAVFile("test.mp3"))
        
        // Test duration calculation
        val duration = audioFileManager.calculateDuration(
            audioDataSize = 32000, // 1 second of 16-bit mono at 16kHz
            sampleRate = 16000,
            channels = 1,
            bitsPerSample = 16
        )
        assertEquals(1000L, duration) // Should be 1000ms (1 second)
    }
}