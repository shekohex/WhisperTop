package me.shadykhalifa.whispertop.data.remote

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import me.shadykhalifa.whispertop.data.models.*

// Logging functions - implemented per platform
expect fun apiLog(tag: String, message: String)
expect fun apiLogError(tag: String, message: String, throwable: Throwable? = null)

class OpenAIApiService(
    private val httpClient: HttpClient
) {
    private companion object {
        const val TAG = "OpenAIApiService"
        
        // JSON parser with custom endpoint compatibility
        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = false
        }
    }
    suspend fun transcribe(
        audioData: ByteArray,
        fileName: String,
        model: WhisperModel = WhisperModel.WHISPER_1,
        language: String? = null,
        prompt: String? = null,
        responseFormat: AudioResponseFormat = AudioResponseFormat.JSON,
        temperature: Float = 0.0f
    ): CreateTranscriptionResponseDto {
        apiLog(TAG, "Starting transcription with built-in model - " +
                "model='${model.modelId}', " +
                "fileName='$fileName', " +
                "responseFormat='${responseFormat.format}', " +
                "language='$language', " +
                "audioSize=${audioData.size} bytes")
        
        validateParameters(model, temperature)
        
        val request = CreateTranscriptionRequestDto(
            model = model.modelId,
            language = language,
            prompt = prompt,
            responseFormat = responseFormat.format,
            temperature = temperature
        )
        
        val contentType = determineContentType(fileName)
        apiLog(TAG, "Determined content type: '$contentType' for file: '$fileName'")
        
        apiLog(TAG, "Sending request to endpoint 'audio/transcriptions'")
        val response = httpClient.uploadAudioFile(
            endpoint = "audio/transcriptions",
            audioData = audioData,
            fileName = fileName,
            contentType = contentType,
            transcriptionRequest = request
        )
        
        apiLog(TAG, "Response status: ${response.status}")
        
        // Log raw response body for debugging
        val responseBodyText = response.bodyAsText()
        apiLog(TAG, "Raw response body: $responseBodyText")
        
        response.validateOrThrow()
        
        val result = when (responseFormat) {
            AudioResponseFormat.VERBOSE_JSON -> {
                apiLog(TAG, "Parsing verbose JSON response")
                try {
                    val verboseResponse = json.decodeFromString<CreateTranscriptionResponseVerboseDto>(responseBodyText)
                    apiLog(TAG, "Verbose response parsed - text: '${verboseResponse.text}', language: ${verboseResponse.language}")
                    CreateTranscriptionResponseDto(text = verboseResponse.text)
                } catch (e: Exception) {
                    apiLogError(TAG, "Failed to parse verbose JSON response", e)
                    throw e
                }
            }
            else -> {
                apiLog(TAG, "Parsing standard JSON response")
                try {
                    val standardResponse = json.decodeFromString<CreateTranscriptionResponseDto>(responseBodyText)
                    apiLog(TAG, "Standard response parsed - text: '${standardResponse.text}'")
                    standardResponse
                } catch (e: Exception) {
                    apiLogError(TAG, "Failed to parse standard JSON response", e)
                    throw e
                }
            }
        }
        
        apiLog(TAG, "Transcription completed successfully - text length: ${result.text.length}")
        return result
    }

    /**
     * Transcribe with language detection support
     * Returns verbose response for language detection information
     */
     suspend fun transcribeWithLanguageDetection(
        audioData: ByteArray,
        fileName: String,
        model: WhisperModel = WhisperModel.GPT_4O_TRANSCRIBE,
        language: String? = null,
        prompt: String? = null,
        temperature: Float = 0.0f
    ): CreateTranscriptionResponseVerboseDto {
        apiLog(TAG, "Starting language detection transcription - " +
                "model='${model.modelId}', " +
                "fileName='$fileName', " +
                "language='$language', " +
                "audioSize=${audioData.size} bytes")
        
        validateParameters(model, temperature)
        
        val request = CreateTranscriptionRequestDto(
            model = model.modelId,
            language = language,
            prompt = prompt,
            responseFormat = AudioResponseFormat.VERBOSE_JSON.format,
            temperature = temperature
        )
        
        val contentType = determineContentType(fileName)
        apiLog(TAG, "Determined content type: '$contentType' for file: '$fileName'")
        
        apiLog(TAG, "Sending language detection request to endpoint 'audio/transcriptions'")
        val response = httpClient.uploadAudioFile(
            endpoint = "audio/transcriptions",
            audioData = audioData,
            fileName = fileName,
            contentType = contentType,
            transcriptionRequest = request
        )
        
        apiLog(TAG, "Language detection response status: ${response.status}")
        
        // Log raw response body for debugging
        val responseBodyText = response.bodyAsText()
        apiLog(TAG, "Language detection raw response body: $responseBodyText")
        
        response.validateOrThrow()
        
        val result = try {
            json.decodeFromString<CreateTranscriptionResponseVerboseDto>(responseBodyText)
        } catch (e: Exception) {
            apiLogError(TAG, "Failed to parse language detection response", e)
            throw e
        }
        apiLog(TAG, "Language detection completed - text: '${result.text}', detected language: '${result.language}'")
        
        return result
    }
    
    suspend fun transcribe(
        audioData: ByteArray,
        fileName: String,
        model: String,
        language: String? = null,
        prompt: String? = null,
        responseFormat: String = "json",
        temperature: Float = 0.0f
    ): CreateTranscriptionResponseDto {
        apiLog(TAG, "Starting transcription with custom model - " +
                "model='$model', " +
                "fileName='$fileName', " +
                "responseFormat='$responseFormat', " +
                "language='$language', " +
                "audioSize=${audioData.size} bytes")
        
        // Try built-in models first
        val whisperModel = WhisperModel.fromString(model)
        if (whisperModel != null) {
            apiLog(TAG, "Using built-in WhisperModel: ${whisperModel.modelId}")
            val audioFormat = AudioResponseFormat.fromString(responseFormat)
                ?: throw IllegalArgumentException("Unsupported response format: $responseFormat")
                
            return transcribe(
                audioData = audioData,
                fileName = fileName,
                model = whisperModel,
                language = language,
                prompt = prompt,
                responseFormat = audioFormat,
                temperature = temperature
            )
        }
        
        // Handle custom models - use raw HTTP request
        apiLog(TAG, "Using custom model (not built-in): '$model'")
        return transcribeCustomModel(
            audioData = audioData,
            fileName = fileName,
            model = model,
            language = language,
            prompt = prompt,
            responseFormat = "json",
            temperature = temperature
        )
    }
    
    private suspend fun transcribeCustomModel(
        audioData: ByteArray,
        fileName: String,
        model: String,
        language: String? = null,
        prompt: String? = null,
        responseFormat: String = "json",
        temperature: Float = 0.0f
    ): CreateTranscriptionResponseDto {
        apiLog(TAG, "transcribeCustomModel - " +
                "model='$model', " +
                "responseFormat='$responseFormat', " +
                "temperature=$temperature")
        
        if (temperature < 0.0f || temperature > 1.0f) {
            throw IllegalArgumentException("Temperature must be between 0.0 and 1.0")
        }
        
        val request = CreateTranscriptionRequestDto(
            model = model,
            language = language,
            prompt = prompt,
            responseFormat = responseFormat,
            temperature = temperature
        )
        
        val contentType = determineContentType(fileName)
        apiLog(TAG, "Determined content type: '$contentType' for file: '$fileName'")
        
        apiLog(TAG, "Sending request to endpoint 'audio/transcriptions'")
        val response = httpClient.uploadAudioFile(
            endpoint = "audio/transcriptions",
            audioData = audioData,
            fileName = fileName,
            contentType = contentType,
            transcriptionRequest = request
        )
        
        apiLog(TAG, "Response status: ${response.status}")
        
        // Log raw response body for debugging
        val responseBodyText = response.bodyAsText()
        apiLog(TAG, "Raw response body: $responseBodyText")
        
        response.validateOrThrow()
        
        // Parse response from the raw text and log the result
        val result = when (responseFormat) {
            "verbose_json" -> {
                apiLog(TAG, "Parsing verbose JSON response")
                try {
                    val verboseResponse = json.decodeFromString<CreateTranscriptionResponseVerboseDto>(responseBodyText)
                    apiLog(TAG, "Verbose response parsed - text: '${verboseResponse.text}', language: ${verboseResponse.language}")
                    CreateTranscriptionResponseDto(text = verboseResponse.text)
                } catch (e: Exception) {
                    apiLogError(TAG, "Failed to parse verbose JSON response", e)
                    throw e
                }
            }
            else -> {
                apiLog(TAG, "Parsing standard JSON response")
                try {
                    val standardResponse = json.decodeFromString<CreateTranscriptionResponseDto>(responseBodyText)
                    apiLog(TAG, "Standard response parsed - text: '${standardResponse.text}'")
                    standardResponse
                } catch (e: Exception) {
                    apiLogError(TAG, "Failed to parse standard JSON response", e)
                    throw e
                }
            }
        }
        
        apiLog(TAG, "Transcription completed successfully - text length: ${result.text.length}")
        return result
    }
    
    private fun validateParameters(model: WhisperModel, temperature: Float) {
        if (temperature < 0.0f || temperature > 1.0f) {
            throw IllegalArgumentException("Temperature must be between 0.0 and 1.0")
        }
    }
    
    private fun determineContentType(fileName: String): String {
        return when (fileName.substringAfterLast('.').lowercase()) {
            "wav" -> "audio/wav"
            "mp3" -> "audio/mpeg"
            "m4a" -> "audio/mp4"
            "ogg" -> "audio/ogg"
            "webm" -> "audio/webm"
            "flac" -> "audio/flac"
            else -> "audio/wav"
        }
    }
}



fun createOpenAIApiService(
    apiKey: String,
    baseUrl: String = "https://api.openai.com/v1/",
    logLevel: OpenAILogLevel = OpenAILogLevel.BASIC
): OpenAIApiService {
    val httpClientProvider = HttpClientProvider(baseUrl, apiKey, logLevel)
    val httpClient = httpClientProvider.createClient()
    return OpenAIApiService(httpClient)
}