package me.shadykhalifa.whispertop.data.models

import kotlinx.serialization.Serializable

@Serializable
enum class WhisperModel(val modelId: String) {
    WHISPER_1("whisper-1"),
    GPT_4O_TRANSCRIBE("gpt-4o-transcribe"),
    GPT_4O_MINI_TRANSCRIBE("gpt-4o-mini-transcribe");

    companion object {
        fun fromString(modelId: String): WhisperModel? {
            return entries.find { it.modelId == modelId }
        }
        
        fun isBuiltIn(modelId: String): Boolean {
            return entries.any { it.modelId == modelId }
        }
        
        fun getBuiltInModels(): List<WhisperModel> = entries.toList()
    }
}

@Serializable
enum class AudioResponseFormat(val format: String) {
    JSON("json"),
    TEXT("text"),
    SRT("srt"),
    VERBOSE_JSON("verbose_json"),
    VTT("vtt");

    companion object {
        fun fromString(format: String): AudioResponseFormat? {
            return entries.find { it.format == format }
        }
    }
}