package me.shadykhalifa.whispertop.data.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WhisperModelsTest {

    @Test
    fun whisperModel_fromString_returnsCorrectModel() {
        assertEquals(WhisperModel.WHISPER_1, WhisperModel.fromString("whisper-1"))
        assertEquals(WhisperModel.GPT_4O_TRANSCRIBE, WhisperModel.fromString("gpt-4o-transcribe"))
        assertEquals(WhisperModel.GPT_4O_MINI_TRANSCRIBE, WhisperModel.fromString("gpt-4o-mini-transcribe"))
    }

    @Test
    fun whisperModel_fromString_withInvalidModel_returnsNull() {
        assertNull(WhisperModel.fromString("invalid-model"))
        assertNull(WhisperModel.fromString(""))
        assertNull(WhisperModel.fromString("whisper-2"))
    }

    @Test
    fun whisperModel_modelId_returnsCorrectId() {
        assertEquals("whisper-1", WhisperModel.WHISPER_1.modelId)
        assertEquals("gpt-4o-transcribe", WhisperModel.GPT_4O_TRANSCRIBE.modelId)
        assertEquals("gpt-4o-mini-transcribe", WhisperModel.GPT_4O_MINI_TRANSCRIBE.modelId)
    }

    @Test
    fun audioResponseFormat_fromString_returnsCorrectFormat() {
        assertEquals(AudioResponseFormat.JSON, AudioResponseFormat.fromString("json"))
        assertEquals(AudioResponseFormat.TEXT, AudioResponseFormat.fromString("text"))
        assertEquals(AudioResponseFormat.SRT, AudioResponseFormat.fromString("srt"))
        assertEquals(AudioResponseFormat.VERBOSE_JSON, AudioResponseFormat.fromString("verbose_json"))
        assertEquals(AudioResponseFormat.VTT, AudioResponseFormat.fromString("vtt"))
    }

    @Test
    fun audioResponseFormat_fromString_withInvalidFormat_returnsNull() {
        assertNull(AudioResponseFormat.fromString("invalid-format"))
        assertNull(AudioResponseFormat.fromString(""))
        assertNull(AudioResponseFormat.fromString("xml"))
    }

    @Test
    fun audioResponseFormat_format_returnsCorrectString() {
        assertEquals("json", AudioResponseFormat.JSON.format)
        assertEquals("text", AudioResponseFormat.TEXT.format)
        assertEquals("srt", AudioResponseFormat.SRT.format)
        assertEquals("verbose_json", AudioResponseFormat.VERBOSE_JSON.format)
        assertEquals("vtt", AudioResponseFormat.VTT.format)
    }
}