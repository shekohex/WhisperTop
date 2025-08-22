package me.shadykhalifa.whispertop.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import me.shadykhalifa.whispertop.data.audio.AudioProcessorImpl
import me.shadykhalifa.whispertop.data.audio.WAVFileWriterImpl
import me.shadykhalifa.whispertop.domain.models.AudioFormat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class AudioProcessingBenchmark {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    
    private val audioProcessor = AudioProcessorImpl()
    private val wavWriter = WAVFileWriterImpl()
    
    private val testAudioFormat = AudioFormat(
        sampleRate = 16000,
        channelCount = 1,
        bitDepth = 16
    )

    @Test
    fun benchmarkAudioProcessing_smallBuffer() = runBlocking {
        val smallBuffer = generateRandomAudioData(1024) // 1KB buffer
        
        benchmarkRule.measureRepeated {
            // Baseline: Small buffer processing should complete < 5ms
            val startTime = System.nanoTime()
            audioProcessor.processAudioData(smallBuffer, testAudioFormat)
            val duration = (System.nanoTime() - startTime) / 1_000_000 // Convert to ms
            
            // Assert performance baseline (will be visible in benchmark reports)
            assert(duration < 5) { "Small buffer processing took ${duration}ms, exceeds 5ms baseline" }
        }
    }

    @Test
    fun benchmarkAudioProcessing_mediumBuffer() = runBlocking {
        val mediumBuffer = generateRandomAudioData(16384) // 16KB buffer
        
        benchmarkRule.measureRepeated {
            // Baseline: Medium buffer processing should complete < 20ms
            val startTime = System.nanoTime()
            audioProcessor.processAudioData(mediumBuffer, testAudioFormat)
            val duration = (System.nanoTime() - startTime) / 1_000_000
            
            assert(duration < 20) { "Medium buffer processing took ${duration}ms, exceeds 20ms baseline" }
        }
    }

    @Test
    fun benchmarkAudioProcessing_largeBuffer() = runBlocking {
        val largeBuffer = generateRandomAudioData(65536) // 64KB buffer
        
        benchmarkRule.measureRepeated {
            // Baseline: Large buffer processing should complete < 100ms
            val startTime = System.nanoTime()
            audioProcessor.processAudioData(largeBuffer, testAudioFormat)
            val duration = (System.nanoTime() - startTime) / 1_000_000
            
            assert(duration < 100) { "Large buffer processing took ${duration}ms, exceeds 100ms baseline" }
        }
    }

    @Test
    fun benchmarkWAVFileCreation_small() = runBlocking {
        val audioData = generateRandomAudioData(8000) // ~0.5 seconds at 16kHz
        val tempFile = File.createTempFile("benchmark_small", ".wav", context.cacheDir)
        
        try {
            benchmarkRule.measureRepeated {
                // Baseline: Small WAV file creation should complete < 10ms
                val startTime = System.nanoTime()
                runBlocking {
                    wavWriter.writeWAVFile(audioData, testAudioFormat, tempFile)
                }
                val duration = (System.nanoTime() - startTime) / 1_000_000
                
                assert(duration < 10) { "Small WAV creation took ${duration}ms, exceeds 10ms baseline" }
            }
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun benchmarkWAVFileCreation_medium() = runBlocking {
        val audioData = generateRandomAudioData(80000) // ~5 seconds at 16kHz
        val tempFile = File.createTempFile("benchmark_medium", ".wav", context.cacheDir)
        
        try {
            benchmarkRule.measureRepeated {
                runBlocking {
                    wavWriter.writeWAVFile(audioData, testAudioFormat, tempFile)
                }
            }
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun benchmarkWAVFileCreation_large() = runBlocking {
        val audioData = generateRandomAudioData(480000) // ~30 seconds at 16kHz
        val tempFile = File.createTempFile("benchmark_large", ".wav", context.cacheDir)
        
        try {
            benchmarkRule.measureRepeated {
                runBlocking {
                    wavWriter.writeWAVFile(audioData, testAudioFormat, tempFile)
                }
            }
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun benchmarkAudioFormatConversion_monoToStereo() = runBlocking {
        val monoData = generateRandomAudioData(32000) // ~2 seconds
        
        benchmarkRule.measureRepeated {
            audioProcessor.convertToStereo(monoData)
        }
    }

    @Test
    fun benchmarkAudioFormatConversion_stereoToMono() = runBlocking {
        val stereoData = generateRandomAudioData(64000) // ~2 seconds stereo
        
        benchmarkRule.measureRepeated {
            audioProcessor.convertToMono(stereoData)
        }
    }

    @Test
    fun benchmarkAudioResampling_16kTo44k() = runBlocking {
        val inputData = generateRandomAudioData(32000) // 2 seconds at 16kHz
        
        benchmarkRule.measureRepeated {
            audioProcessor.resample(inputData, 16000, 44100)
        }
    }

    @Test
    fun benchmarkAudioResampling_44kTo16k() = runBlocking {
        val inputData = generateRandomAudioData(88200) // 2 seconds at 44.1kHz
        
        benchmarkRule.measureRepeated {
            audioProcessor.resample(inputData, 44100, 16000)
        }
    }

    @Test
    fun benchmarkNoiseReduction() = runBlocking {
        val noisyData = generateNoisyAudioData(48000) // 3 seconds with noise
        
        benchmarkRule.measureRepeated {
            audioProcessor.applyNoiseReduction(noisyData, 0.3f)
        }
    }

    @Test
    fun benchmarkVolumeNormalization() = runBlocking {
        val audioData = generateRandomAudioData(48000) // 3 seconds
        
        benchmarkRule.measureRepeated {
            audioProcessor.normalizeVolume(audioData, 0.8f)
        }
    }

    @Test
    fun benchmarkSilenceDetection() = runBlocking {
        val audioWithSilence = generateAudioWithSilence(64000) // 4 seconds with gaps
        
        benchmarkRule.measureRepeated {
            audioProcessor.detectSilence(audioWithSilence, 0.1f, 500)
        }
    }

    @Test
    fun benchmarkAudioCompression() = runBlocking {
        val uncompressedData = generateRandomAudioData(160000) // 10 seconds
        
        benchmarkRule.measureRepeated {
            audioProcessor.compressAudio(uncompressedData, 0.7f)
        }
    }

    @Test
    fun benchmarkConcurrentProcessing_multipleStreams() = runBlocking {
        val stream1 = generateRandomAudioData(16000)
        val stream2 = generateRandomAudioData(16000)
        val stream3 = generateRandomAudioData(16000)
        
        benchmarkRule.measureRepeated {
            runBlocking {
                // Simulate concurrent processing of multiple audio streams
                val job1 = kotlinx.coroutines.async {
                    audioProcessor.processAudioData(stream1, testAudioFormat)
                }
                val job2 = kotlinx.coroutines.async {
                    audioProcessor.processAudioData(stream2, testAudioFormat)
                }
                val job3 = kotlinx.coroutines.async {
                    audioProcessor.processAudioData(stream3, testAudioFormat)
                }
                
                job1.await()
                job2.await()
                job3.await()
            }
        }
    }

    @Test
    fun benchmarkMemoryIntensiveProcessing() = runBlocking {
        // Test with large buffer to stress memory allocation
        val hugeBuffer = generateRandomAudioData(1600000) // 100 seconds at 16kHz
        
        benchmarkRule.measureRepeated {
            audioProcessor.processAudioData(hugeBuffer, testAudioFormat)
        }
    }

    @Test
    fun benchmarkRealTimeProcessing_simulation() = runBlocking {
        // Simulate real-time processing with small chunks
        val chunk = generateRandomAudioData(512) // ~32ms chunk at 16kHz
        
        benchmarkRule.measureRepeated {
            // Simulate real-time constraints
            audioProcessor.processAudioDataRealTime(chunk, testAudioFormat)
        }
    }

    @Test
    fun benchmarkAudioFeatureExtraction() = runBlocking {
        val audioData = generateRandomAudioData(32000) // 2 seconds
        
        benchmarkRule.measureRepeated {
            // Extract audio features for analysis
            audioProcessor.extractAudioFeatures(audioData, testAudioFormat)
        }
    }

    @Test
    fun benchmarkAudioQualityAnalysis() = runBlocking {
        val audioData = generateRandomAudioData(80000) // 5 seconds
        
        benchmarkRule.measureRepeated {
            audioProcessor.analyzeAudioQuality(audioData, testAudioFormat)
        }
    }

    // Helper functions for generating test data

    private fun generateRandomAudioData(size: Int): ShortArray {
        val random = Random(12345) // Fixed seed for reproducible benchmarks
        return ShortArray(size) { 
            (random.nextFloat() * 32767 - 16384).toInt().toShort()
        }
    }

    private fun generateNoisyAudioData(size: Int): ShortArray {
        val random = Random(54321)
        return ShortArray(size) { index ->
            val signal = (kotlin.math.sin(2 * kotlin.math.PI * 440 * index / 16000) * 16384).toInt()
            val noise = (random.nextFloat() * 6553 - 3276).toInt()
            (signal + noise).coerceIn(-32768, 32767).toShort()
        }
    }

    private fun generateAudioWithSilence(size: Int): ShortArray {
        val random = Random(98765)
        return ShortArray(size) { index ->
            if (index % 8000 < 1000) { // 1 second of silence every 8000 samples
                0
            } else {
                (random.nextFloat() * 32767 - 16384).toInt().toShort()
            }
        }
    }
}