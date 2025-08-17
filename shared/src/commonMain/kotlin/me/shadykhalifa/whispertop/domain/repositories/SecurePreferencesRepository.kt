package me.shadykhalifa.whispertop.domain.repositories

import me.shadykhalifa.whispertop.utils.Result

interface SecurePreferencesRepository {
    suspend fun saveApiKey(apiKey: String): Result<Unit>
    suspend fun getApiKey(): Result<String?>
    suspend fun clearApiKey(): Result<Unit>
    suspend fun hasApiKey(): Result<Boolean>
    suspend fun saveApiEndpoint(endpoint: String): Result<Unit>
    suspend fun getApiEndpoint(): Result<String>
    fun validateApiKey(apiKey: String, isOpenAIEndpoint: Boolean = true): Boolean
}