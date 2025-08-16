package me.shadykhalifa.whispertop.data.remote

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.http.*
import me.shadykhalifa.whispertop.data.models.*

class OpenAIApiService(
    private val httpClient: HttpClient
) {
    suspend fun transcribe(
        audioData: ByteArray,
        fileName: String,
        model: WhisperModel = WhisperModel.WHISPER_1,
        language: String? = null,
        prompt: String? = null,
        responseFormat: AudioResponseFormat = AudioResponseFormat.JSON,
        temperature: Float = 0.0f
    ): CreateTranscriptionResponseDto {
        validateParameters(model, temperature)
        
        val request = CreateTranscriptionRequestDto(
            model = model.modelId,
            language = language,
            prompt = prompt,
            responseFormat = responseFormat.format,
            temperature = temperature
        )
        
        val contentType = determineContentType(fileName)
        
        val response = httpClient.uploadAudioFile(
            endpoint = "audio/transcriptions",
            audioData = audioData,
            fileName = fileName,
            contentType = contentType,
            transcriptionRequest = request
        )
        
        response.validateOrThrow()
        
        return when (responseFormat) {
            AudioResponseFormat.VERBOSE_JSON -> {
                val verboseResponse = response.body<CreateTranscriptionResponseVerboseDto>()
                CreateTranscriptionResponseDto(text = verboseResponse.text)
            }
            else -> response.body<CreateTranscriptionResponseDto>()
        }
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
        validateParameters(model, temperature)
        
        val request = CreateTranscriptionRequestDto(
            model = model.modelId,
            language = language,
            prompt = prompt,
            responseFormat = AudioResponseFormat.VERBOSE_JSON.format,
            temperature = temperature
        )
        
        val contentType = determineContentType(fileName)
        
        val response = httpClient.uploadAudioFile(
            endpoint = "audio/transcriptions",
            audioData = audioData,
            fileName = fileName,
            contentType = contentType,
            transcriptionRequest = request
        )
        
        response.validateOrThrow()
        return response.body<CreateTranscriptionResponseVerboseDto>()
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
        val whisperModel = WhisperModel.fromString(model) 
            ?: throw IllegalArgumentException("Unsupported model: $model")
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