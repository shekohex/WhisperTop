package me.shadykhalifa.whispertop.presentation.ui.utils

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.Locale

object TextUtils {
    
    @Composable
    fun buildSyntaxHighlightedText(text: String): AnnotatedString {
        val primaryColor = MaterialTheme.colorScheme.primary
        val secondaryColor = MaterialTheme.colorScheme.secondary
        
        return buildAnnotatedString {
            append(text)
            
            // Highlight URLs
            val urlPattern = Regex("""https?://[^\s]+""")
            urlPattern.findAll(text).forEach { match ->
                addStyle(
                    style = SpanStyle(
                        color = primaryColor,
                        textDecoration = TextDecoration.Underline,
                        fontWeight = FontWeight.Medium
                    ),
                    start = match.range.first,
                    end = match.range.last + 1
                )
                addStringAnnotation(
                    tag = "URL",
                    annotation = match.value,
                    start = match.range.first,
                    end = match.range.last + 1
                )
            }
            
            // Highlight email addresses
            val emailPattern = Regex("""[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}""")
            emailPattern.findAll(text).forEach { match ->
                addStyle(
                    style = SpanStyle(
                        color = secondaryColor,
                        textDecoration = TextDecoration.Underline,
                        fontWeight = FontWeight.Medium
                    ),
                    start = match.range.first,
                    end = match.range.last + 1
                )
                addStringAnnotation(
                    tag = "EMAIL",
                    annotation = match.value,
                    start = match.range.first,
                    end = match.range.last + 1
                )
            }
            
            // Highlight phone numbers
            val phonePattern = Regex("""(\+?1[-.\s]?)?\(?[0-9]{3}\)?[-.\s]?[0-9]{3}[-.\s]?[0-9]{4}""")
            phonePattern.findAll(text).forEach { match ->
                addStyle(
                    style = SpanStyle(
                        color = primaryColor,
                        fontWeight = FontWeight.Medium
                    ),
                    start = match.range.first,
                    end = match.range.last + 1
                )
                addStringAnnotation(
                    tag = "PHONE",
                    annotation = match.value,
                    start = match.range.first,
                    end = match.range.last + 1
                )
            }
        }
    }
    
    fun formatTimestamp(timestamp: Long): String {
        return try {
            val instant = Instant.fromEpochMilliseconds(timestamp)
            val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            
            val month = localDateTime.month.name.take(3).lowercase()
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            val day = localDateTime.dayOfMonth.toString().padStart(2, '0')
            val year = localDateTime.year
            val hour = localDateTime.hour.toString().padStart(2, '0')
            val minute = localDateTime.minute.toString().padStart(2, '0')
            
            "$month $day, $year at $hour:$minute"
        } catch (e: Exception) {
            "Invalid date"
        }
    }
    
    fun formatDuration(durationMs: Float?): String {
        if (durationMs == null || durationMs <= 0f) return "N/A"
        
        val totalSeconds = (durationMs / 1000).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
    
    fun formatFileSize(sizeBytes: Long?): String {
        if (sizeBytes == null || sizeBytes <= 0) return "N/A"
        
        val kb = sizeBytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        
        return when {
            gb >= 1.0 -> String.format(Locale.getDefault(), "%.1f GB", gb)
            mb >= 1.0 -> String.format(Locale.getDefault(), "%.1f MB", mb)
            kb >= 1.0 -> String.format(Locale.getDefault(), "%.1f KB", kb)
            else -> "$sizeBytes bytes"
        }
    }
    
    fun getWordCount(text: String): Int {
        return if (text.isBlank()) 0 else text.trim().split(Regex("\\s+")).size
    }
    
    fun getCharacterCount(text: String): Int {
        return text.length
    }
}