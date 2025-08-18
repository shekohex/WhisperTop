package me.shadykhalifa.whispertop.data.usecases

import android.util.Log
import me.shadykhalifa.whispertop.domain.models.ServiceConnectionStatus
import me.shadykhalifa.whispertop.domain.usecases.ServiceInitializationUseCase
import me.shadykhalifa.whispertop.managers.AudioServiceManager

/**
 * Android implementation of ServiceInitializationUseCase.
 * Maps AudioServiceManager.ServiceBindResult to domain ServiceConnectionStatus.
 */
class ServiceInitializationUseCaseImpl(
    private val audioServiceManager: AudioServiceManager
) : ServiceInitializationUseCase {
    
    private companion object {
        const val TAG = "ServiceInitializationUseCase"
    }
    
    override suspend fun invoke(): Result<ServiceConnectionStatus> {
        return try {
            Log.d(TAG, "Starting service initialization")
            
            val result = audioServiceManager.bindService()
            val connectionStatus = mapToConnectionStatus(result)
            
            Log.d(TAG, "Service initialization completed: $connectionStatus")
            Result.success(connectionStatus)
        } catch (exception: Exception) {
            Log.e(TAG, "Service initialization failed with exception", exception)
            val errorStatus = ServiceConnectionStatus.Error(
                message = "Service initialization failed: ${exception.message}",
                cause = exception
            )
            Result.success(errorStatus)
        }
    }
    
    /**
     * Maps infrastructure ServiceBindResult to domain ServiceConnectionStatus.
     */
    private fun mapToConnectionStatus(result: AudioServiceManager.ServiceBindResult): ServiceConnectionStatus {
        return when (result) {
            is AudioServiceManager.ServiceBindResult.SUCCESS -> {
                Log.d(TAG, "Service bound successfully")
                ServiceConnectionStatus.Connected
            }
            is AudioServiceManager.ServiceBindResult.ALREADY_BOUND -> {
                Log.d(TAG, "Service already bound")
                ServiceConnectionStatus.AlreadyConnected
            }
            is AudioServiceManager.ServiceBindResult.FAILED -> {
                Log.w(TAG, "Service binding failed")
                ServiceConnectionStatus.Failed
            }
            is AudioServiceManager.ServiceBindResult.ERROR -> {
                Log.e(TAG, "Service binding error", result.exception)
                ServiceConnectionStatus.Error(
                    message = "Service binding error: ${result.exception.message}",
                    cause = result.exception
                )
            }
        }
    }
}