package me.shadykhalifa.whispertop.utils

object TestConstants {
    // Safe mock API keys that are clearly for testing only
    const val MOCK_API_KEY = "MOCK-sk-test1234567890abcdefghijklmnopqrstuvwxyz1234567890ab"
    const val MOCK_OPENAI_API_KEY = "TEST-sk-demo1234567890abcdefghijklmnopqrstuvwxyz1234567890ab"
    const val MOCK_INVALID_API_KEY = "INVALID_TEST_KEY"
    const val MOCK_EMPTY_API_KEY = ""
    
    // Mock JWT tokens for testing
    const val MOCK_JWT_TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.TEST.MOCK"
    const val MOCK_BEARER_TOKEN = "MOCK_BEARER_TOKEN_FOR_TESTING"
    
    // Mock API endpoints
    const val MOCK_API_ENDPOINT = "https://api.test.com"
    const val MOCK_OPENAI_ENDPOINT = "https://api.openai.test.com"
    
    // Mock models
    const val MOCK_WHISPER_MODEL = "whisper-test"
    const val MOCK_GPT_MODEL = "gpt-test"
    
    // Mock user data
    const val MOCK_USER_ID = "test_user_123"
    const val MOCK_SESSION_ID = "test_session_456"
    
    // Mock file paths
    const val MOCK_AUDIO_FILE_PATH = "/test/path/audio.wav"
    const val MOCK_TEMP_FILE_PATH = "/test/temp/file.tmp"
    
    // Mock transcription text
    const val MOCK_TRANSCRIPTION_TEXT = "This is a mock transcription for testing purposes"
    val MOCK_LONG_TRANSCRIPTION_TEXT = """
        This is a longer mock transcription text that spans multiple lines
        and contains various punctuation marks! It includes questions? And
        exclamations! This helps test edge cases in transcription processing
        without exposing any real user data or API keys.
    """.trimIndent()
    
    // Mock durations and metrics
    const val MOCK_DURATION = 5.5f
    const val MOCK_CONFIDENCE = 0.95f
    const val MOCK_WORD_COUNT = 10
    
    // Security test patterns that should be flagged
    val INSECURE_API_KEY_PATTERNS = listOf(
        "sk-",
        "gsk_",
        "sess-",
        "Bearer ",
        "Authorization:",
        "api_key"
    )
    
    // Safe replacement patterns for logs
    val SECURE_LOG_REPLACEMENTS = mapOf(
        Regex("Authorization: Bearer [A-Za-z0-9-._~+/]+=*") to "Authorization: Bearer [REDACTED]",
        Regex("Bearer (MOCK|TEST|FAKE)-sk-[A-Za-z0-9-._~+/]+=*") to "Bearer [REDACTED]",
        Regex("\"api_key\"\\s*:\\s*\"[^\"]+\"") to "\"api_key\": \"[REDACTED]\"",
        Regex("(MOCK|TEST|FAKE)-sk-[A-Za-z0-9]{48}") to "[REDACTED_API_KEY]",
        Regex(Regex.escape(MOCK_API_KEY)) to "[REDACTED_API_KEY]"
    )
    
    // Test validation helpers
    fun isSecureTestKey(key: String): Boolean {
        return key.startsWith("MOCK_") || 
               key.startsWith("TEST_") ||
               key.startsWith("FAKE_") ||
               key == MOCK_API_KEY ||
               key == MOCK_OPENAI_API_KEY
    }
    
    fun containsInsecurePattern(text: String): Boolean {
        return INSECURE_API_KEY_PATTERNS.any { pattern ->
            text.contains(pattern, ignoreCase = true) && 
            !isSecureTestKey(text)
        }
    }
}