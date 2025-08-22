package me.shadykhalifa.whispertop.utils

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.abs

object PrivacyUtils {
    
    /**
     * Hash sensitive text for privacy while preserving metrics
     */
    fun hashSensitiveText(text: String): String {
        if (text.isBlank()) return ""
        
        // Use a simple hash that's consistent but not reversible
        val hash = abs(text.hashCode()).toString(16)
        val wordCount = countWords(text)
        val charCount = text.length
        
        return "HASH_${hash}_W${wordCount}_C${charCount}"
    }
    
    /**
     * Create anonymized version of transcription text for storage
     */
    fun anonymizeTranscription(text: String, preserveMetrics: Boolean = true): String {
        if (text.isBlank()) return ""
        
        return if (preserveMetrics) {
            // Keep statistical properties but remove actual content
            val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }
            val avgWordLength = if (words.isNotEmpty()) words.map { it.length }.average() else 0.0
            val sentenceCount = text.split(Regex("[.!?]+")).filter { it.trim().isNotBlank() }.size
            
            Json.encodeToString(mapOf(
                "type" to "anonymized",
                "wordCount" to words.size,
                "characterCount" to text.length,
                "averageWordLength" to avgWordLength,
                "sentenceCount" to sentenceCount,
                "hash" to hashSensitiveText(text)
            ))
        } else {
            "[ANONYMIZED_TRANSCRIPTION]"
        }
    }
    
    /**
     * Truncate transcription for storage while preserving privacy
     */
    fun truncateForStorage(text: String, maxLength: Int = 100): String {
        if (text.length <= maxLength) return text
        
        val truncated = text.take(maxLength)
        val lastSpace = truncated.lastIndexOf(' ')
        
        return if (lastSpace > maxLength / 2) {
            truncated.take(lastSpace) + "..."
        } else {
            truncated + "..."
        }
    }
    
    /**
     * Determine if text contains sensitive information
     */
    fun containsSensitiveInfo(text: String): Boolean {
        val sensitivePatterns = listOf(
            // Phone numbers
            Regex("\\b\\d{3}[-.\\s]?\\d{3}[-.\\s]?\\d{4}\\b"),
            // Email addresses
            Regex("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b"),
            // Credit card numbers (basic pattern)
            Regex("\\b\\d{4}[-.\\s]?\\d{4}[-.\\s]?\\d{4}[-.\\s]?\\d{4}\\b"),
            // SSN pattern
            Regex("\\b\\d{3}[-.\\s]?\\d{2}[-.\\s]?\\d{4}\\b"),
            // Address patterns (basic)
            Regex("\\b\\d+\\s+[A-Za-z]+\\s+(Street|St|Avenue|Ave|Road|Rd|Drive|Dr|Lane|Ln)\\b", RegexOption.IGNORE_CASE)
        )
        
        return sensitivePatterns.any { it.containsMatchIn(text) }
    }
    
    /**
     * Sanitize text by removing or masking sensitive information
     */
    fun sanitizeText(text: String): String {
        var sanitized = text
        
        // Mask phone numbers
        sanitized = sanitized.replace(
            Regex("\\b\\d{3}[-.\\s]?\\d{3}[-.\\s]?\\d{4}\\b"),
            "[PHONE]"
        )
        
        // Mask email addresses
        sanitized = sanitized.replace(
            Regex("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b"),
            "[EMAIL]"
        )
        
        // Mask credit card numbers
        sanitized = sanitized.replace(
            Regex("\\b\\d{4}[-.\\s]?\\d{4}[-.\\s]?\\d{4}[-.\\s]?\\d{4}\\b"),
            "[CARD]"
        )
        
        // Mask SSN
        sanitized = sanitized.replace(
            Regex("\\b\\d{3}[-.\\s]?\\d{2}[-.\\s]?\\d{4}\\b"),
            "[SSN]"
        )
        
        return sanitized
    }
    
    /**
     * Create privacy-safe summary for logging
     */
    fun createLogSafeSummary(text: String): String {
        return "Length: ${text.length}, Words: ${countWords(text)}, " +
               "Sensitive: ${containsSensitiveInfo(text)}, " +
               "Hash: ${abs(text.hashCode()).toString(16)}"
    }
    
    private fun countWords(text: String): Int {
        if (text.isBlank()) return 0
        return text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size
    }
}