package me.shadykhalifa.whispertop.domain.models

import kotlinx.serialization.Serializable

/**
 * Represents supported languages for speech recognition with their ISO-639-1 codes
 * Based on OpenAI Whisper API language support
 */
@Serializable
enum class Language(
    val code: String,
    val displayName: String,
    val nativeName: String
) {
    // Auto-detect (no language specified)
    AUTO("", "Auto-detect", "Auto-detect"),
    
    // Common languages supported by OpenAI Whisper
    ENGLISH("en", "English", "English"),
    SPANISH("es", "Spanish", "Español"),
    FRENCH("fr", "French", "Français"),
    GERMAN("de", "German", "Deutsch"),
    ITALIAN("it", "Italian", "Italiano"),
    PORTUGUESE("pt", "Portuguese", "Português"),
    RUSSIAN("ru", "Russian", "Русский"),
    CHINESE("zh", "Chinese", "中文"),
    JAPANESE("ja", "Japanese", "日本語"),
    KOREAN("ko", "Korean", "한국어"),
    ARABIC("ar", "Arabic", "العربية"),
    HINDI("hi", "Hindi", "हिन्दी"),
    TURKISH("tr", "Turkish", "Türkçe"),
    DUTCH("nl", "Dutch", "Nederlands"),
    POLISH("pl", "Polish", "Polski"),
    SWEDISH("sv", "Swedish", "Svenska"),
    DANISH("da", "Danish", "Dansk"),
    NORWEGIAN("no", "Norwegian", "Norsk"),
    FINNISH("fi", "Finnish", "Suomi"),
    GREEK("el", "Greek", "Ελληνικά"),
    HEBREW("he", "Hebrew", "עברית"),
    CZECH("cs", "Czech", "Čeština"),
    HUNGARIAN("hu", "Hungarian", "Magyar"),
    ROMANIAN("ro", "Romanian", "Română"),
    UKRAINIAN("uk", "Ukrainian", "Українська"),
    VIETNAMESE("vi", "Vietnamese", "Tiếng Việt"),
    THAI("th", "Thai", "ไทย"),
    INDONESIAN("id", "Indonesian", "Bahasa Indonesia"),
    MALAY("ms", "Malay", "Bahasa Melayu"),
    FILIPINO("tl", "Filipino", "Filipino"),
    BENGALI("bn", "Bengali", "বাংলা"),
    TAMIL("ta", "Tamil", "தமிழ்"),
    TELUGU("te", "Telugu", "తెలుగు"),
    MARATHI("mr", "Marathi", "मराठी"),
    GUJARATI("gu", "Gujarati", "ગુજરાતી"),
    KANNADA("kn", "Kannada", "ಕನ್ನಡ"),
    MALAYALAM("ml", "Malayalam", "മലയാളം"),
    PUNJABI("pa", "Punjabi", "ਪੰਜਾਬੀ"),
    URDU("ur", "Urdu", "اردو"),
    PERSIAN("fa", "Persian", "فارسی"),
    SWAHILI("sw", "Swahili", "Kiswahili"),
    AFRIKAANS("af", "Afrikaans", "Afrikaans"),
    WELSH("cy", "Welsh", "Cymraeg"),
    IRISH("ga", "Irish", "Gaeilge"),
    SCOTTISH_GAELIC("gd", "Scottish Gaelic", "Gàidhlig"),
    BASQUE("eu", "Basque", "Euskera"),
    CATALAN("ca", "Catalan", "Català"),
    GALICIAN("gl", "Galician", "Galego"),
    BULGARIAN("bg", "Bulgarian", "Български"),
    CROATIAN("hr", "Croatian", "Hrvatski"),
    SERBIAN("sr", "Serbian", "Српски"),
    SLOVENIAN("sl", "Slovenian", "Slovenščina"),
    SLOVAK("sk", "Slovak", "Slovenčina"),
    LITHUANIAN("lt", "Lithuanian", "Lietuvių"),
    LATVIAN("lv", "Latvian", "Latviešu"),
    ESTONIAN("et", "Estonian", "Eesti"),
    MALTESE("mt", "Maltese", "Malti"),
    ICELANDIC("is", "Icelandic", "Íslenska"),
    ALBANIAN("sq", "Albanian", "Shqip"),
    MACEDONIAN("mk", "Macedonian", "Македонски");

    companion object {
        /**
         * Find language by ISO-639-1 code
         */
        fun fromCode(code: String?): Language? {
            if (code.isNullOrBlank()) return AUTO
            return entries.find { it.code.equals(code, ignoreCase = true) }
        }

        /**
         * Get popular languages for quick selection
         */
        fun getPopularLanguages(): List<Language> = listOf(
            AUTO, ENGLISH, SPANISH, FRENCH, GERMAN, ITALIAN, 
            PORTUGUESE, RUSSIAN, CHINESE, JAPANESE, KOREAN, ARABIC, HINDI
        )

        /**
         * Get all supported languages except AUTO
         */
        fun getSupportedLanguages(): List<Language> = entries.filter { it != AUTO }
    }

    /**
     * Get the code to send to OpenAI API (empty string for auto-detect)
     */
    fun getApiCode(): String? = if (this == AUTO) null else code

    /**
     * Get display name with native name if different
     */
    fun getFullDisplayName(): String = 
        if (displayName == nativeName) displayName
        else "$displayName ($nativeName)"
}

/**
 * Represents the result of automatic language detection
 */
@Serializable
data class LanguageDetectionResult(
    val detectedLanguage: Language,
    val confidence: Float? = null,
    val isManualOverride: Boolean = false
) {
    companion object {
        fun autoDetected(language: Language, confidence: Float? = null) = 
            LanguageDetectionResult(language, confidence, false)
        
        fun manualOverride(language: Language) = 
            LanguageDetectionResult(language, null, true)
    }

    /**
     * Get confidence as percentage string
     */
    fun getConfidencePercentage(): String? = 
        confidence?.let { "${(it * 100).toInt()}%" }
}

/**
 * Language preference settings
 */
@Serializable
data class LanguagePreference(
    val preferredLanguage: Language = Language.AUTO,
    val autoDetectEnabled: Boolean = true,
    val showConfidence: Boolean = true,
    val allowManualOverride: Boolean = true
)