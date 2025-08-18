package me.shadykhalifa.whispertop.domain.usecases

import me.shadykhalifa.whispertop.domain.models.PermissionStatus

/**
 * Use case for managing permission requests and workflows.
 * Encapsulates permission handling logic and maps infrastructure types to domain models.
 */
interface PermissionManagementUseCase {
    /**
     * Requests all required permissions for the application.
     * 
     * @return Result containing PermissionStatus domain model
     */
    suspend operator fun invoke(): Result<PermissionStatus>
}