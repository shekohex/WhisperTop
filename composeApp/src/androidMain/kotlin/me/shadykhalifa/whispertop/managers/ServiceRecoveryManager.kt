package me.shadykhalifa.whispertop.managers

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.shadykhalifa.whispertop.service.AudioRecordingService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min
import kotlin.math.pow

class ServiceRecoveryManager(private val context: Context) : KoinComponent {
    
    private val audioServiceManager: AudioServiceManager by inject()
    private val audioRecordingServiceManager: AudioRecordingServiceManager by inject()
    private val bindAudioServiceUseCase: me.shadykhalifa.whispertop.domain.usecases.BindAudioServiceUseCase by inject()
    private val permissionHandler: PermissionHandler by inject()
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    
    private val _recoveryState = MutableStateFlow(RecoveryState.IDLE)
    val recoveryState: StateFlow<RecoveryState> = _recoveryState.asStateFlow()
    
    private val _serviceHealth = MutableStateFlow(ServiceHealth())
    val serviceHealth: StateFlow<ServiceHealth> = _serviceHealth.asStateFlow()
    
    private var retryCount = 0
    private val isRecovering = AtomicBoolean(false)
    private var crashCount = 0
    private var lastCrashTime = 0L
    
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("service_recovery_state", Context.MODE_PRIVATE)
    }
    
    data class ServiceHealth(
        val isHealthy: Boolean = false,
        val lastCheckTime: Long = 0L,
        val consecutiveFailures: Int = 0,
        val uptime: Long = 0L,
        val lastRestartTime: Long = 0L
    )
    
    data class ServiceState(
        val isRecording: Boolean = false,
        val recordingStartTime: Long = 0L,
        val currentFilePath: String? = null,
        val pausedDuration: Long = 0L
    )
    
    suspend fun attemptServiceRecovery(): RecoveryResult {
        if (!isRecovering.compareAndSet(false, true)) {
            return RecoveryResult.ALREADY_RECOVERING
        }
        _recoveryState.value = RecoveryState.ATTEMPTING_RECOVERY
        
        try {
            // Step 1: Check permissions first
            val permissionResult = attemptPermissionRecovery()
            if (permissionResult != RecoveryResult.SUCCESS) {
                isRecovering.set(false)
                _recoveryState.value = RecoveryState.FAILED
                return permissionResult
            }
            
            // Step 2: Attempt service binding with exponential backoff
            val serviceResult = attemptServiceBinding()
            
            isRecovering.set(false)
            
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
            isRecovering.set(false)
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
            
            when (val result = bindAudioServiceUseCase()) {
                is me.shadykhalifa.whispertop.utils.Result.Success -> {
                    when (result.data) {
                        is me.shadykhalifa.whispertop.domain.services.IAudioServiceManager.ServiceBindResult.Success,
                        is me.shadykhalifa.whispertop.domain.services.IAudioServiceManager.ServiceBindResult.AlreadyBound -> {
                            return ServiceBindingResult.SUCCESS
                        }
                        is me.shadykhalifa.whispertop.domain.services.IAudioServiceManager.ServiceBindResult.Failed,
                        is me.shadykhalifa.whispertop.domain.services.IAudioServiceManager.ServiceBindResult.Error -> {
                            // Continue to next retry
                        }
                    }
                }
                is me.shadykhalifa.whispertop.utils.Result.Error,
                is me.shadykhalifa.whispertop.utils.Result.Loading -> {
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
                
                val healthCheck = performHealthCheck()
                _serviceHealth.value = healthCheck
                
                // Attempt recovery if service is unhealthy
                if (!healthCheck.isHealthy) {
                    attemptServiceRecovery()
                }
            }
        }
    }
    
    private fun performHealthCheck(): ServiceHealth {
        val currentTime = System.currentTimeMillis()
        val isHealthy = isServiceHealthy()
        val previousHealth = _serviceHealth.value
        
        val consecutiveFailures = if (isHealthy) {
            0
        } else {
            previousHealth.consecutiveFailures + 1
        }
        
        val uptime = if (isHealthy && previousHealth.isHealthy) {
            previousHealth.uptime + (currentTime - previousHealth.lastCheckTime)
        } else if (isHealthy) {
            currentTime - (previousHealth.lastRestartTime.takeIf { it > 0 } ?: currentTime)
        } else {
            0L
        }
        
        return ServiceHealth(
            isHealthy = isHealthy,
            lastCheckTime = currentTime,
            consecutiveFailures = consecutiveFailures,
            uptime = uptime,
            lastRestartTime = if (!isHealthy && previousHealth.isHealthy) currentTime else previousHealth.lastRestartTime
        )
    }
    
    fun getServiceHealthMetrics(): Map<String, Any> {
        val health = _serviceHealth.value
        val recovery = _recoveryState.value
        
        return mapOf(
            "isHealthy" to health.isHealthy,
            "uptime" to health.uptime,
            "consecutiveFailures" to health.consecutiveFailures,
            "lastCheckTime" to health.lastCheckTime,
            "recoveryState" to recovery.name,
            "crashCount" to crashCount,
            "retryCount" to retryCount
        )
    }
    
    private fun isServiceHealthy(): Boolean {
        return audioServiceManager.connectionState.value == me.shadykhalifa.whispertop.domain.services.IAudioServiceManager.ServiceConnectionState.CONNECTED &&
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
                when (val result = bindAudioServiceUseCase()) {
                    is me.shadykhalifa.whispertop.utils.Result.Success -> {
                        when (result.data) {
                            is me.shadykhalifa.whispertop.domain.services.IAudioServiceManager.ServiceBindResult.Success -> {
                                _recoveryState.value = RecoveryState.RECOVERED
                            }
                            else -> {
                                _recoveryState.value = RecoveryState.FAILED
                            }
                        }
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
    
    fun handleServiceCrash(preserveState: Boolean = true) {
        scope.launch {
            _recoveryState.value = RecoveryState.SERVICE_CRASHED
            
            val currentTime = System.currentTimeMillis()
            
            // Check for crash loop
            if (currentTime - lastCrashTime < CRASH_LOOP_DETECTION_WINDOW) {
                crashCount++
                if (crashCount >= MAX_CRASHES_IN_WINDOW) {
                    _recoveryState.value = RecoveryState.CRASH_LOOP_DETECTED
                    delay(CRASH_LOOP_RECOVERY_DELAY_MS)
                    crashCount = 0 // Reset after cooling off
                }
            } else {
                crashCount = 1 // Reset crash count
            }
            
            lastCrashTime = currentTime
            
            // Preserve service state if requested
            val savedState = if (preserveState) {
                saveServiceState()
            } else null
            
            // Wait before attempting recovery to avoid immediate crash loop
            val delay = calculateCrashRecoveryDelay()
            delay(delay)
            
            val result = attemptServiceRecovery()
            
            // Restore state if recovery was successful
            if (result == RecoveryResult.SUCCESS && savedState != null) {
                restoreServiceState(savedState)
            }
        }
    }
    
    private fun saveServiceState(): ServiceState? {
        return try {
            // Get current service state from AudioServiceManager
            val currentState = audioRecordingServiceManager.getCurrentRecordingState()
            val isRecording = currentState == AudioRecordingService.RecordingState.RECORDING || 
                             currentState == AudioRecordingService.RecordingState.PAUSED
            val recordingStartTime = if (isRecording) System.currentTimeMillis() else 0L
            val currentFilePath: String? = null // Not currently exposed by AudioServiceManager
            
            val state = ServiceState(
                isRecording = isRecording,
                recordingStartTime = recordingStartTime,
                currentFilePath = currentFilePath,
                pausedDuration = 0L
            )
            
            // Persist to SharedPreferences
            prefs.edit()
                .putBoolean("was_recording", state.isRecording)
                .putLong("recording_start_time", state.recordingStartTime)
                .putString("current_file_path", state.currentFilePath)
                .putLong("paused_duration", state.pausedDuration)
                .putLong("crash_time", System.currentTimeMillis())
                .apply()
            
            state
        } catch (e: Exception) {
            null
        }
    }
    
    private fun restoreServiceState(state: ServiceState) {
        scope.launch {
            try {
                // Only restore if crash was recent (within 5 minutes)
                val crashTime = prefs.getLong("crash_time", 0L)
                val timeSinceCrash = System.currentTimeMillis() - crashTime
                
                if (timeSinceCrash < 5 * 60 * 1000L && state.isRecording) {
                    // Attempt to restart the service - automatic recording restoration is risky
                    // Instead, we just ensure the service is bound and available
                    when (val result = bindAudioServiceUseCase()) {
                        is me.shadykhalifa.whispertop.utils.Result.Success -> {
                            when (result.data) {
                                is me.shadykhalifa.whispertop.domain.services.IAudioServiceManager.ServiceBindResult.Success,
                                is me.shadykhalifa.whispertop.domain.services.IAudioServiceManager.ServiceBindResult.AlreadyBound -> {
                                    // Service is available, user can manually restart recording if needed
                                }
                                else -> {
                                    // Service binding failed, will be handled by health checks
                                }
                            }
                        }
                        else -> {
                            // Service binding failed, will be handled by health checks
                        }
                    }
                }
                
                // Clear saved state
                prefs.edit().clear().apply()
                
            } catch (e: Exception) {
                // Handle restoration error
            }
        }
    }
    
    private fun calculateCrashRecoveryDelay(): Long {
        return when (crashCount) {
            1 -> CRASH_RECOVERY_DELAY_MS
            2 -> CRASH_RECOVERY_DELAY_MS * 2
            3 -> CRASH_RECOVERY_DELAY_MS * 4
            else -> CRASH_LOOP_RECOVERY_DELAY_MS
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
        isRecovering.set(false)
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
        CRASH_LOOP_DETECTED,
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
        private const val CRASH_LOOP_DETECTION_WINDOW = 60000L // 1 minute
        private const val MAX_CRASHES_IN_WINDOW = 3
        private const val CRASH_LOOP_RECOVERY_DELAY_MS = 300000L // 5 minutes
    }
}