package me.shadykhalifa.whispertop.managers

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.math.min
import kotlin.math.pow

class ServiceRecoveryManager : KoinComponent {
    
    private val audioServiceManager: AudioServiceManager by inject()
    private val permissionHandler: PermissionHandler by inject()
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    
    private val _recoveryState = MutableStateFlow(RecoveryState.IDLE)
    val recoveryState: StateFlow<RecoveryState> = _recoveryState.asStateFlow()
    
    private var retryCount = 0
    private var isRecovering = false
    
    suspend fun attemptServiceRecovery(): RecoveryResult {
        if (isRecovering) {
            return RecoveryResult.ALREADY_RECOVERING
        }
        
        isRecovering = true
        _recoveryState.value = RecoveryState.ATTEMPTING_RECOVERY
        
        try {
            // Step 1: Check permissions first
            val permissionResult = attemptPermissionRecovery()
            if (permissionResult != RecoveryResult.SUCCESS) {
                isRecovering = false
                _recoveryState.value = RecoveryState.FAILED
                return permissionResult
            }
            
            // Step 2: Attempt service binding with exponential backoff
            val serviceResult = attemptServiceBinding()
            
            isRecovering = false
            
            return when (serviceResult) {
                is ServiceBindingResult.SUCCESS -> {
                    retryCount = 0
                    _recoveryState.value = RecoveryState.RECOVERED
                    RecoveryResult.SUCCESS
                }
                is ServiceBindingResult.FAILED -> {
                    _recoveryState.value = RecoveryState.FAILED
                    RecoveryResult.SERVICE_BINDING_FAILED
                }
                is ServiceBindingResult.MAX_RETRIES_EXCEEDED -> {
                    _recoveryState.value = RecoveryState.FAILED
                    RecoveryResult.MAX_RETRIES_EXCEEDED
                }
            }
        } catch (e: Exception) {
            isRecovering = false
            _recoveryState.value = RecoveryState.FAILED
            return RecoveryResult.UNEXPECTED_ERROR(e)
        }
    }
    
    private suspend fun attemptPermissionRecovery(): RecoveryResult {
        return when (val result = permissionHandler.requestAllPermissions()) {
            is PermissionHandler.PermissionResult.GRANTED -> RecoveryResult.SUCCESS
            is PermissionHandler.PermissionResult.DENIED -> {
                RecoveryResult.PERMISSIONS_DENIED(result.deniedPermissions)
            }
            is PermissionHandler.PermissionResult.SHOW_RATIONALE -> {
                RecoveryResult.PERMISSION_RATIONALE_NEEDED(result.permissions)
            }
        }
    }
    
    private suspend fun attemptServiceBinding(): ServiceBindingResult {
        repeat(MAX_RETRY_ATTEMPTS) { attempt ->
            val delay = calculateBackoffDelay(attempt)
            if (attempt > 0) {
                delay(delay)
            }
            
            when (val result = audioServiceManager.bindService()) {
                is AudioServiceManager.ServiceBindResult.SUCCESS,
                is AudioServiceManager.ServiceBindResult.ALREADY_BOUND -> {
                    return ServiceBindingResult.SUCCESS
                }
                is AudioServiceManager.ServiceBindResult.FAILED,
                is AudioServiceManager.ServiceBindResult.ERROR -> {
                    // Continue to next retry
                }
            }
        }
        
        return ServiceBindingResult.MAX_RETRIES_EXCEEDED
    }
    
    fun schedulePeriodicRecoveryCheck() {
        scope.launch {
            while (true) {
                delay(PERIODIC_CHECK_INTERVAL)
                
                // Check if service is healthy
                if (!isServiceHealthy()) {
                    attemptServiceRecovery()
                }
            }
        }
    }
    
    private fun isServiceHealthy(): Boolean {
        return audioServiceManager.connectionState.value == AudioServiceManager.ServiceConnectionState.CONNECTED &&
                permissionHandler.permissionState.value == PermissionHandler.PermissionState.GRANTED
    }
    
    private fun calculateBackoffDelay(attempt: Int): Long {
        val baseDelay = INITIAL_RETRY_DELAY_MS
        val maxDelay = MAX_RETRY_DELAY_MS
        val exponentialDelay = (baseDelay * 2.0.pow(attempt)).toLong()
        return min(exponentialDelay, maxDelay)
    }
    
    fun forceServiceRestart() {
        scope.launch {
            _recoveryState.value = RecoveryState.RESTARTING_SERVICE
            
            try {
                // Unbind existing service
                audioServiceManager.unbindService()
                
                // Wait a moment for cleanup
                delay(1000)
                
                // Attempt to rebind
                when (val result = audioServiceManager.bindService()) {
                    is AudioServiceManager.ServiceBindResult.SUCCESS -> {
                        _recoveryState.value = RecoveryState.RECOVERED
                    }
                    else -> {
                        _recoveryState.value = RecoveryState.FAILED
                    }
                }
            } catch (e: Exception) {
                _recoveryState.value = RecoveryState.FAILED
            }
        }
    }
    
    fun handleServiceCrash() {
        scope.launch {
            _recoveryState.value = RecoveryState.SERVICE_CRASHED
            
            // Wait before attempting recovery to avoid immediate crash loop
            delay(CRASH_RECOVERY_DELAY_MS)
            
            attemptServiceRecovery()
        }
    }
    
    fun handlePermissionDenied(deniedPermissions: List<String>) {
        scope.launch {
            _recoveryState.value = RecoveryState.PERMISSION_DENIED
            
            // Attempt to guide user to settings if critical permissions denied
            val criticalPermissions = deniedPermissions.filter { 
                it == android.Manifest.permission.RECORD_AUDIO ||
                it == android.Manifest.permission.SYSTEM_ALERT_WINDOW
            }
            
            if (criticalPermissions.isNotEmpty()) {
                // Could trigger a UI flow to guide user to settings
                _recoveryState.value = RecoveryState.REQUIRES_MANUAL_INTERVENTION
            }
        }
    }
    
    fun reset() {
        retryCount = 0
        isRecovering = false
        _recoveryState.value = RecoveryState.IDLE
    }
    
    fun cleanup() {
        scope.cancel()
    }
    
    enum class RecoveryState {
        IDLE,
        ATTEMPTING_RECOVERY,
        RESTARTING_SERVICE,
        SERVICE_CRASHED,
        PERMISSION_DENIED,
        RECOVERED,
        FAILED,
        REQUIRES_MANUAL_INTERVENTION
    }
    
    sealed class RecoveryResult {
        object SUCCESS : RecoveryResult()
        object ALREADY_RECOVERING : RecoveryResult()
        object SERVICE_BINDING_FAILED : RecoveryResult()
        object MAX_RETRIES_EXCEEDED : RecoveryResult()
        data class PERMISSIONS_DENIED(val deniedPermissions: List<String>) : RecoveryResult()
        data class PERMISSION_RATIONALE_NEEDED(val permissions: List<String>) : RecoveryResult()
        data class UNEXPECTED_ERROR(val exception: Exception) : RecoveryResult()
    }
    
    private sealed class ServiceBindingResult {
        object SUCCESS : ServiceBindingResult()
        object FAILED : ServiceBindingResult()
        object MAX_RETRIES_EXCEEDED : ServiceBindingResult()
    }
    
    companion object {
        private const val MAX_RETRY_ATTEMPTS = 5
        private const val INITIAL_RETRY_DELAY_MS = 1000L
        private const val MAX_RETRY_DELAY_MS = 16000L
        private const val PERIODIC_CHECK_INTERVAL = 30000L // 30 seconds
        private const val CRASH_RECOVERY_DELAY_MS = 5000L
    }
}