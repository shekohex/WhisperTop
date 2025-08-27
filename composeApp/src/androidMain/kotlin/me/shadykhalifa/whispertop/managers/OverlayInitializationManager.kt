package me.shadykhalifa.whispertop.managers

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.shadykhalifa.whispertop.presentation.models.RecordingStatus
import me.shadykhalifa.whispertop.domain.repositories.ServiceStateRepository
import me.shadykhalifa.whispertop.presentation.AudioRecordingViewModel
import me.shadykhalifa.whispertop.service.AudioRecordingService
import me.shadykhalifa.whispertop.ui.overlay.MicButtonOverlay
import me.shadykhalifa.whispertop.ui.overlay.MicButtonState
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class OverlayInitializationManager : KoinComponent, DefaultLifecycleObserver {
    
    companion object {
        private const val TAG = "OverlayInitManager"
        private const val MIC_BUTTON_OVERLAY_ID = "mic_button"
        private const val SERVICE_CONNECTION_TIMEOUT_MS = 3000L
        private const val SERVICE_CONNECTION_RETRY_DELAY_MS = 500L
        private const val MAX_RETRY_ATTEMPTS = 3
        
        // Overlay position persistence
        private const val OVERLAY_PREFS = "overlay_preferences"
        private const val KEY_OVERLAY_X = "overlay_x"
        private const val KEY_OVERLAY_Y = "overlay_y"
        private const val KEY_OVERLAY_ENABLED = "overlay_enabled"
        private const val KEY_OVERLAY_HIDDEN = "overlay_hidden"
    }
    
    private val context: Context by inject()
    private val overlayManager: OverlayManager by inject()
    private val permissionHandler: PermissionHandler by inject()
    private val audioRecordingViewModel: AudioRecordingViewModel by inject()
    
    // Lazy injection since these may not be needed immediately
    private val overlayNotificationManager: me.shadykhalifa.whispertop.ui.notifications.OverlayNotificationManager by inject()
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private val _initializationState = MutableStateFlow(InitializationState.NOT_STARTED)
    val initializationState: StateFlow<InitializationState> = _initializationState.asStateFlow()
    
    private var micButtonOverlay: MicButtonOverlay? = null
    private var retryAttempts = 0
    private var isAppInForeground = true
    private var lastKnownConfiguration: Configuration? = null
    
    private val overlayPrefs: SharedPreferences by lazy {
        context.getSharedPreferences(OVERLAY_PREFS, Context.MODE_PRIVATE)
    }
    
    init {
        // Register for app lifecycle events
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        lastKnownConfiguration = Configuration(context.resources.configuration)
    }
    
    enum class InitializationState {
        NOT_STARTED,
        CHECKING_PERMISSIONS,
        STARTING_SERVICE,
        CREATING_OVERLAY,
        COMPLETED,
        FAILED,
        PERMISSION_DENIED
    }
    
    /**
     * Initialize the overlay system with the floating mic button
     */
    suspend fun initializeOverlay(): Boolean {
        Log.d(TAG, "Starting overlay initialization")
        _initializationState.value = InitializationState.CHECKING_PERMISSIONS
        
        // Check overlay permission
        if (!permissionHandler.hasOverlayPermission()) {
            Log.w(TAG, "Overlay permission not granted")
            _initializationState.value = InitializationState.PERMISSION_DENIED
            return false
        }
        
        return try {
            // Start overlay service
            _initializationState.value = InitializationState.STARTING_SERVICE
            if (!startOverlayService()) {
                Log.e(TAG, "Failed to start overlay service")
                _initializationState.value = InitializationState.FAILED
                return false
            }
            
            // Create and show mic button overlay
            _initializationState.value = InitializationState.CREATING_OVERLAY
            if (!createAndShowMicButton()) {
                Log.e(TAG, "Failed to create and show mic button")
                _initializationState.value = InitializationState.FAILED
                return false
            }
            
            _initializationState.value = InitializationState.COMPLETED
            Log.i(TAG, "Overlay initialization completed successfully")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during overlay initialization", e)
            _initializationState.value = InitializationState.FAILED
            false
        }
    }
    
    /**
     * Start the overlay service and wait for connection
     */
    private suspend fun startOverlayService(): Boolean {
        val startResult = overlayManager.startService()
        if (!startResult) {
            return false
        }
        
        // Wait for service to connect with timeout and retry logic
        var waitTime = 0L
        val checkInterval = 100L
        
        while (waitTime < SERVICE_CONNECTION_TIMEOUT_MS) {
            if (overlayManager.isServiceConnected()) {
                Log.d(TAG, "Overlay service connected after ${waitTime}ms")
                return true
            }
            delay(checkInterval)
            waitTime += checkInterval
        }
        
        Log.w(TAG, "Overlay service connection timeout after ${waitTime}ms")
        
        // Retry if we haven't exceeded max attempts
        if (retryAttempts < MAX_RETRY_ATTEMPTS) {
            retryAttempts++
            Log.d(TAG, "Retrying overlay service connection (attempt $retryAttempts)")
            delay(SERVICE_CONNECTION_RETRY_DELAY_MS)
            return startOverlayService()
        }
        
        return false
    }
    
    /**
     * Create the mic button overlay and add it to the overlay service
     */
    private suspend fun createAndShowMicButton(): Boolean {
        try {
            // Create layout parameters for the overlay
            val layoutParams = createMicButtonLayoutParams()
            val (initialX, initialY) = loadOverlayPosition()
            layoutParams.x = initialX
            layoutParams.y = initialY
            
            // Create mic button overlay
            micButtonOverlay = MicButtonOverlay(context).apply {
                // Set layout parameters for the overlay view
                setLayoutParams(layoutParams)
                
                // Connect mic button click listener to recording workflow
                addMicButtonListener(createMicButtonListener())
            }
            
            // Set up state synchronization between recording state and overlay button
            setupStateSync()
            
            // Add overlay to service
            val addResult = overlayManager.addOverlay(
                overlayId = MIC_BUTTON_OVERLAY_ID,
                overlayView = micButtonOverlay!!,
                layoutParams = layoutParams
            )
            
            if (addResult) {
                Log.i(TAG, "Mic button overlay added successfully")
                
                // Check if overlay should be hidden initially
                if (isOverlayHidden()) {
                    Log.d(TAG, "Overlay is set to hidden, hiding it")
                    overlayManager.hideOverlay(MIC_BUTTON_OVERLAY_ID)
                }
                
                return true
            } else {
                Log.e(TAG, "Failed to add mic button overlay to service")
                return false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error creating mic button overlay", e)
            return false
        }
    }
    
    /**
     * Create window layout parameters for the mic button overlay
     */
    private fun createMicButtonLayoutParams(): WindowManager.LayoutParams {
        val buttonSizeDp = 56
        val buttonSizePx = (buttonSizeDp * context.resources.displayMetrics.density).toInt()
        
        return WindowManager.LayoutParams().apply {
            width = buttonSizePx
            height = buttonSizePx
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
            gravity = Gravity.TOP or Gravity.START
            format = android.graphics.PixelFormat.TRANSLUCENT
        }
    }
    
    /**
     * Remove the mic button overlay
     */
    fun removeOverlay() {
        scope.launch {
            try {
                if (micButtonOverlay != null) {
                    overlayManager.removeOverlay(MIC_BUTTON_OVERLAY_ID)
                    micButtonOverlay = null
                    Log.d(TAG, "Mic button overlay removed")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error removing mic button overlay", e)
            }
        }
    }
    
    /**
     * Stop the overlay service and clean up resources
     */
    fun cleanup() {
        scope.launch {
            try {
                removeOverlay()
                overlayManager.stopService()
                _initializationState.value = InitializationState.NOT_STARTED
                retryAttempts = 0
                Log.d(TAG, "Overlay cleanup completed")
            } catch (e: Exception) {
                Log.e(TAG, "Error during overlay cleanup", e)
            }
        }
    }
    
    /**
     * Get the current mic button overlay instance
     */
    fun getMicButtonOverlay(): MicButtonOverlay? = micButtonOverlay
    
    /**
     * Check if overlay is currently shown
     */
    fun isOverlayShown(): Boolean {
        return micButtonOverlay != null && overlayManager.isOverlayVisible(MIC_BUTTON_OVERLAY_ID)
    }
    

    
    /**
     * Handle mic button click - toggle recording state
     */
    private suspend fun handleMicButtonClick() {
        val currentState = audioRecordingViewModel.uiState.value.status
        val uiState = audioRecordingViewModel.uiState.value
        
        Log.d(TAG, "handleMicButtonClick: currentState=$currentState")
        
        when (currentState) {
            RecordingStatus.Idle -> {
                Log.d(TAG, "Starting recording from mic button")
                audioRecordingViewModel.startRecording()
            }
            RecordingStatus.Recording -> {
                Log.d(TAG, "Stopping recording from mic button")
                audioRecordingViewModel.stopRecording()
            }
            RecordingStatus.Processing -> {
                Log.d(TAG, "Recording is processing, ignoring click")
            }
            RecordingStatus.InsertingText -> {
                Log.d(TAG, "Inserting text, ignoring click")
            }
            RecordingStatus.Success -> {
                Log.d(TAG, "Recording successful, ignoring click")
            }
            RecordingStatus.Error -> {
                Log.d(TAG, "Recording error detected, attempting retry")
                audioRecordingViewModel.retryFromError()
            }
        }
    }
    
    /**
     * Handle long press on mic button to hide overlay
     */
    private suspend fun handleMicButtonLongPress() {
        Log.d(TAG, "Handling mic button long press - hiding overlay")
        
        // Hide the overlay
        val hideResult = overlayManager.hideOverlay(MIC_BUTTON_OVERLAY_ID)
        if (hideResult) {
            // Set hidden state and show notification
            setOverlayHidden(true)
            Log.i(TAG, "Overlay hidden successfully with notification shown")
        } else {
            Log.e(TAG, "Failed to hide overlay")
        }
    }
    
    /**
     * Set up state synchronization between AudioRecordingViewModel and MicButtonOverlay
     */
    private fun setupStateSync() {
        scope.launch {
            audioRecordingViewModel.uiState.collect { uiState ->
                val buttonState = when (uiState.status) {
                    RecordingStatus.Idle -> MicButtonState.IDLE
                    RecordingStatus.Recording -> MicButtonState.RECORDING
                    RecordingStatus.Processing -> MicButtonState.PROCESSING
                    RecordingStatus.InsertingText -> MicButtonState.PROCESSING
                    RecordingStatus.Success -> MicButtonState.IDLE
                    RecordingStatus.Error -> MicButtonState.IDLE
                }
                
                micButtonOverlay?.setState(buttonState)
                Log.d(TAG, "Updated mic button state to: $buttonState (from recording status: ${uiState.status})")
                
                // Log error details for debugging when in error state
                if (uiState.status == RecordingStatus.Error && uiState.errorMessage != null) {
                    Log.w(TAG, "Recording error details: ${uiState.errorMessage}")
                }
            }
        }
    }
    
    /**
     * Handle app going to foreground
     */
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        isAppInForeground = true
        Log.d(TAG, "App in foreground")
        
        scope.launch {
            // Check if overlay permission is still granted
            if (permissionHandler.hasOverlayPermission()) {
                // Re-initialize overlay if it's not shown but should be
                if (!isOverlayShown() && isOverlayEnabled()) {
                    Log.d(TAG, "Re-initializing overlay on app foreground")
                    initializeOverlay()
                }
            } else {
                // Permission was revoked, clean up overlay
                Log.w(TAG, "Overlay permission revoked, cleaning up")
                removeOverlay()
                setOverlayEnabled(false)
            }
        }
    }
    
    /**
     * Handle app going to background
     */
    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        isAppInForeground = false
        Log.d(TAG, "App in background")
        
        // Keep overlay visible in background for system-wide functionality
        // This is the desired behavior for a floating mic button
    }
    
    /**
     * Handle configuration changes (rotation, screen size, etc.)
     */
    fun onConfigurationChanged(newConfig: Configuration) {
        scope.launch {
            try {
                val oldConfig = lastKnownConfiguration
                lastKnownConfiguration = Configuration(newConfig)
                
                if (micButtonOverlay != null && isOverlayShown()) {
                    Log.d(TAG, "Configuration changed, updating overlay position")
                    
                    // Handle screen rotation/size change
                    if (oldConfig?.orientation != newConfig.orientation ||
                        oldConfig?.screenWidthDp != newConfig.screenWidthDp ||
                        oldConfig?.screenHeightDp != newConfig.screenHeightDp) {
                        
                        updateOverlayForConfigurationChange(newConfig)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling configuration change", e)
            }
        }
    }
    
    /**
     * Update overlay position and layout for configuration changes
     */
    private suspend fun updateOverlayForConfigurationChange(config: Configuration) {
        try {
            micButtonOverlay?.let { overlay ->
                val displayMetrics = context.resources.displayMetrics
                val (currentX, currentY) = overlay.getCurrentPosition()
                
                // Ensure overlay is still within screen bounds
                val maxX = displayMetrics.widthPixels - overlay.width
                val maxY = displayMetrics.heightPixels - overlay.height
                
                val adjustedX = currentX.coerceIn(0, maxX)
                val adjustedY = currentY.coerceIn(0, maxY)
                
                if (adjustedX != currentX || adjustedY != currentY) {
                    Log.d(TAG, "Adjusting overlay position from ($currentX, $currentY) to ($adjustedX, $adjustedY)")
                    overlay.updatePosition(adjustedX, adjustedY)
                    saveOverlayPosition(adjustedX, adjustedY)
                }
                
                // Update layout parameters if needed
                val newLayoutParams = createMicButtonLayoutParams()
                overlayManager.updateOverlayParams(MIC_BUTTON_OVERLAY_ID, newLayoutParams)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating overlay for configuration change", e)
        }
    }
    
    /**
     * Save overlay position to preferences
     */
    private fun saveOverlayPosition(x: Int, y: Int) {
        overlayPrefs.edit()
            .putInt(KEY_OVERLAY_X, x)
            .putInt(KEY_OVERLAY_Y, y)
            .apply()
        Log.d(TAG, "Saved overlay position: ($x, $y)")
    }
    
    /**
     * Load overlay position from preferences
     */
    private fun loadOverlayPosition(): Pair<Int, Int> {
        val displayMetrics = context.resources.displayMetrics
        val defaultX = displayMetrics.widthPixels - (80 * displayMetrics.density).toInt()
        val defaultY = displayMetrics.heightPixels / 2
        
        val x = overlayPrefs.getInt(KEY_OVERLAY_X, defaultX)
        val y = overlayPrefs.getInt(KEY_OVERLAY_Y, defaultY)
        
        Log.d(TAG, "Loaded overlay position: ($x, $y)")
        return Pair(x, y)
    }
    
    /**
     * Check if overlay is enabled in preferences
     */
    private fun isOverlayEnabled(): Boolean {
        return overlayPrefs.getBoolean(KEY_OVERLAY_ENABLED, true)
    }
    
    /**
     * Set overlay enabled state in preferences
     */
    private fun setOverlayEnabled(enabled: Boolean) {
        overlayPrefs.edit()
            .putBoolean(KEY_OVERLAY_ENABLED, enabled)
            .apply()
        Log.d(TAG, "Set overlay enabled: $enabled")
    }
    
    /**
     * Check if overlay is currently hidden
     */
    private fun isOverlayHidden(): Boolean {
        return overlayPrefs.getBoolean(KEY_OVERLAY_HIDDEN, false)
    }
    
    /**
     * Set overlay hidden state and show/hide notification accordingly
     */
    private fun setOverlayHidden(hidden: Boolean) {
        overlayPrefs.edit()
            .putBoolean(KEY_OVERLAY_HIDDEN, hidden)
            .apply()
        Log.d(TAG, "Set overlay hidden: $hidden")
        
        if (hidden) {
            overlayNotificationManager.showOverlayHiddenNotification()
        } else {
            overlayNotificationManager.dismissNotification()
        }
    }
    
    /**
     * Clear overlay hidden state (used when app is reopened)
     */
    fun clearOverlayHiddenState() {
        if (isOverlayHidden()) {
            Log.d(TAG, "Clearing overlay hidden state on app reopen")
            setOverlayHidden(false)
            overlayManager.showOverlay(MIC_BUTTON_OVERLAY_ID)
        }
    }
    
    /**
     * Show overlay from hidden state (called from notification action)
     */
    suspend fun showOverlayFromHidden() {
        Log.d(TAG, "Showing overlay from hidden state")
        setOverlayHidden(false)
        overlayManager.showOverlay(MIC_BUTTON_OVERLAY_ID)
    }
    
    /**
     * Enhanced mic button listener that includes position saving
     */
    private fun createMicButtonListener(): MicButtonOverlay.MicButtonListener {
        return object : MicButtonOverlay.MicButtonListener {
            override fun onStateChanged(newState: MicButtonState) {
                Log.d(TAG, "Mic button state changed to: $newState")
            }
            
            override fun onMicButtonClicked() {
                scope.launch {
                    try {
                        Log.d(TAG, "Mic button clicked")
                        handleMicButtonClick()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error handling mic button click", e)
                    }
                }
            }
            
            override fun onMicButtonLongPressed() {
                scope.launch {
                    try {
                        Log.d(TAG, "Mic button long pressed - hiding overlay")
                        handleMicButtonLongPress()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error handling mic button long press", e)
                    }
                }
            }
            
            override fun onPositionSnapped(x: Int, y: Int) {
                Log.d(TAG, "Mic button position snapped to: ($x, $y)")
                saveOverlayPosition(x, y)
            }
            
            override fun onAudioLevelChanged(level: Float) {
                // Could be used for visual feedback during recording
            }
        }
    }
    
    /**
     * Check if overlay should be automatically re-initialized
     */
    suspend fun checkAndReinitializeIfNeeded() {
        if (!isOverlayShown() && 
            isOverlayEnabled() && 
            permissionHandler.hasOverlayPermission() &&
            _initializationState.value != InitializationState.STARTING_SERVICE &&
            _initializationState.value != InitializationState.CREATING_OVERLAY) {
            
            Log.d(TAG, "Auto-reinitializing overlay")
            initializeOverlay()
        }
    }
    
    /**
     * Disable overlay (user choice)
     */
    fun disableOverlay() {
        scope.launch {
            setOverlayEnabled(false)
            removeOverlay()
            Log.i(TAG, "Overlay disabled by user")
        }
    }
    
    /**
     * Enable overlay (user choice)
     */
    suspend fun enableOverlay(): Boolean {
        return if (permissionHandler.hasOverlayPermission()) {
            setOverlayEnabled(true)
            initializeOverlay()
        } else {
            Log.w(TAG, "Cannot enable overlay - permission not granted")
            false
        }
    }
    
    /**
     * Retry initialization if it failed
     */
    suspend fun retryInitialization(): Boolean {
        if (_initializationState.value == InitializationState.FAILED) {
            retryAttempts = 0
            return initializeOverlay()
        }
        return false
    }
}