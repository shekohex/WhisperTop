package me.shadykhalifa.whispertop.data.repositories

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import me.shadykhalifa.whispertop.data.models.OpenAIException
import me.shadykhalifa.whispertop.data.models.TranscriptionRequestDto
import me.shadykhalifa.whispertop.data.models.TranscriptionResponseDto
import me.shadykhalifa.whispertop.data.models.toDomain
import me.shadykhalifa.whispertop.data.models.WhisperModel
import me.shadykhalifa.whispertop.data.remote.OpenAIApiService
import me.shadykhalifa.whispertop.data.remote.createOpenAIApiService
import me.shadykhalifa.whispertop.domain.models.TranscriptionError
import me.shadykhalifa.whispertop.domain.models.TranscriptionRequest
import me.shadykhalifa.whispertop.domain.models.TranscriptionResponse
import me.shadykhalifa.whispertop.domain.models.Language
import me.shadykhalifa.whispertop.domain.models.LanguageDetectionResult
import me.shadykhalifa.whispertop.domain.repositories.SettingsRepository
import me.shadykhalifa.whispertop.domain.repositories.TranscriptionRepository
import me.shadykhalifa.whispertop.domain.services.FileReaderService
import me.shadykhalifa.whispertop.domain.services.MetricsCollector
import me.shadykhalifa.whispertop.domain.usecases.LanguageDetectionUseCase
import me.shadykhalifa.whispertop.utils.Result
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

expect fun log(tag: String, message: String)
expect fun logError(tag: String, message: String, throwable: Throwable? = null)

class TranscriptionRepositoryImpl(
    private val httpClient: HttpClient,
    private val settingsRepository: SettingsRepository,
    private val fileReaderService: FileReaderService,
    private val metricsCollector: MetricsCollector,
    private val languageDetectionUseCase: LanguageDetectionUseCase = LanguageDetectionUseCase()
) : BaseRepository(), TranscriptionRepository {

    private companion object {
        const val TAG = "TranscriptionRepo"
        const val OPENAI_BASE_URL = "https://api.openai.com/v1"
        const val TRANSCRIPTIONS_ENDPOINT = "$OPENAI_BASE_URL/audio/transcriptions"
    }

    override suspend fun transcribe(request: TranscriptionRequest): Result<TranscriptionResponse> = execute {
        log(TAG, "Starting transcription for audio file: ${request.audioFile.path}")
        log(TAG, "Request details: model=${request.model}, language=${request.language}")
        
        val settings = settingsRepository.getSettings()
        val sessionId = request.audioFile.sessionId ?: generateSessionId()
        
        log(TAG, "Session ID: $sessionId")
        log(TAG, "API key configured: ${settings.apiKey.isNotBlank()}")
        log(TAG, "Base URL: '${settings.baseUrl}'")
        log(TAG, "Is custom endpoint: ${settings.isCustomEndpoint}")
        log(TAG, "Is OpenAI endpoint: ${settings.isOpenAIEndpoint()}")
        
        if (settings.apiKey.isBlank()) {
            logError(TAG, "API key is missing or empty")
            throw TranscriptionError.ApiKeyMissing()
        }

        try {
            // Start transcription metrics
            log(TAG, "Starting transcription metrics collection")
            val metrics = metricsCollector.startTranscriptionMetrics(sessionId)
            
            log(TAG, "Reading audio file: ${request.audioFile.path}")
            val audioData = fileReaderService.readFileAsBytes(request.audioFile.path)
            val audioFileSize = audioData.size.toLong()
            log(TAG, "Audio file read successfully: $audioFileSize bytes")
            
            // Update metrics with initial data
            metricsCollector.updateTranscriptionMetrics(sessionId) { currentMetrics ->
                currentMetrics.copy(
                    audioFileSize = audioFileSize,
                    audioFileDuration = request.audioFile.duration ?: 0,
                    model = request.model,
                    language = request.language
                )
            }
            
            // Check file size (OpenAI limit is 25MB)
            if (audioData.size > 25 * 1024 * 1024) {
                logError(TAG, "Audio file too large: $audioFileSize bytes (limit: 25MB)")
                metricsCollector.endTranscriptionMetrics(sessionId, false, "Audio file too large")
                throw TranscriptionError.AudioTooLarge()
            }
            
            log(TAG, "Audio file size check passed: $audioFileSize bytes")
            
            val requestStartTime = System.currentTimeMillis()
            var retryCount = 0
            var lastException: Exception? = null
            
            // Retry logic with metrics
            log(TAG, "Starting HTTP request attempts (max 3 retries)")
            repeat(3) { attempt ->
                try {
                    log(TAG, "Attempt ${attempt + 1}/3: Starting API call to OpenAI")
                    val connectionStartTime = System.currentTimeMillis()
                    
                    val transcriptionUrl = "${settings.baseUrl.trimEnd('/')}/audio/transcriptions"
                    log(TAG, "Making request to URL: '$transcriptionUrl'")
                    val response = httpClient.submitFormWithBinaryData(
                        url = transcriptionUrl,
                        formData = formData {
                            append("file", audioData, Headers.build {
                                append(HttpHeaders.ContentType, "audio/wav")
                                append(HttpHeaders.ContentDisposition, "filename=\"audio.wav\"")
                            })
                            append("model", request.model)
                            append("response_format", "json")
                            if (request.language != null) {
                                append("language", request.language)
                            }
                        }
                    ) {
                        headers {
                            append(HttpHeaders.Authorization, "Bearer ${settings.apiKey}")
                        }
                    }
                    
                    val connectionTime = System.currentTimeMillis() - connectionStartTime
                    log(TAG, "API request completed in ${connectionTime}ms, status: ${response.status}")
                    
                    // Update metrics with network timing
                    metricsCollector.updateTranscriptionMetrics(sessionId) { currentMetrics ->
                        currentMetrics.copy(
                            networkRequestSize = audioFileSize,
                            connectionTimeMs = connectionTime,
                            httpStatusCode = response.status.value,
                            retryCount = retryCount
                        )
                    }

                    if (!response.status.isSuccess()) {
                        retryCount++
                        logError(TAG, "API request failed with status: ${response.status.value} ${response.status.description}")
                        val error = when (response.status.value) {
                            401 -> TranscriptionError.AuthenticationError()
                            429 -> TranscriptionError.RateLimitError()
                            in 500..599 -> TranscriptionError.ApiError(response.status.value, "Server error")
                            else -> TranscriptionError.ApiError(response.status.value, "API error: ${response.status}")
                        }
                        
                        if (attempt == 2) { // Last attempt
                            logError(TAG, "All retry attempts failed, throwing error: ${error.message}")
                            metricsCollector.endTranscriptionMetrics(sessionId, false, error.message ?: "API error")
                            throw error
                        } else {
                            log(TAG, "Retrying after failure (attempt $attempt), waiting ${1000L * (attempt + 1)}ms")
                            lastException = error
                            delay(1000L * (attempt + 1)) // Exponential backoff
                            return@repeat
                        }
                    }

                    log(TAG, "Parsing response body...")
                    val transcriptionResponse: TranscriptionResponseDto = response.body()
                    val responseSize = transcriptionResponse.toString().length.toLong() // Approximate size
                    val transferTime = System.currentTimeMillis() - requestStartTime
                    
                    log(TAG, "Response parsed successfully, transcription length: ${transcriptionResponse.text?.length ?: 0} chars")
                    log(TAG, "Total request time: ${transferTime}ms")
                    
                    val result = transcriptionResponse.toDomain()
                    log(TAG, "Transcription result: '${result.text}' (language: ${result.language})")
                    
                    // Update final metrics
                    metricsCollector.updateTranscriptionMetrics(sessionId) { currentMetrics ->
                        currentMetrics.copy(
                            networkResponseSize = responseSize,
                            transcriptionLength = result.text.length,
                            transferTimeMs = transferTime,
                            detectedLanguage = result.language
                        )
                    }
                    
                    metricsCollector.endTranscriptionMetrics(sessionId, true)
                    log(TAG, "Transcription completed successfully!")
                    return@execute result
                    
                } catch (e: Exception) {
                    retryCount++
                    lastException = e
                    logError(TAG, "API request attempt ${attempt + 1} failed", e)
                    
                    if (attempt == 2) { // Last attempt
                        val errorMessage = "Network error after retries: ${e.message}"
                        logError(TAG, "All retry attempts exhausted: $errorMessage")
                        metricsCollector.updateTranscriptionMetrics(sessionId) { currentMetrics ->
                            currentMetrics.copy(retryCount = retryCount)
                        }
                        metricsCollector.endTranscriptionMetrics(sessionId, false, errorMessage)
                        throw if (e.message?.contains("network", ignoreCase = true) == true ||
                                  e.message?.contains("timeout", ignoreCase = true) == true ||
                                  e.message?.contains("connection", ignoreCase = true) == true) {
                            TranscriptionError.NetworkError(e)
                        } else {
                            TranscriptionError.UnexpectedError(e)
                        }
                    } else {
                        log(TAG, "Retrying after exception (attempt $attempt), waiting ${1000L * (attempt + 1)}ms")
                        delay(1000L * (attempt + 1)) // Exponential backoff
                    }
                }
            }
            
            // This should never be reached due to the retry logic above
            throw lastException ?: TranscriptionError.UnexpectedError(Exception("Unknown error"))
            
        } catch (e: TranscriptionError) {
            throw e
        } catch (e: OpenAIException) {
            metricsCollector.endTranscriptionMetrics(sessionId, false, "OpenAI API error: ${e.message}")
            throw TranscriptionError.fromOpenAIException(e)
        } catch (e: Exception) {
            val errorMessage = if (e.message?.contains("network", ignoreCase = true) == true ||
                                  e.message?.contains("timeout", ignoreCase = true) == true ||
                                  e.message?.contains("connection", ignoreCase = true) == true) {
                "Network error: ${e.message}"
            } else {
                "Unexpected error: ${e.message}"
            }
            metricsCollector.endTranscriptionMetrics(sessionId, false, errorMessage)
            
            if (e.message?.contains("network", ignoreCase = true) == true ||
                e.message?.contains("timeout", ignoreCase = true) == true ||
                e.message?.contains("connection", ignoreCase = true) == true) {
                throw TranscriptionError.NetworkError(e)
            } else {
                throw TranscriptionError.UnexpectedError(e)
            }
        }
    }

    override suspend fun validateApiKey(apiKey: String): Result<Boolean> = execute {
        if (apiKey.isBlank()) {
            return@execute false
        }

        try {
            val settings = settingsRepository.getSettings()
            val modelsUrl = "${settings.baseUrl.trimEnd('/')}/models"
            val response = httpClient.get(modelsUrl) {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $apiKey")
                }
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun transcribeWithLanguageDetection(
        request: TranscriptionRequest,
        userLanguageOverride: Language?
    ): Result<TranscriptionResponse> = execute {
        val settings = settingsRepository.getSettings()
        
        if (settings.apiKey.isBlank()) {
            throw TranscriptionError.ApiKeyMissing()
        }

        try {
            val audioData = fileReaderService.readFileAsBytes(request.audioFile.path)
            
            // Check file size (OpenAI limit is 25MB)
            if (audioData.size > 25 * 1024 * 1024) {
                throw TranscriptionError.AudioTooLarge()
            }

            // Create OpenAI API service
            val apiService = createOpenAIApiService(
                apiKey = settings.apiKey,
                baseUrl = settings.baseUrl
            )

            // Determine language for transcription
            val transcriptionLanguage = languageDetectionUseCase.determineTranscriptionLanguage(
                preference = settings.languagePreference,
                userOverride = userLanguageOverride
            )

            // Use the best model for language detection
            val modelForDetection = when {
                WhisperModel.isBuiltIn(request.model) -> WhisperModel.fromString(request.model) ?: WhisperModel.GPT_4O_TRANSCRIBE
                settings.isOpenAIEndpoint() -> WhisperModel.GPT_4O_TRANSCRIBE // Default to best OpenAI model
                else -> null // For custom endpoints, use the custom model as-is
            }

            // Get verbose response for language detection
            val verboseResponse = if (modelForDetection != null) {
                // Use built-in model with language detection
                apiService.transcribeWithLanguageDetection(
                    audioData = audioData,
                    fileName = "audio.wav",
                    model = modelForDetection,
                    language = transcriptionLanguage,
                    prompt = null,
                    temperature = 0.0f
                )
            } else {
                // Use custom model - fall back to standard transcribe with string model
                val standardResponse = apiService.transcribe(
                    audioData = audioData,
                    fileName = "audio.wav",
                    model = request.model,
                    language = transcriptionLanguage,
                    prompt = null,
                    responseFormat = "verbose_json",
                    temperature = 0.0f
                )
                // Convert to verbose format - custom endpoints may not support all verbose fields
                me.shadykhalifa.whispertop.data.models.CreateTranscriptionResponseVerboseDto(
                    text = standardResponse.text,
                    language = transcriptionLanguage ?: "en", // Default fallback
                    duration = 0.0f // Not available from custom endpoints
                )
            }

            // Create language detection result
            val languageDetection = languageDetectionUseCase.createDetectionResult(
                detectedLanguageCode = verboseResponse.language,
                userOverride = userLanguageOverride,
                confidence = null // OpenAI doesn't provide confidence scores
            )

            // Convert to domain model with language detection
            TranscriptionResponse(
                text = verboseResponse.text,
                language = verboseResponse.language,
                duration = verboseResponse.duration,
                languageDetection = languageDetection
            )
            
        } catch (e: TranscriptionError) {
            throw e
        } catch (e: OpenAIException) {
            throw TranscriptionError.fromOpenAIException(e)
        } catch (e: Exception) {
            if (e.message?.contains("network", ignoreCase = true) == true ||
                e.message?.contains("timeout", ignoreCase = true) == true ||
                e.message?.contains("connection", ignoreCase = true) == true) {
                throw TranscriptionError.NetworkError(e)
            } else {
                throw TranscriptionError.UnexpectedError(e)
            }
        }
    }

    override suspend fun isConfigured(): Boolean {
        return try {
            val settings = settingsRepository.getSettings()
            settings.apiKey.isNotBlank()
        } catch (e: Exception) {
            false
        }
    }
    
    private fun generateSessionId(): String = "session_${System.currentTimeMillis()}"
}

expect class FileReader {
    suspend fun readFileAsBytes(filePath: String): ByteArray
}