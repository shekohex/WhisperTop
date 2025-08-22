package me.shadykhalifa.whispertop.domain.usecases

import me.shadykhalifa.whispertop.domain.models.ServiceConnectionStatus
import me.shadykhalifa.whispertop.domain.repositories.ServiceStateRepository
import me.shadykhalifa.whispertop.domain.utils.ErrorMessageSanitizer
import me.shadykhalifa.whispertop.utils.Result
import me.shadykhalifa.whispertop.utils.safeCall

class ServiceInitializationUseCase(
    private val serviceStateRepository: ServiceStateRepository
) {
    suspend operator fun invoke(): Result<ServiceConnectionStatus> = safeCall {
        val result = serviceStateRepository.bindService()
        
        when (result) {
            is ServiceStateRepository.ServiceBindResult.SUCCESS -> {
                ServiceConnectionStatus.Connected
            }
            is ServiceStateRepository.ServiceBindResult.ALREADY_BOUND -> {
                ServiceConnectionStatus.AlreadyBound
            }
            is ServiceStateRepository.ServiceBindResult.FAILED -> {
                ServiceConnectionStatus.Failed("Service connection failed")
            }
            is ServiceStateRepository.ServiceBindResult.ERROR -> {
                ServiceConnectionStatus.Error(result.exception)
            }
        }
    }
}