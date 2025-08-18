package me.shadykhalifa.whispertop.domain.usecases

import me.shadykhalifa.whispertop.domain.models.ServiceReadinessState

/**
 * Use case for coordinating service binding with permission checks.
 * Provides unified service readiness state by combining both service and permission status.
 */
interface ServiceBindingUseCase {
    /**
     * Coordinates service initialization and permission requests to determine overall readiness.
     * 
     * @return Result containing ServiceReadinessState domain model
     */
    suspend operator fun invoke(): Result<ServiceReadinessState>
}