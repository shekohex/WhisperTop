package me.shadykhalifa.whispertop.domain.models

import kotlinx.serialization.Serializable

/**
 * Data retention policy configuration
 */
@Serializable
data class RetentionPolicy(
    val id: String,
    val name: String,
    val description: String,
    val retentionDays: Int?, // null means unlimited
    val isDefault: Boolean = false,
    val isProtected: Boolean = false
) {
    companion object {
        val SEVEN_DAYS = RetentionPolicy(
            id = "retention_7_days",
            name = "7 Days",
            description = "Delete transcriptions after 7 days",
            retentionDays = 7
        )
        
        val THIRTY_DAYS = RetentionPolicy(
            id = "retention_30_days", 
            name = "30 Days",
            description = "Delete transcriptions after 30 days",
            retentionDays = 30,
            isDefault = true
        )
        
        val NINETY_DAYS = RetentionPolicy(
            id = "retention_90_days",
            name = "90 Days", 
            description = "Delete transcriptions after 90 days",
            retentionDays = 90
        )
        
        val UNLIMITED = RetentionPolicy(
            id = "retention_unlimited",
            name = "Keep Forever",
            description = "Never automatically delete transcriptions",
            retentionDays = null
        )
        
        val PROTECTED = RetentionPolicy(
            id = "retention_protected",
            name = "Protected",
            description = "Manually protected from automatic deletion",
            retentionDays = null,
            isProtected = true
        )
        
        fun getAllPolicies(): List<RetentionPolicy> = listOf(
            SEVEN_DAYS,
            THIRTY_DAYS, 
            NINETY_DAYS,
            UNLIMITED,
            PROTECTED
        )
        
        fun getDefaultPolicy(): RetentionPolicy = THIRTY_DAYS
        
        fun findById(id: String): RetentionPolicy? = getAllPolicies().find { it.id == id }
    }
}

/**
 * Configuration for data retention and cleanup
 */
@Serializable 
data class RetentionConfiguration(
    val defaultPolicyId: String = RetentionPolicy.THIRTY_DAYS.id,
    val enableAutomaticCleanup: Boolean = true,
    val cleanupFrequencyHours: Int = 24, // Run daily
    val backupBeforeDelete: Boolean = true,
    val notifyBeforeCleanup: Boolean = true,
    val gracePeriodDays: Int = 3 // Additional days before actual deletion
) {
    companion object {
        fun default(): RetentionConfiguration = RetentionConfiguration()
    }
}