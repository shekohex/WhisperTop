package me.shadykhalifa.whispertop.audio

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
import me.shadykhalifa.whispertop.data.audio.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import kotlin.test.*
import java.io.File

@RunWith(AndroidJUnit4::class)
class AudioQualityIntegrationTest : KoinTest {
    
    private val context: Context by inject()
    private lateinit var testOutputDir: File
    
    @Before
    fun setup() {
        stopKoin()
        
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        testOutputDir = File(appContext.cacheDir, "test_audio")
        testOutputDir.mkdirs()
        
        startKoin {
            modules(
                module {
                    single<Context> { appContext }
                }
            )
        }
    }
    
    @Test
    fun testAudioRecorderWithQualityManagement() = runTest {
        val recorder = AudioRecorderImpl()
        val outputPath = File(testOutputDir, "test_recording.wav").absolutePath
        
        // Start recording
        val result = recorder.startRecording(outputPath)
        assertTrue(result is AudioRecordingResult.Success)
        
        // Let it record for a bit
        Thread.sleep(1000)
        
        // Check that we're getting quality metrics
        val metrics = recorder.getCurrentMetrics()
        assertNotNull(metrics)
        
        val stats = recorder.getRecordingStatistics()
        assertNotNull(stats)
        assertTrue(stats.duration > 0)
        assertTrue(stats.fileSize > 0)
        
        // Stop recording
        val audioFile = recorder.stopRecording()
        assertNotNull(audioFile)
        assertTrue(File(audioFile.path).exists())
        
        // Get final quality report
        val report = recorder.getQualityReport()
        assertNotNull(report)
        assertTrue(report.overallQuality in 0..100)
        
        // Cleanup
        recorder.cleanup()
        File(outputPath).delete()
    }
    
    @Test
    fun testFileSizeLimitEnforcement() = runTest {
        val manager = AudioQualityManager()
        manager.startMonitoring()
        
        // Simulate recording that approaches file size limit
        var totalSize = 0L
        while (totalSize < RecordingConstraints.MAX_FILE_SIZE_BYTES * 0.9) {
            val buffer = ShortArray(16000) { 1000 } // 1 second of audio
            manager.processAudioBuffer(buffer)
            totalSize += buffer.size * 2
        }
        
        // Should not stop yet
        assertFalse(manager.shouldStopRecording())
        
        // Add more to exceed threshold
        val largeBuffer = ShortArray(1024 * 1024) { 1000 } // Large buffer
        manager.processAudioBuffer(largeBuffer)
        
        // Should now suggest stopping
        assertTrue(manager.shouldStopRecording())
    }
    
    @Test
    fun testAudioProcessingPipeline() = runTest {
        val processor = AudioProcessor(QualityPreset.HIGH)
        
        // Create test audio with various characteristics
        val testAudio = ShortArray(10000) { i ->
            when {
                i < 1000 -> 0 // Leading silence
                i > 9000 -> 0 // Trailing silence
                i in 4000..5000 -> 32767 // Clipping section
                i in 6000..7000 -> 50 // Very quiet section
                else -> (8000 * kotlin.math.sin(i * 0.1)).toInt().toShort()
            }
        }
        
        val processed = processor.processAudio(testAudio)
        
        // Should be shorter due to silence trimming
        assertTrue(processed.size < testAudio.size)
        
        // Check that processing improved the audio
        val originalMetrics = AudioMetrics.calculate(testAudio)
        val processedMetrics = AudioMetrics.calculate(processed)
        
        // Processed audio should have better quality score
        assertTrue(processedMetrics.qualityScore >= originalMetrics.qualityScore)
    }
    
    @Test
    fun testRealTimeQualityMonitoring() = runTest {
        val manager = AudioQualityManager()
        val processor = AudioProcessor()
        
        manager.startMonitoring()
        
        // Simulate real-time audio processing
        val bufferDurationMs = 100L
        val buffersToProcess = 50
        
        repeat(buffersToProcess) { i ->
            // Generate audio buffer with varying characteristics
            val buffer = when (i) {
                in 0..10 -> ShortArray(1600) { 0 } // Silence
                in 11..20 -> ShortArray(1600) { (100 * kotlin.math.sin(it * 0.1)).toInt().toShort() } // Quiet
                in 21..30 -> ShortArray(1600) { (8000 * kotlin.math.sin(it * 0.1)).toInt().toShort() } // Normal
                in 31..40 -> ShortArray(1600) { if (it % 10 == 0) 32767 else 16000 } // Some clipping
                else -> ShortArray(1600) { kotlin.random.Random.nextInt(-1000, 1000).toShort() } // Noise
            }
            
            val metrics = manager.processAudioBuffer(buffer)
            
            // Verify metrics are being updated
            when (i) {
                in 0..10 -> assertTrue(metrics.isSilent)
                in 31..40 -> assertTrue(metrics.isClipping || metrics.peakLevel > 0.9f)
            }
            
            Thread.sleep(bufferDurationMs)
        }
        
        val report = manager.getQualityReport()
        
        // Should have detected various issues
        assertTrue(report.issues.isNotEmpty())
        assertTrue(report.recommendations.isNotEmpty())
        
        // Statistics should be populated
        val stats = report.recordingStatistics
        assertNotNull(stats)
        assertTrue(stats.duration >= buffersToProcess * bufferDurationMs * 0.8) // Allow some tolerance
        assertTrue(stats.clippingOccurrences > 0)
        assertTrue(stats.silencePercentage > 0)
    }
    
    @Test
    fun testQualityPresets() = runTest {
        val presets = listOf(
            QualityPreset.LOW,
            QualityPreset.MEDIUM,
            QualityPreset.HIGH
        )
        
        val testAudio = ShortArray(5000) { i ->
            when {
                i < 500 -> 0 // Silence
                else -> (5000 * kotlin.math.sin(i * 0.1)).toInt().toShort()
            }
        }
        
        for (preset in presets) {
            val processor = AudioProcessor(preset)
            val processed = processor.processAudio(testAudio)
            
            // Different presets should produce different results
            when (preset) {
                QualityPreset.LOW -> {
                    // Low preset does minimal processing
                    assertFalse(preset.noiseReduction)
                    assertFalse(preset.normalization)
                }
                QualityPreset.HIGH -> {
                    // High preset does all processing
                    assertTrue(preset.noiseReduction)
                    assertTrue(preset.normalization)
                    assertTrue(preset.silenceTrimming)
                }
                else -> { /* Medium preset */ }
            }
        }
    }
    
    @Test
    fun testAudioLevelValidation() = runTest {
        val recorder = AudioRecorderImpl()
        val outputPath = File(testOutputDir, "level_test.wav").absolutePath
        
        // Start recording
        val result = recorder.startRecording(outputPath)
        assertTrue(result is AudioRecordingResult.Success)
        
        // Record for enough time to get meaningful metrics
        Thread.sleep(2000)
        
        // Test audio level validation
        val isAcceptable = recorder.isAudioLevelAcceptable()
        val metrics = recorder.getCurrentMetrics()
        val stats = recorder.getRecordingStatistics()
        
        // Log diagnostics for debugging
        recorder.logAudioDiagnostics()
        
        // Audio should have some level (might be quiet depending on environment)
        assertNotNull(metrics)
        assertNotNull(stats)
        
        // Check that metrics are being calculated
        assertTrue(metrics.rmsLevel >= 0f)
        assertTrue(metrics.peakLevel >= 0f)
        assertTrue(metrics.dbLevel <= 0f) // dB should be negative or 0
        assertTrue(metrics.qualityScore in 0..100)
        
        // Stop recording  
        val audioFile = recorder.stopRecording()
        assertNotNull(audioFile)
        
        // Verify WAV file was created with content
        val file = File(audioFile.path)
        assertTrue(file.exists())
        assertTrue(file.length() > 44) // Header + some audio data
        
        // Cleanup
        recorder.cleanup()
        file.delete()
    }
}