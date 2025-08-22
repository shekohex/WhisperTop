package me.shadykhalifa.whispertop.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class DatabaseKeyManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "DatabaseKeyManager"
        private const val PREFERENCES_FILE = "whispertop_db_keys"
        private const val DATABASE_KEY_ALIAS = "main_db_key"
        private const val DATABASE_KEY_BACKUP_ALIAS = "main_db_key_backup"
        private const val KEYSTORE_ALIAS = "whispertop_db_master_key"
        private const val KEY_SIZE_BYTES = 32 // 256-bit key for AES-256
        private const val KEY_GENERATION_RETRY_COUNT = 3
        
        @Volatile
        private var INSTANCE: DatabaseKeyManager? = null
        
        fun getInstance(context: Context): DatabaseKeyManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DatabaseKeyManager(context.applicationContext).also { INSTANCE = it }
            }
        }
        
        /**
         * Securely zero out sensitive byte arrays
         */
        fun clearSensitiveData(data: ByteArray) {
            data.fill(0)
        }
    }
    
    private val masterKey: MasterKey by lazy {
        createMasterKeyWithFallback()
    }
    
    private val encryptedPrefs by lazy {
        createEncryptedPreferencesWithRetry()
    }
    
    private fun createMasterKeyWithFallback(): MasterKey {
        return try {
            // First attempt: StrongBox-backed hardware security
            Log.d(TAG, "Attempting to create StrongBox-backed master key")
            MasterKey.Builder(context, KEYSTORE_ALIAS)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .setRequestStrongBoxBacked(true)
                .build()
        } catch (e: Exception) {
            Log.w(TAG, "StrongBox not available, falling back to regular hardware keystore", e)
            logSecurityEvent("master_key_strongbox_fallback", "StrongBox unavailable: ${e.message}")
            
            try {
                // Fallback: Regular hardware-backed keystore
                MasterKey.Builder(context, KEYSTORE_ALIAS)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .setRequestStrongBoxBacked(false)
                    .build()
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to create hardware-backed master key", e2)
                logSecurityEvent("master_key_creation_failed", "Hardware keystore failed: ${e2.message}")
                throw SecurityException("Cannot create secure master key for database encryption", e2)
            }
        }
    }
    
    private fun createEncryptedPreferencesWithRetry(): android.content.SharedPreferences {
        for (attempt in 1..KEY_GENERATION_RETRY_COUNT) {
            try {
                Log.d(TAG, "Creating encrypted preferences, attempt $attempt")
                return EncryptedSharedPreferences.create(
                    context,
                    PREFERENCES_FILE,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed to create encrypted preferences, attempt $attempt", e)
                if (attempt == KEY_GENERATION_RETRY_COUNT) {
                    logSecurityEvent("encrypted_prefs_creation_failed", "All attempts failed: ${e.message}")
                    throw SecurityException("Cannot create secure preferences for key storage", e)
                }
                // Brief delay before retry
                Thread.sleep(100 * attempt.toLong())
            }
        }
        throw IllegalStateException("Unreachable code")
    }
    
    fun getOrCreateDatabaseKey(): ByteArray {
        return try {
            val existingKey = encryptedPrefs.getString(DATABASE_KEY_ALIAS, null)
            
            if (existingKey != null) {
                try {
                    val decodedKey = Base64.decode(existingKey, Base64.NO_WRAP)
                    if (decodedKey.size == KEY_SIZE_BYTES) {
                        Log.d(TAG, "Successfully retrieved existing database key")
                        logSecurityEvent("database_key_retrieved", "Key retrieved successfully")
                        return decodedKey
                    } else {
                        Log.w(TAG, "Retrieved key has invalid size: ${decodedKey.size}, expected: $KEY_SIZE_BYTES")
                        logSecurityEvent("database_key_invalid_size", "Key size: ${decodedKey.size}")
                        clearSensitiveData(decodedKey)
                        return recoverOrGenerateKey()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to decode existing key, attempting recovery", e)
                    logSecurityEvent("database_key_decode_failed", "Decode error: ${e.message}")
                    return recoverOrGenerateKey()
                }
            } else {
                Log.i(TAG, "No existing database key found, generating new key")
                return generateNewDatabaseKeySecure()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Critical error in key management", e)
            logSecurityEvent("database_key_critical_error", "Error: ${e.message}")
            throw SecurityException("Database key management failed", e)
        }
    }
    
    private fun recoverOrGenerateKey(): ByteArray {
        return try {
            // Attempt to recover from backup
            val backupKey = encryptedPrefs.getString(DATABASE_KEY_BACKUP_ALIAS, null)
            if (backupKey != null) {
                Log.i(TAG, "Attempting key recovery from backup")
                val decodedBackup = Base64.decode(backupKey, Base64.NO_WRAP)
                if (decodedBackup.size == KEY_SIZE_BYTES) {
                    // Restore backup as primary key
                    val encodedKey = Base64.encodeToString(decodedBackup, Base64.NO_WRAP)
                    encryptedPrefs.edit()
                        .putString(DATABASE_KEY_ALIAS, encodedKey)
                        .apply()
                    
                    logSecurityEvent("database_key_recovered", "Key recovered from backup")
                    Log.i(TAG, "Successfully recovered database key from backup")
                    return decodedBackup
                } else {
                    clearSensitiveData(decodedBackup)
                }
            }
            
            // Generate new key if recovery fails
            Log.w(TAG, "Key recovery failed, generating new database key")
            logSecurityEvent("database_key_recovery_failed", "Generating new key")
            generateNewDatabaseKeySecure()
        } catch (e: Exception) {
            Log.e(TAG, "Key recovery failed", e)
            logSecurityEvent("database_key_recovery_error", "Recovery error: ${e.message}")
            generateNewDatabaseKeySecure()
        }
    }
    
    private fun generateNewDatabaseKeySecure(): ByteArray {
        var newKey: ByteArray? = null
        var encodedKey: String? = null
        
        return try {
            // Generate cryptographically secure random key
            newKey = ByteArray(KEY_SIZE_BYTES)
            val secureRandom = SecureRandom.getInstanceStrong()
            secureRandom.nextBytes(newKey)
            
            // Verify key entropy (basic check)
            if (newKey.all { it == newKey[0] }) {
                throw SecurityException("Generated key lacks entropy")
            }
            
            // Create backup before storing primary key
            encodedKey = Base64.encodeToString(newKey, Base64.NO_WRAP)
            
            // Store key and backup atomically
            val editor = encryptedPrefs.edit()
            editor.putString(DATABASE_KEY_ALIAS, encodedKey)
            editor.putString(DATABASE_KEY_BACKUP_ALIAS, encodedKey) // Same key as backup
            editor.putLong("key_created_timestamp", System.currentTimeMillis())
            editor.putInt("key_version", 1)
            
            if (!editor.commit()) {
                throw SecurityException("Failed to store database key securely")
            }
            
            Log.i(TAG, "Successfully generated and stored new database key")
            logSecurityEvent("database_key_generated", "New key generated and stored")
            
            // Return copy of the key
            newKey.copyOf()
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate database key", e)
            logSecurityEvent("database_key_generation_failed", "Error: ${e.message}")
            
            // Clean up sensitive data
            newKey?.let { clearSensitiveData(it) }
            
            throw SecurityException("Database key generation failed", e)
        }
    }
    
    fun rotateDatabaseKey(): ByteArray {
        return try {
            Log.i(TAG, "Starting database key rotation")
            logSecurityEvent("database_key_rotation_started", "Key rotation initiated")
            
            // Store old key temporarily for recovery if needed
            val oldKey = encryptedPrefs.getString(DATABASE_KEY_ALIAS, null)
            
            // Generate new key
            val newKey = generateNewDatabaseKeySecure()
            
            // Clear old key from memory if we had it
            oldKey?.let { 
                val oldKeyBytes = Base64.decode(it, Base64.NO_WRAP)
                clearSensitiveData(oldKeyBytes)
            }
            
            Log.i(TAG, "Database key rotation completed successfully")
            logSecurityEvent("database_key_rotated", "Key rotation completed")
            
            newKey
        } catch (e: Exception) {
            Log.e(TAG, "Database key rotation failed", e)
            logSecurityEvent("database_key_rotation_failed", "Error: ${e.message}")
            throw SecurityException("Key rotation failed", e)
        }
    }
    
    fun clearDatabaseKey() {
        try {
            Log.w(TAG, "Clearing database key - this will make database inaccessible")
            logSecurityEvent("database_key_cleared", "Key manually cleared")
            
            encryptedPrefs.edit()
                .remove(DATABASE_KEY_ALIAS)
                .remove(DATABASE_KEY_BACKUP_ALIAS)
                .remove("key_created_timestamp")
                .remove("key_version")
                .apply()
                
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear database key", e)
            logSecurityEvent("database_key_clear_failed", "Error: ${e.message}")
            throw SecurityException("Key clearing failed", e)
        }
    }
    
    fun isDatabaseKeyPresent(): Boolean {
        return try {
            encryptedPrefs.contains(DATABASE_KEY_ALIAS)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check key presence", e)
            false
        }
    }
    
    fun getDatabaseKeyMetadata(): KeyMetadata? {
        return try {
            if (!isDatabaseKeyPresent()) return null
            
            KeyMetadata(
                createdTimestamp = encryptedPrefs.getLong("key_created_timestamp", 0),
                version = encryptedPrefs.getInt("key_version", 0),
                hasBackup = encryptedPrefs.contains(DATABASE_KEY_BACKUP_ALIAS)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get key metadata", e)
            null
        }
    }
    
    data class KeyMetadata(
        val createdTimestamp: Long,
        val version: Int,
        val hasBackup: Boolean
    )
    
    private fun logSecurityEvent(event: String, details: String) {
        try {
            val timestamp = System.currentTimeMillis()
            Log.i(TAG, "SecurityEvent: $event - $details")
            
            // In a production app, you might want to:
            // 1. Send to security monitoring system
            // 2. Store in secure audit log
            // 3. Alert on critical events
            // 4. Aggregate for security analytics
            
            // For now, we'll use Android Log with structured format
            Log.i("SECURITY_AUDIT", "timestamp=$timestamp,component=DatabaseKeyManager,event=$event,details=$details")
            
        } catch (e: Exception) {
            // Never let logging errors affect security operations
            Log.w(TAG, "Failed to log security event: $event", e)
        }
    }
    
    private fun validateKeyIntegrity(key: ByteArray): Boolean {
        return try {
            // Basic integrity checks
            key.size == KEY_SIZE_BYTES &&
            !key.all { it == 0.toByte() } &&  // Not all zeros
            !key.all { it == key[0] } &&      // Not all same value
            key.any { it != 0.toByte() }      // Has non-zero bytes
        } catch (e: Exception) {
            Log.e(TAG, "Key integrity validation failed", e)
            false
        }
    }
    
    private fun generateHardwareBackedKey(): SecretKey? {
        return try {
            Log.d(TAG, "Generating hardware-backed key for additional security")
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(false)
                .setRandomizedEncryptionRequired(true)
                .setKeySize(256) // Explicit key size
                .build()
            
            keyGenerator.init(keyGenParameterSpec)
            val key = keyGenerator.generateKey()
            logSecurityEvent("hardware_key_generated", "Successfully generated hardware-backed key")
            key
        } catch (e: Exception) {
            Log.w(TAG, "Hardware-backed key generation failed", e)
            logSecurityEvent("hardware_key_generation_failed", "Error: ${e.message}")
            null
        }
    }
    
    private fun getHardwareBackedKey(): SecretKey? {
        return try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            val key = keyStore.getKey(KEYSTORE_ALIAS, null) as? SecretKey
            if (key != null) {
                Log.d(TAG, "Successfully retrieved hardware-backed key")
            }
            key
        } catch (e: Exception) {
            Log.w(TAG, "Failed to retrieve hardware-backed key", e)
            null
        }
    }
}