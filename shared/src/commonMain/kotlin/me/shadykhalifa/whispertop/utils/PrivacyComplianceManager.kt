package me.shadykhalifa.whispertop.utils

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable

/**
 * Privacy compliance manager for handling data protection regulations (GDPR, CCPA, etc.)
 */
@Serializable
data class PrivacySettings(
    val enableDataCollection: Boolean = true,
    val enableAnalytics: Boolean = false,
    val enableTranscriptionStorage: Boolean = true,
    val enableAppUsageTracking: Boolean = false,
    val dataRetentionDays: Int = 90,
    val anonymizeAfterDays: Int = 30,
    val lastConsentUpdate: Long = 0,
    val consentVersion: String = "1.0"
)

@Serializable
data class DataProcessingPurpose(
    val id: String,
    val name: String,
    val description: String,
    val isRequired: Boolean,
    val dataTypes: List<String>,
    val retentionPeriodDays: Int
)

@Serializable
data class ConsentRecord(
    val userId: String,
    val timestamp: Long,
    val purposes: Map<String, Boolean>, // purpose ID -> granted
    val version: String,
    val ipAddress: String? = null,
    val deviceId: String? = null
)

object PrivacyComplianceManager {
    
    // Standard data processing purposes
    val CORE_FUNCTIONALITY = DataProcessingPurpose(
        id = "core_functionality",
        name = "Core Functionality",
        description = "Essential features like speech-to-text transcription",
        isRequired = true,
        dataTypes = listOf("audio_data", "transcription_text", "session_metrics"),
        retentionPeriodDays = 30
    )
    
    val PERFORMANCE_ANALYTICS = DataProcessingPurpose(
        id = "performance_analytics",
        name = "Performance Analytics",
        description = "App performance monitoring and improvement",
        isRequired = false,
        dataTypes = listOf("usage_metrics", "error_logs", "performance_data"),
        retentionPeriodDays = 90
    )
    
    val PERSONALIZATION = DataProcessingPurpose(
        id = "personalization",
        name = "Personalization",
        description = "Customize app experience based on usage patterns",
        isRequired = false,
        dataTypes = listOf("usage_patterns", "preferences", "app_interactions"),
        retentionPeriodDays = 365
    )
    
    private val allPurposes = listOf(CORE_FUNCTIONALITY, PERFORMANCE_ANALYTICS, PERSONALIZATION)
    
    /**
     * Get all available data processing purposes
     */
    fun getDataProcessingPurposes(): List<DataProcessingPurpose> = allPurposes
    
    /**
     * Get required purposes that cannot be opted out
     */
    fun getRequiredPurposes(): List<DataProcessingPurpose> = allPurposes.filter { it.isRequired }
    
    /**
     * Validate privacy settings against regulations
     */
    fun validatePrivacySettings(settings: PrivacySettings): ValidationResult {
        val errors = mutableListOf<String>()
        
        // GDPR compliance checks
        if (settings.dataRetentionDays > 1095) { // 3 years max
            errors.add("Data retention period exceeds GDPR recommendations (max 3 years)")
        }
        
        if (settings.anonymizeAfterDays > settings.dataRetentionDays) {
            errors.add("Anonymization period must be less than or equal to retention period")
        }
        
        if (settings.enableAnalytics && settings.enableAppUsageTracking) {
            val consentAge = Clock.System.now().toEpochMilliseconds() - settings.lastConsentUpdate
            val maxConsentAge = 365 * 24 * 60 * 60 * 1000L // 1 year
            
            if (consentAge > maxConsentAge) {
                errors.add("Analytics consent is older than 1 year and needs renewal")
            }
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(errors)
        }
    }
    
    /**
     * Apply privacy-preserving transformations to data
     */
    fun applyPrivacyTransformations(
        data: String,
        settings: PrivacySettings,
        dataAge: Long
    ): String {
        val ageInDays = (Clock.System.now().toEpochMilliseconds() - dataAge) / (24 * 60 * 60 * 1000L)
        
        return when {
            // Anonymize old data
            ageInDays > settings.anonymizeAfterDays -> anonymizeData(data)
            // Return as-is for recent data (SQLCipher handles encryption at database level)
            else -> data
        }
    }
    
    /**
     * Check if data collection is allowed for a specific purpose
     */
    fun isDataCollectionAllowed(purposeId: String, consent: ConsentRecord?): Boolean {
        if (consent == null) {
            // Only allow required purposes without explicit consent
            return getRequiredPurposes().any { it.id == purposeId }
        }
        
        // Check if consent is still valid (not older than 2 years)
        val consentAge = Clock.System.now().toEpochMilliseconds() - consent.timestamp
        val maxConsentAge = 2 * 365 * 24 * 60 * 60 * 1000L // 2 years
        
        if (consentAge > maxConsentAge) {
            return getRequiredPurposes().any { it.id == purposeId }
        }
        
        return consent.purposes[purposeId] == true
    }
    
    /**
     * Generate data processing record for audit trail
     */
    fun createDataProcessingRecord(
        operation: String,
        dataTypes: List<String>,
        purposeId: String,
        userId: String? = null
    ): DataProcessingRecord {
        return DataProcessingRecord(
            timestamp = Clock.System.now().toEpochMilliseconds(),
            operation = operation,
            dataTypes = dataTypes,
            purposeId = purposeId,
            userId = userId,
            legalBasis = getLegalBasis(purposeId)
        )
    }
    
    /**
     * Check if data should be deleted based on retention policy
     */
    fun shouldDeleteData(
        dataTimestamp: Long,
        purposeId: String,
        settings: PrivacySettings
    ): Boolean {
        val purpose = allPurposes.find { it.id == purposeId }
        val retentionDays = purpose?.retentionPeriodDays ?: settings.dataRetentionDays
        
        val dataAge = Clock.System.now().toEpochMilliseconds() - dataTimestamp
        val maxAge = retentionDays * 24 * 60 * 60 * 1000L
        
        return dataAge > maxAge
    }
    
    /**
     * Generate privacy report for user
     */
    fun generatePrivacyReport(
        userId: String,
        consent: ConsentRecord?,
        settings: PrivacySettings
    ): PrivacyReport {
        val currentDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        
        return PrivacyReport(
            userId = userId,
            generatedDate = currentDate,
            consentStatus = consent?.let { "Granted on ${getDateFromTimestamp(it.timestamp)}" } ?: "Not provided",
            dataRetentionPeriod = "${settings.dataRetentionDays} days",
            anonymizationPeriod = "${settings.anonymizeAfterDays} days",
            enabledPurposes = consent?.purposes?.filter { it.value }?.keys?.toList() ?: emptyList(),
            dataTypes = getDataTypesForUser(consent),
            nextReviewDate = getDateFromTimestamp(
                (consent?.timestamp ?: 0) + (365 * 24 * 60 * 60 * 1000L)
            )
        )
    }
    
    private fun anonymizeData(data: String): String {
        // Simple anonymization strategy - replace with generic placeholder
        // More sophisticated anonymization could be implemented here based on data type
        return when {
            data.contains("@") -> "[EMAIL_ANONYMIZED]"
            data.matches(Regex("\\d{3}-\\d{3}-\\d{4}")) -> "[PHONE_ANONYMIZED]"
            data.length > 50 -> "[LONG_TEXT_ANONYMIZED]"
            else -> "[ANONYMIZED]"
        }
    }
    
    private fun getLegalBasis(purposeId: String): String {
        return when (purposeId) {
            CORE_FUNCTIONALITY.id -> "Legitimate Interest"
            PERFORMANCE_ANALYTICS.id -> "Consent"
            PERSONALIZATION.id -> "Consent"
            else -> "Consent"
        }
    }
    
    private fun getDataTypesForUser(consent: ConsentRecord?): List<String> {
        if (consent == null) {
            return getRequiredPurposes().flatMap { it.dataTypes }
        }
        
        return allPurposes
            .filter { consent.purposes[it.id] == true }
            .flatMap { it.dataTypes }
    }
    
    private fun getDateFromTimestamp(timestamp: Long): LocalDate {
        return kotlinx.datetime.Instant.fromEpochMilliseconds(timestamp)
            .toLocalDateTime(TimeZone.currentSystemDefault()).date
    }
}

@Serializable
data class DataProcessingRecord(
    val timestamp: Long,
    val operation: String,
    val dataTypes: List<String>,
    val purposeId: String,
    val userId: String?,
    val legalBasis: String
)

@Serializable
data class PrivacyReport(
    val userId: String,
    val generatedDate: LocalDate,
    val consentStatus: String,
    val dataRetentionPeriod: String,
    val anonymizationPeriod: String,
    val enabledPurposes: List<String>,
    val dataTypes: List<String>,
    val nextReviewDate: LocalDate
)

sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val errors: List<String>) : ValidationResult()
}