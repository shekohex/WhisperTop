package me.shadykhalifa.whispertop.data.remote

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.http.*
import me.shadykhalifa.whispertop.data.models.*

expect fun apiLog(tag: String, message: String)
expect fun apiLogError(tag: String, message: String, throwable: Throwable? = null)

class OpenAIApiService(
    private val httpClient: HttpClient
) {
    private companion object {
        const val TAG = "OpenAIApiService"
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
        apiLog(TAG, "Starting transcription: file=$fileName, model=${model.modelId}, language=$language, format=${responseFormat.format}")
        apiLog(TAG, "Audio data size: ${audioData.size} bytes")
        
        validateParameters(model, temperature)
        
        val request = CreateTranscriptionRequestDto(
            model = model.modelId,
            language = language,
            prompt = prompt,
            responseFormat = responseFormat.format,
            temperature = temperature
        )
        
        val contentType = determineContentType(fileName)
        apiLog(TAG, "Determined content type: $contentType")
        
        apiLog(TAG, "Uploading audio file to OpenAI API...")
        val response = httpClient.uploadAudioFile(
            endpoint = "audio/transcriptions",
            audioData = audioData,
            fileName = fileName,
            contentType = contentType,
            transcriptionRequest = request
        )
        
        apiLog(TAG, "API response received: ${response.status}")
        response.validateOrThrow()
        
        return when (responseFormat) {
            AudioResponseFormat.VERBOSE_JSON -> {
                apiLog(TAG, "Parsing verbose JSON response...")
                val verboseResponse = response.body<CreateTranscriptionResponseVerboseDto>()
                apiLog(TAG, "Verbose response parsed: text length=${verboseResponse.text?.length ?: 0}")
                CreateTranscriptionResponseDto(text = verboseResponse.text)
            }
            else -> {
                apiLog(TAG, "Parsing standard JSON response...")
                val result = response.body<CreateTranscriptionResponseDto>()
                apiLog(TAG, "Standard response parsed: text length=${result.text?.length ?: 0}")
                result
            }
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
        apiLog(TAG, "Starting transcription with language detection: file=$fileName, model=${model.modelId}")
        apiLog(TAG, "Language detection params: language=$language, audio size=${audioData.size} bytes")
        
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
        
        apiLog(TAG, "Language detection API response received: ${response.status}")
        response.validateOrThrow()
        
        apiLog(TAG, "Parsing verbose response for language detection...")
        val result = response.body<CreateTranscriptionResponseVerboseDto>()
        apiLog(TAG, "Language detection completed: text='${result.text}', detected_language='${result.language}'")
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