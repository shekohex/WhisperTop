package me.shadykhalifa.whispertop.domain.usecases

import me.shadykhalifa.whispertop.domain.models.ServiceConnectionStatus

/**
 * Use case for initializing and binding to the audio recording service.
 * Encapsulates service binding logic and maps infrastructure types to domain models.
 */
interface ServiceInitializationUseCase {
    /**
     * Attempts to initialize and bind to the audio recording service.
     * 
     * @return Result containing ServiceConnectionStatus domain model
     */
    suspend operator fun invoke(): Result<ServiceConnectionStatus>
}