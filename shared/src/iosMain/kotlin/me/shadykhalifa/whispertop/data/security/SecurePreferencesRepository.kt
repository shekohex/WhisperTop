package me.shadykhalifa.whispertop.data.security

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.shadykhalifa.whispertop.domain.repositories.SecurePreferencesRepository
import me.shadykhalifa.whispertop.utils.Result
import platform.Foundation.NSUserDefaults
import platform.Security.*
import platform.darwin.OSStatus
import platform.darwin.noErr

class SecurePreferencesRepositoryImpl : SecurePreferencesRepository {

    private companion object {
        const val SERVICE_NAME = "me.shadykhalifa.whispertop"
        const val KEY_API_KEY = "openai_api_key"
        const val KEY_API_ENDPOINT = "api_endpoint"
        const val DEFAULT_API_ENDPOINT = "https://api.openai.com/v1/"
        const val API_KEY_PREFIX = "sk-"
        const val API_KEY_MIN_LENGTH = 51
        const val KEY_WPM = "user_wpm"
        const val KEY_WPM_ONBOARDING_COMPLETED = "wmp_onboarding_completed"
        const val DEFAULT_WPM = 36 // Mobile-optimized default based on research
        const val MIN_WPM = 20
        const val MAX_WPM = 60
    }

    override suspend fun saveApiKey(apiKey: String): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            // Get current endpoint to determine validation rules
            val endpoint = NSUserDefaults.standardUserDefaults.stringForKey(KEY_API_ENDPOINT) ?: DEFAULT_API_ENDPOINT
            val isOpenAI = isOpenAIEndpoint(endpoint)
            
            println("SecurePreferencesRepository (iOS): Saving API key - " +
                    "endpoint='$endpoint', " +
                    "isOpenAI=$isOpenAI, " +
                    "keyLength=${apiKey.length}")
            
            if (!validateApiKey(apiKey, isOpenAI)) {
                val errorMsg = if (isOpenAI) {
                    "Invalid OpenAI API key format. Must start with 'sk-' and be at least 51 characters."
                } else {
                    "Invalid API key format."
                }
                println("SecurePreferencesRepository (iOS): API key validation failed - $errorMsg")
                return@withContext Result.Error(IllegalArgumentException(errorMsg))
            }

            val status = saveToKeychain(KEY_API_KEY, apiKey.trim())
            if (status == noErr) {
                println("SecurePreferencesRepository (iOS): API key saved successfully to keychain")
                Result.Success(Unit)
            } else {
                println("SecurePreferencesRepository (iOS): Failed to save API key to keychain - status: $status")
                Result.Error(Exception("Failed to save API key to keychain: $status"))
            }
        } catch (e: Exception) {
            println("SecurePreferencesRepository (iOS): Failed to save API key - ${e.message}")
            Result.Error(Exception("Failed to save API key", e))
        }
    }

    override suspend fun getApiKey(): Result<String?> = withContext(Dispatchers.Default) {
        try {
            val apiKey = getFromKeychain(KEY_API_KEY)
            Result.Success(apiKey)
        } catch (e: Exception) {
            Result.Error(Exception("Failed to retrieve API key", e))
        }
    }

    override suspend fun clearApiKey(): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            val status = deleteFromKeychain(KEY_API_KEY)
            if (status == noErr || status == errSecItemNotFound) {
                Result.Success(Unit)
            } else {
                Result.Error(Exception("Failed to clear API key: $status"))
            }
        } catch (e: Exception) {
            Result.Error(Exception("Failed to clear API key", e))
        }
    }

    override suspend fun hasApiKey(): Result<Boolean> = withContext(Dispatchers.Default) {
        try {
            val apiKey = getFromKeychain(KEY_API_KEY)
            Result.Success(!apiKey.isNullOrBlank())
        } catch (e: Exception) {
            Result.Success(false)
        }
    }

    override suspend fun saveApiEndpoint(endpoint: String): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            val cleanEndpoint = endpoint.trim().let { 
                if (it.endsWith("/")) it else "$it/"
            }
            
            NSUserDefaults.standardUserDefaults.setObject(cleanEndpoint, KEY_API_ENDPOINT)
            NSUserDefaults.standardUserDefaults.synchronize()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(Exception("Failed to save API endpoint", e))
        }
    }

    override suspend fun getApiEndpoint(): Result<String> = withContext(Dispatchers.Default) {
        try {
            val endpoint = NSUserDefaults.standardUserDefaults.stringForKey(KEY_API_ENDPOINT)
                ?: DEFAULT_API_ENDPOINT
            Result.Success(endpoint)
        } catch (e: Exception) {
            Result.Error(Exception("Failed to retrieve API endpoint", e))
        }
    }

    override fun validateApiKey(apiKey: String, isOpenAIEndpoint: Boolean): Boolean {
        println("SecurePreferencesRepository (iOS): Starting API key validation - " +
                "endpoint=${if (isOpenAIEndpoint) "OpenAI" else "Custom"}, " +
                "keyLength=${apiKey.length}")
        
        // Don't allow leading/trailing whitespace
        if (apiKey != apiKey.trim()) {
            println("SecurePreferencesRepository (iOS): Validation failed - key has leading/trailing whitespace")
            return false
        }
        
        // For custom endpoints, API key is optional or can have different format
        if (!isOpenAIEndpoint) {
            // Allow empty API key for custom endpoints
            if (apiKey.isBlank()) {
                println("SecurePreferencesRepository (iOS): Custom endpoint validation - empty key allowed")
                return true
            }
            // Basic validation for non-empty keys on custom endpoints
            val isValid = apiKey.isNotBlank() && apiKey.length >= 3
            println("SecurePreferencesRepository (iOS): Custom endpoint validation - " +
                    "keyLength=${apiKey.length}, isValid=$isValid")
            return isValid
        }
        
        // Strict OpenAI validation
        val hasPrefix = apiKey.startsWith(API_KEY_PREFIX)
        val hasMinLength = apiKey.length >= API_KEY_MIN_LENGTH
        val matchesPattern = apiKey.matches(Regex("^sk-[A-Za-z0-9\\-_]+$"))
        val isValid = hasPrefix && hasMinLength && matchesPattern
        
        println("SecurePreferencesRepository (iOS): OpenAI validation - " +
                "hasPrefix=$hasPrefix, " +
                "hasMinLength=$hasMinLength (${apiKey.length}>=$API_KEY_MIN_LENGTH), " +
                "matchesPattern=$matchesPattern, " +
                "isValid=$isValid")
        
        return isValid
    }
    
    private fun isOpenAIEndpoint(endpoint: String): Boolean {
        val isOpenAI = endpoint.isBlank() ||
                      endpoint.equals("https://api.openai.com/v1", ignoreCase = true) ||
                      endpoint.contains("api.openai.com", ignoreCase = true) || 
                      endpoint.contains("openai.azure.com", ignoreCase = true) ||
                      endpoint.contains("oai.azure.com", ignoreCase = true)
        
        println("SecurePreferencesRepository (iOS): Endpoint detection - " +
                "endpoint='$endpoint', isOpenAI=$isOpenAI")
        return isOpenAI
    }
    
    override suspend fun saveWpm(wpm: Int): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            println("SecurePreferencesRepository (iOS): Saving WPM - value=$wpm")
            
            if (!validateWpm(wpm)) {
                val errorMsg = "Invalid WPM value. Must be between $MIN_WPM and $MAX_WPM."
                println("SecurePreferencesRepository (iOS): WPM validation failed - $errorMsg")
                return@withContext Result.Error(IllegalArgumentException(errorMsg))
            }
            
            NSUserDefaults.standardUserDefaults.setInteger(wpm.toLong(), KEY_WPM)
            NSUserDefaults.standardUserDefaults.synchronize()
            
            println("SecurePreferencesRepository (iOS): WPM saved successfully")
            Result.Success(Unit)
        } catch (e: Exception) {
            println("SecurePreferencesRepository (iOS): Failed to save WPM - ${e.message}")
            Result.Error(Exception("Failed to save WPM", e))
        }
    }
    
    override suspend fun getWpm(): Result<Int> = withContext(Dispatchers.Default) {
        try {
            val wpm = NSUserDefaults.standardUserDefaults.integerForKey(KEY_WPM).let {
                if (it == 0L) DEFAULT_WPM else it.toInt()
            }
            println("SecurePreferencesRepository (iOS): Retrieved WPM - value=$wpm")
            Result.Success(wpm)
        } catch (e: Exception) {
            println("SecurePreferencesRepository (iOS): Failed to retrieve WPM - ${e.message}")
            Result.Error(Exception("Failed to retrieve WPM", e))
        }
    }
    
    override suspend fun saveWpmOnboardingCompleted(completed: Boolean): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            NSUserDefaults.standardUserDefaults.setBool(completed, KEY_WPM_ONBOARDING_COMPLETED)
            NSUserDefaults.standardUserDefaults.synchronize()
            println("SecurePreferencesRepository (iOS): WPM onboarding completion status saved - $completed")
            Result.Success(Unit)
        } catch (e: Exception) {
            println("SecurePreferencesRepository (iOS): Failed to save WPM onboarding status - ${e.message}")
            Result.Error(Exception("Failed to save WPM onboarding status", e))
        }
    }
    
    override suspend fun isWpmOnboardingCompleted(): Result<Boolean> = withContext(Dispatchers.Default) {
        try {
            val completed = NSUserDefaults.standardUserDefaults.boolForKey(KEY_WPM_ONBOARDING_COMPLETED)
            println("SecurePreferencesRepository (iOS): WPM onboarding completion status - $completed")
            Result.Success(completed)
        } catch (e: Exception) {
            println("SecurePreferencesRepository (iOS): Failed to check WPM onboarding status - ${e.message}")
            Result.Error(Exception("Failed to check WPM onboarding status", e))
        }
    }
    
    override fun validateWpm(wpm: Int): Boolean {
        val isValid = wpm in MIN_WPM..MAX_WPM
        println("SecurePreferencesRepository (iOS): WPM validation - value=$wpm, range=$MIN_WPM-$MAX_WPM, isValid=$isValid")
        return isValid
    }

    private fun saveToKeychain(key: String, value: String): OSStatus {
        val query = mapOf(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to SERVICE_NAME,
            kSecAttrAccount to key,
            kSecValueData to value.encodeToByteArray().toNSData()
        )

        deleteFromKeychain(key)

        return SecItemAdd(query as CFDictionaryRef, null)
    }

    private fun getFromKeychain(key: String): String? {
        val query = mapOf(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to SERVICE_NAME,
            kSecAttrAccount to key,
            kSecReturnData to kCFBooleanTrue,
            kSecMatchLimit to kSecMatchLimitOne
        )

        return try {
            val result = SecItemCopyMatching(query as CFDictionaryRef, null)
            if (result == noErr) {
                null
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun deleteFromKeychain(key: String): OSStatus {
        val query = mapOf(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to SERVICE_NAME,
            kSecAttrAccount to key
        )

        return SecItemDelete(query as CFDictionaryRef)
    }
}

private fun ByteArray.toNSData(): platform.Foundation.NSData {
    return platform.Foundation.NSData.create(
        bytes = this.refTo(0),
        length = this.size.toULong()
    )
}