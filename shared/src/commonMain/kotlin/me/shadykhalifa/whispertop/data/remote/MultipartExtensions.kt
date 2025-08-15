package me.shadykhalifa.whispertop.data.remote

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.core.*
import me.shadykhalifa.whispertop.data.models.CreateTranscriptionRequestDto
import me.shadykhalifa.whispertop.data.models.MultipartTranscriptionRequest

suspend fun HttpClient.postMultipartTranscription(
    url: String,
    request: MultipartTranscriptionRequest
): HttpResponse {
    return submitFormWithBinaryData(
        url = url,
        formData = formData {
            append("file", request.audioData, Headers.build {
                append(HttpHeaders.ContentType, request.contentType)
                append(HttpHeaders.ContentDisposition, "filename=\"${request.fileName}\"")
            })
            append("model", request.request.model)
            request.request.language?.let { append("language", it) }
            request.request.prompt?.let { append("prompt", it) }
            append("response_format", request.request.responseFormat)
            append("temperature", request.request.temperature.toString())
        }
    ) {
        method = HttpMethod.Post
    }
}

suspend fun HttpClient.uploadAudioFile(
    endpoint: String = "audio/transcriptions",
    audioData: ByteArray,
    fileName: String,
    contentType: String = "audio/wav",
    transcriptionRequest: CreateTranscriptionRequestDto
): HttpResponse {
    val multipartRequest = MultipartTranscriptionRequest(
        audioData = audioData,
        fileName = fileName,
        contentType = contentType,
        request = transcriptionRequest
    )
    
    return postMultipartTranscription(endpoint, multipartRequest)
}

suspend fun HttpClient.uploadWavFile(
    audioData: ByteArray,
    fileName: String,
    model: String = "whisper-1",
    language: String? = null,
    responseFormat: String = "json",
    temperature: Float = 0.0f
): HttpResponse {
    val request = CreateTranscriptionRequestDto(
        model = model,
        language = language,
        responseFormat = responseFormat,
        temperature = temperature
    )
    
    return uploadAudioFile(
        audioData = audioData,
        fileName = fileName,
        contentType = "audio/wav",
        transcriptionRequest = request
    )
}