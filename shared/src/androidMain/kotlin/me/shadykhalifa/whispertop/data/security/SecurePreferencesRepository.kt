package me.shadykhalifa.whispertop.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.shadykhalifa.whispertop.domain.repositories.SecurePreferencesRepository
import me.shadykhalifa.whispertop.utils.Result

class SecurePreferencesRepositoryImpl(
    private val context: Context
) : SecurePreferencesRepository {

    private companion object {
        const val SECURE_PREFS_NAME = "whispertop_secure_prefs"
        const val KEY_API_KEY = "openai_api_key"
        const val KEY_API_ENDPOINT = "api_endpoint"
        const val DEFAULT_API_ENDPOINT = "https://api.openai.com/v1/"
        const val API_KEY_PREFIX = "sk-"
        const val API_KEY_MIN_LENGTH = 51
        const val KEY_WPM = "user_wpm"
        const val KEY_WPM_ONBOARDING_COMPLETED = "wpm_onboarding_completed"
        const val DEFAULT_WPM = 36 // Mobile-optimized default based on research
        const val MIN_WPM = 20
        const val MAX_WPM = 60
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            SECURE_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override suspend fun saveApiKey(apiKey: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Get current endpoint to determine validation rules
            val endpoint = encryptedPrefs.getString(KEY_API_ENDPOINT, DEFAULT_API_ENDPOINT) ?: DEFAULT_API_ENDPOINT
            val isOpenAI = isOpenAIEndpoint(endpoint)
            
            println("SecurePreferencesRepository: Saving API key - " +
                    "endpoint='$endpoint', " +
                    "isOpenAI=$isOpenAI, " +
                    "keyLength=${apiKey.length}")
            
            if (!validateApiKey(apiKey, isOpenAI)) {
                val errorMsg = if (isOpenAI) {
                    "Invalid OpenAI API key format. Must start with 'sk-' and be at least 51 characters."
                } else {
                    "Invalid API key format."
                }
                println("SecurePreferencesRepository: API key validation failed - $errorMsg")
                return@withContext Result.Error(IllegalArgumentException(errorMsg))
            }
            
            encryptedPrefs.edit {
                putString(KEY_API_KEY, apiKey.trim())
            }
            
            println("SecurePreferencesRepository: API key saved successfully")
            Result.Success(Unit)
        } catch (e: Exception) {
            println("SecurePreferencesRepository: Failed to save API key - ${e.message}")
            Result.Error(Exception("Failed to save API key", e))
        }
    }
    
    private fun isOpenAIEndpoint(endpoint: String): Boolean {
        val isOpenAI = endpoint.isBlank() ||
                      endpoint.equals("https://api.openai.com/v1", ignoreCase = true) ||
                      endpoint.contains("api.openai.com", ignoreCase = true) || 
                      endpoint.contains("openai.azure.com", ignoreCase = true) ||
                      endpoint.contains("oai.azure.com", ignoreCase = true)
        
        println("SecurePreferencesRepository: Endpoint detection - " +
                "endpoint='$endpoint', isOpenAI=$isOpenAI")
        return isOpenAI
    }

    override suspend fun getApiKey(): Result<String?> = withContext(Dispatchers.IO) {
        try {
            val apiKey = encryptedPrefs.getString(KEY_API_KEY, null)
            Result.Success(apiKey)
        } catch (e: Exception) {
            Result.Error(Exception("Failed to retrieve API key", e))
        }
    }

    override suspend fun clearApiKey(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            encryptedPrefs.edit {
                remove(KEY_API_KEY)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(Exception("Failed to clear API key", e))
        }
    }

    override suspend fun hasApiKey(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val hasKey = encryptedPrefs.contains(KEY_API_KEY) && 
                        !encryptedPrefs.getString(KEY_API_KEY, "").isNullOrBlank()
            Result.Success(hasKey)
        } catch (e: Exception) {
            Result.Error(Exception("Failed to check API key existence", e))
        }
    }

    override suspend fun saveApiEndpoint(endpoint: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val cleanEndpoint = endpoint.trim().let { 
                if (it.endsWith("/")) it else "$it/"
            }
            
            encryptedPrefs.edit {
                putString(KEY_API_ENDPOINT, cleanEndpoint)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(Exception("Failed to save API endpoint", e))
        }
    }

    override suspend fun getApiEndpoint(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val endpoint = encryptedPrefs.getString(KEY_API_ENDPOINT, DEFAULT_API_ENDPOINT)
                ?: DEFAULT_API_ENDPOINT
            Result.Success(endpoint)
        } catch (e: Exception) {
            Result.Error(Exception("Failed to retrieve API endpoint", e))
        }
    }

    override fun validateApiKey(apiKey: String, isOpenAIEndpoint: Boolean): Boolean {
        println("SecurePreferencesRepository: Starting API key validation - " +
                "endpoint=${if (isOpenAIEndpoint) "OpenAI" else "Custom"}, " +
                "keyLength=${apiKey.length}")
        
        // Don't allow leading/trailing whitespace
        if (apiKey != apiKey.trim()) {
            println("SecurePreferencesRepository: Validation failed - key has leading/trailing whitespace")
            return false
        }
        
        // For custom endpoints, API key is optional or can have different format
        if (!isOpenAIEndpoint) {
            // Allow empty API key for custom endpoints
            if (apiKey.isBlank()) {
                println("SecurePreferencesRepository: Custom endpoint validation - empty key allowed")
                return true
            }
            // Basic validation for non-empty keys on custom endpoints
            val isValid = apiKey.isNotBlank() && apiKey.length >= 3
            println("SecurePreferencesRepository: Custom endpoint validation - " +
                    "keyLength=${apiKey.length}, isValid=$isValid")
            return isValid
        }
        
        // Strict OpenAI validation
        val hasPrefix = apiKey.startsWith(API_KEY_PREFIX)
        val hasMinLength = apiKey.length >= API_KEY_MIN_LENGTH
        val matchesPattern = apiKey.matches(Regex("^sk-[A-Za-z0-9\\-_]+$"))
        val isValid = hasPrefix && hasMinLength && matchesPattern
        
        println("SecurePreferencesRepository: OpenAI validation - " +
                "hasPrefix=$hasPrefix, " +
                "hasMinLength=$hasMinLength (${apiKey.length}>=$API_KEY_MIN_LENGTH), " +
                "matchesPattern=$matchesPattern, " +
                "isValid=$isValid")
        
        return isValid
    }
    
    override suspend fun saveWpm(wpm: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            println("SecurePreferencesRepository: Saving WPM - value=$wpm")
            
            if (!validateWpm(wpm)) {
                val errorMsg = "Invalid WPM value. Must be between $MIN_WPM and $MAX_WPM."
                println("SecurePreferencesRepository: WPM validation failed - $errorMsg")
                return@withContext Result.Error(IllegalArgumentException(errorMsg))
            }
            
            encryptedPrefs.edit {
                putInt(KEY_WPM, wpm)
            }
            
            println("SecurePreferencesRepository: WPM saved successfully")
            Result.Success(Unit)
        } catch (e: Exception) {
            println("SecurePreferencesRepository: Failed to save WPM - ${e.message}")
            Result.Error(Exception("Failed to save WPM", e))
        }
    }
    
    override suspend fun getWpm(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val wpm = encryptedPrefs.getInt(KEY_WPM, DEFAULT_WPM)
            println("SecurePreferencesRepository: Retrieved WPM - value=$wpm")
            Result.Success(wpm)
        } catch (e: Exception) {
            println("SecurePreferencesRepository: Failed to retrieve WPM - ${e.message}")
            Result.Error(Exception("Failed to retrieve WPM", e))
        }
    }
    
    override suspend fun saveWpmOnboardingCompleted(completed: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            encryptedPrefs.edit {
                putBoolean(KEY_WPM_ONBOARDING_COMPLETED, completed)
            }
            println("SecurePreferencesRepository: WPM onboarding completion status saved - $completed")
            Result.Success(Unit)
        } catch (e: Exception) {
            println("SecurePreferencesRepository: Failed to save WPM onboarding status - ${e.message}")
            Result.Error(Exception("Failed to save WPM onboarding status", e))
        }
    }
    
    override suspend fun isWpmOnboardingCompleted(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val completed = encryptedPrefs.getBoolean(KEY_WPM_ONBOARDING_COMPLETED, false)
            println("SecurePreferencesRepository: WPM onboarding completion status - $completed")
            Result.Success(completed)
        } catch (e: Exception) {
            println("SecurePreferencesRepository: Failed to check WPM onboarding status - ${e.message}")
            Result.Error(Exception("Failed to check WPM onboarding status", e))
        }
    }
    
    override fun validateWpm(wpm: Int): Boolean {
        val isValid = wpm in MIN_WPM..MAX_WPM
        println("SecurePreferencesRepository: WPM validation - value=$wpm, range=$MIN_WPM-$MAX_WPM, isValid=$isValid")
        return isValid
    }
}