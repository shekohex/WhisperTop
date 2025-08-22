package me.shadykhalifa.whispertop.utils

import kotlinx.datetime.Clock
import me.shadykhalifa.whispertop.domain.models.AppSettings
import me.shadykhalifa.whispertop.domain.models.AudioFile
import me.shadykhalifa.whispertop.domain.models.AudioFormat
import me.shadykhalifa.whispertop.domain.models.Language
import me.shadykhalifa.whispertop.domain.models.LanguageDetectionResult
import me.shadykhalifa.whispertop.domain.models.RecordingState
import me.shadykhalifa.whispertop.domain.models.Theme
import me.shadykhalifa.whispertop.domain.models.TranscriptionHistory
import me.shadykhalifa.whispertop.domain.models.TranscriptionRequest
import me.shadykhalifa.whispertop.domain.models.TranscriptionResponse
import kotlin.random.Random

object TestFixtures {
    
    // Deterministic random for reproducible tests
    private val testRandom = Random(42)
    
    // Audio Format fixtures
    fun createAudioFormat(
        sampleRate: Int = 16000,
        channelCount: Int = 1,
        bitDepth: Int = 16
    ) = AudioFormat(
        sampleRate = sampleRate,
        channelCount = channelCount,
        bitDepth = bitDepth
    )
    
    fun createHighQualityAudioFormat() = createAudioFormat(
        sampleRate = 44100,
        channelCount = 2,
        bitDepth = 24
    )
    
    // Audio File fixtures
    fun createAudioFile(
        path: String = TestConstants.MOCK_AUDIO_FILE_PATH,
        durationMs: Long = 10000L,
        sizeBytes: Long = 1024L,
        sessionId: String? = null
    ) = AudioFile(
        path = path,
        durationMs = durationMs,
        sizeBytes = sizeBytes,
        sessionId = sessionId
    )
    
    fun createLargeAudioFile() = createAudioFile(
        durationMs = 300000L, // 5 minutes
        sizeBytes = 1024 * 1024 * 10 // 10MB
    )
    
    // App Settings fixtures
    fun createAppSettings(
        apiKey: String = TestConstants.MOCK_API_KEY,
        selectedModel: String = TestConstants.MOCK_WHISPER_MODEL,
        theme: Theme = Theme.System,
        customPrompt: String? = null,
        temperature: Float = 0.7f
    ) = AppSettings(
        apiKey = apiKey,
        selectedModel = selectedModel,
        theme = theme,
        customPrompt = customPrompt,
        temperature = temperature,
        enableUsageAnalytics = false,
        enableApiCallLogging = false,
        autoCleanupTempFiles = true
    )
    
    fun createMinimalAppSettings() = createAppSettings(
        apiKey = TestConstants.MOCK_API_KEY,
        selectedModel = "whisper-1"
    )
    
    fun createCompleteAppSettings() = createAppSettings(
        customPrompt = "Please transcribe this audio accurately",
        temperature = 0.5f
    )
    
    // Transcription History fixtures
    fun createTranscriptionHistory(
        id: String = "test_${testRandom.nextInt()}",
        text: String = TestConstants.MOCK_TRANSCRIPTION_TEXT,
        timestamp: Long = Clock.System.now().toEpochMilliseconds(),
        duration: Float? = TestConstants.MOCK_DURATION,
        confidence: Float? = TestConstants.MOCK_CONFIDENCE,
        wordCount: Int = TestConstants.MOCK_WORD_COUNT,
        language: String? = "en",
        model: String? = TestConstants.MOCK_WHISPER_MODEL,
        customPrompt: String? = null,
        temperature: Float? = 0.7f,
        audioFilePath: String? = TestConstants.MOCK_AUDIO_FILE_PATH,
        createdAt: Long = Clock.System.now().toEpochMilliseconds(),
        updatedAt: Long = Clock.System.now().toEpochMilliseconds()
    ) = TranscriptionHistory(
        id = id,
        text = text,
        timestamp = timestamp,
        duration = duration,
        audioFilePath = audioFilePath,
        confidence = confidence,
        customPrompt = customPrompt,
        temperature = temperature,
        language = language,
        model = model,
        wordCount = wordCount,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
    
    fun createRecentTranscriptionHistory() = createTranscriptionHistory(
        timestamp = Clock.System.now().toEpochMilliseconds() - 300000 // 5 minutes ago
    )
    
    fun createOldTranscriptionHistory() = createTranscriptionHistory(
        timestamp = Clock.System.now().toEpochMilliseconds() - 86400000 // 24 hours ago
    )
    
    fun createLongTranscriptionHistory() = createTranscriptionHistory(
        text = TestConstants.MOCK_LONG_TRANSCRIPTION_TEXT,
        duration = 120f,
        wordCount = 200
    )
    
    fun createTranscriptionHistoryList(count: Int): List<TranscriptionHistory> {
        return (1..count).map { index ->
            createTranscriptionHistory(
                id = "history_$index",
                text = "Test transcription number $index",
                timestamp = Clock.System.now().toEpochMilliseconds() - (index * 60000L), // 1 minute apart
                wordCount = 5 + index
            )
        }
    }
    
    // Transcription Request fixtures
    fun createTranscriptionRequest(
        audioFile: AudioFile = createAudioFile(),
        model: String = TestConstants.MOCK_WHISPER_MODEL,
        language: String? = "en",
        customPrompt: String? = null,
        temperature: Float = 0.7f
    ) = TranscriptionRequest(
        audioFile = audioFile,
        model = model,
        language = language,
        customPrompt = customPrompt,
        temperature = temperature
    )
    
    fun createSimpleTranscriptionRequest() = createTranscriptionRequest()
    
    fun createComplexTranscriptionRequest() = createTranscriptionRequest(
        audioFile = createLargeAudioFile(),
        language = "es",
        customPrompt = "This is a Spanish audio recording about technology",
        temperature = 0.3f
    )
    
    // Transcription Response fixtures
    fun createTranscriptionResponse(
        text: String = TestConstants.MOCK_TRANSCRIPTION_TEXT,
        language: String = "en",
        duration: Float = TestConstants.MOCK_DURATION,
        languageDetection: LanguageDetectionResult? = null
    ) = TranscriptionResponse(
        text = text,
        language = language,
        duration = duration,
        languageDetection = languageDetection
    )
    
    fun createSuccessfulTranscriptionResponse() = createTranscriptionResponse(
        languageDetection = LanguageDetectionResult.autoDetected(Language.ENGLISH, 0.98f)
    )
    
    fun createLowConfidenceTranscriptionResponse() = createTranscriptionResponse(
        languageDetection = LanguageDetectionResult.autoDetected(Language.ENGLISH, 0.45f),
        text = "This transcription has low confidence"
    )
    
    // Recording State fixtures
    fun createIdleRecordingState() = RecordingState.Idle
    
    fun createRecordingRecordingState(
        startTime: Long = Clock.System.now().toEpochMilliseconds(),
        duration: Long = 5000L
    ) = RecordingState.Recording(
        startTime = startTime,
        duration = duration
    )
    
    fun createProcessingRecordingState() = RecordingState.Processing(progress = 0.5f)
    
    // Error scenarios
    fun createErrorRecordingState(errorMessage: String = "Recording failed") = 
        RecordingState.Error(
            throwable = Exception(errorMessage),
            retryable = true
        )
    
    // Edge case fixtures
    fun createEmptyTranscriptionHistory() = createTranscriptionHistory(
        text = "",
        duration = null,
        confidence = null,
        wordCount = 0
    )
    
    fun createMinimalAudioFile() = createAudioFile(
        durationMs = 100L, // 100ms
        sizeBytes = 160L // Very small
    )
    
    fun createMaximalTranscriptionHistory() = createTranscriptionHistory(
        text = "a".repeat(10000), // Very long text
        duration = 3600f, // 1 hour
        confidence = 1.0f,
        wordCount = 10000
    )
    
    // Collections and batches
    fun createMixedQualityTranscriptions(count: Int): List<TranscriptionHistory> {
        return (1..count).map { index ->
            when (index % 3) {
                0 -> createTranscriptionHistory(
                    id = "high_quality_$index",
                    confidence = 0.9f + testRandom.nextFloat() * 0.1f
                )
                1 -> createTranscriptionHistory(
                    id = "medium_quality_$index", 
                    confidence = 0.7f + testRandom.nextFloat() * 0.2f
                )
                else -> createTranscriptionHistory(
                    id = "low_quality_$index",
                    confidence = 0.3f + testRandom.nextFloat() * 0.4f
                )
            }
        }
    }
    
    fun createMultiLanguageTranscriptions(): List<TranscriptionHistory> {
        val languages = listOf("en", "es", "fr", "de", "it", "pt")
        val texts = mapOf(
            "en" to "This is an English transcription for testing purposes",
            "es" to "Esta es una transcripción en español para pruebas",
            "fr" to "Ceci est une transcription française pour les tests",
            "de" to "Dies ist eine deutsche Transkription für Tests",
            "it" to "Questa è una trascrizione italiana per i test",
            "pt" to "Esta é uma transcrição em português para testes"
        )
        
        return languages.mapIndexed { index, lang ->
            createTranscriptionHistory(
                id = "${lang}_transcription_$index",
                text = texts[lang] ?: "Test transcription in $lang",
                language = lang
            )
        }
    }
    
    // Time-based fixtures
    fun createTranscriptionsByTimeRange(
        startTime: Long,
        endTime: Long,
        count: Int
    ): List<TranscriptionHistory> {
        val timeRange = endTime - startTime
        val timeStep = timeRange / count
        
        return (0 until count).map { index ->
            createTranscriptionHistory(
                id = "time_based_$index",
                timestamp = startTime + (index * timeStep),
                text = "Transcription at time ${startTime + (index * timeStep)}"
            )
        }
    }
    
    // Performance testing fixtures
    fun createLargeDataset(size: Int): List<TranscriptionHistory> {
        return (1..size).map { index ->
            when {
                index % 100 == 0 -> createLongTranscriptionHistory().copy(id = "large_$index")
                index % 50 == 0 -> createMaximalTranscriptionHistory().copy(id = "large_$index")
                else -> createTranscriptionHistory(id = "large_$index")
            }
        }
    }
    
    // Validation helpers
    fun isValidTestFixture(transcription: TranscriptionHistory): Boolean {
        return transcription.id.isNotEmpty() &&
               transcription.text.isNotEmpty() &&
               transcription.timestamp > 0 &&
               (transcription.confidence == null || transcription.confidence in 0f..1f) &&
               transcription.wordCount >= 0
    }
    
    fun generateReproducibleData(seed: Int, count: Int): List<TranscriptionHistory> {
        val random = Random(seed)
        return (1..count).map { index ->
            createTranscriptionHistory(
                id = "reproducible_$index",
                text = "Reproducible text $index with random number ${random.nextInt()}",
                confidence = random.nextFloat(),
                duration = random.nextFloat() * 60f // 0-60 seconds
            )
        }
    }
}