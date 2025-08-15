package me.shadykhalifa.whispertop.data.security

import android.content.Context
import android.content.SharedPreferences
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
            if (!validateApiKey(apiKey)) {
                return@withContext Result.Error(IllegalArgumentException("Invalid API key format"))
            }
            
            encryptedPrefs.edit()
                .putString(KEY_API_KEY, apiKey.trim())
                .apply()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(Exception("Failed to save API key", e))
        }
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
            encryptedPrefs.edit()
                .remove(KEY_API_KEY)
                .apply()
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
            
            encryptedPrefs.edit()
                .putString(KEY_API_ENDPOINT, cleanEndpoint)
                .apply()
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

    override fun validateApiKey(apiKey: String): Boolean {
        // Don't allow leading/trailing whitespace
        if (apiKey != apiKey.trim()) {
            return false
        }
        
        return apiKey.startsWith(API_KEY_PREFIX) && 
               apiKey.length >= API_KEY_MIN_LENGTH &&
               apiKey.matches(Regex("^sk-[A-Za-z0-9\\-_]+$"))
    }
}