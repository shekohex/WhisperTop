package me.shadykhalifa.whispertop.data.repositories

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.shadykhalifa.whispertop.data.models.OpenAIException
import me.shadykhalifa.whispertop.data.models.TranscriptionRequestDto
import me.shadykhalifa.whispertop.data.models.TranscriptionResponseDto
import me.shadykhalifa.whispertop.data.models.toDomain
import me.shadykhalifa.whispertop.domain.models.TranscriptionError
import me.shadykhalifa.whispertop.domain.models.TranscriptionRequest
import me.shadykhalifa.whispertop.domain.models.TranscriptionResponse
import me.shadykhalifa.whispertop.domain.repositories.SettingsRepository
import me.shadykhalifa.whispertop.domain.repositories.TranscriptionRepository
import me.shadykhalifa.whispertop.domain.services.FileReaderService
import me.shadykhalifa.whispertop.utils.Result
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class TranscriptionRepositoryImpl(
    private val httpClient: HttpClient,
    private val settingsRepository: SettingsRepository,
    private val fileReaderService: FileReaderService
) : BaseRepository(), TranscriptionRepository {

    private companion object {
        const val OPENAI_BASE_URL = "https://api.openai.com/v1"
        const val TRANSCRIPTIONS_ENDPOINT = "$OPENAI_BASE_URL/audio/transcriptions"
    }

    override suspend fun transcribe(request: TranscriptionRequest): Result<TranscriptionResponse> = execute {
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
            
            val response = httpClient.submitFormWithBinaryData(
                url = TRANSCRIPTIONS_ENDPOINT,
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

            if (!response.status.isSuccess()) {
                when (response.status.value) {
                    401 -> throw TranscriptionError.AuthenticationError()
                    429 -> throw TranscriptionError.RateLimitError()
                    in 500..599 -> throw TranscriptionError.ApiError(response.status.value, "Server error")
                    else -> throw TranscriptionError.ApiError(response.status.value, "API error: ${response.status}")
                }
            }

            val transcriptionResponse: TranscriptionResponseDto = response.body()
            transcriptionResponse.toDomain()
            
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

    override suspend fun validateApiKey(apiKey: String): Result<Boolean> = execute {
        if (apiKey.isBlank()) {
            return@execute false
        }

        try {
            val response = httpClient.get("$OPENAI_BASE_URL/models") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $apiKey")
                }
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            false
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
}

expect class FileReader {
    suspend fun readFileAsBytes(filePath: String): ByteArray
}