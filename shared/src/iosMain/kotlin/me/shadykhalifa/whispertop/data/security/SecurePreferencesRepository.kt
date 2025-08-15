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
    }

    override suspend fun saveApiKey(apiKey: String): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            if (!validateApiKey(apiKey)) {
                return@withContext Result.Error(IllegalArgumentException("Invalid API key format"))
            }

            val status = saveToKeychain(KEY_API_KEY, apiKey.trim())
            if (status == noErr) {
                Result.Success(Unit)
            } else {
                Result.Error(Exception("Failed to save API key to keychain: $status"))
            }
        } catch (e: Exception) {
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

    override fun validateApiKey(apiKey: String): Boolean {
        // Don't allow leading/trailing whitespace
        if (apiKey != apiKey.trim()) {
            return false
        }
        
        return apiKey.startsWith(API_KEY_PREFIX) && 
               apiKey.length >= API_KEY_MIN_LENGTH &&
               apiKey.matches(Regex("^sk-[A-Za-z0-9\\-_]+$"))
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