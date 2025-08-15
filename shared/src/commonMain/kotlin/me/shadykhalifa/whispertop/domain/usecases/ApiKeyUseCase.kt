package me.shadykhalifa.whispertop.domain.usecases

import me.shadykhalifa.whispertop.domain.repositories.SecurePreferencesRepository
import me.shadykhalifa.whispertop.utils.Result

class SaveApiKeyUseCase(
    private val securePreferencesRepository: SecurePreferencesRepository
) {
    suspend operator fun invoke(apiKey: String): Result<Unit> {
        return securePreferencesRepository.saveApiKey(apiKey)
    }
}

class GetApiKeyUseCase(
    private val securePreferencesRepository: SecurePreferencesRepository
) {
    suspend operator fun invoke(): Result<String?> {
        return securePreferencesRepository.getApiKey()
    }
}

class ValidateApiKeyUseCase(
    private val securePreferencesRepository: SecurePreferencesRepository
) {
    operator fun invoke(apiKey: String): Boolean {
        return securePreferencesRepository.validateApiKey(apiKey)
    }
}

class ClearApiKeyUseCase(
    private val securePreferencesRepository: SecurePreferencesRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return securePreferencesRepository.clearApiKey()
    }
}