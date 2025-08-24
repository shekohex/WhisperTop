package me.shadykhalifa.whispertop.service

import android.Manifest
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import me.shadykhalifa.whispertop.R
import me.shadykhalifa.whispertop.managers.PermissionHandler
import org.koin.android.ext.android.inject
import org.koin.core.error.NoDefinitionFoundException

@RequiresApi(Build.VERSION_CODES.N)
open class WhisperTopTileService : TileService() {
    
    private val permissionHandler: PermissionHandler by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private var audioService: AudioRecordingService? = null
    private var isServiceBound = false
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? AudioRecordingService.AudioRecordingBinder
            audioService = binder?.getService()
            
            audioService?.let { audioService ->
                audioService.addStateListener(serviceStateListener)
                isServiceBound = true
                updateTileState(audioService.getCurrentState())
            }
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            audioService?.removeStateListener(serviceStateListener)
            audioService = null
            isServiceBound = false
            updateTileState(AudioRecordingService.RecordingState.IDLE)
        }
    }
    
    private val serviceStateListener = object : AudioRecordingService.RecordingStateListener {
        override fun onStateChanged(state: AudioRecordingService.RecordingState) {
            updateTileState(state)
        }
        
        override fun onRecordingComplete(audioFile: me.shadykhalifa.whispertop.domain.models.AudioFile?) {
            // Handle recording completion if needed
        }
        
        override fun onRecordingError(error: String) {
            // Handle recording error if needed
            updateTileState(AudioRecordingService.RecordingState.IDLE)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        // Ensure Koin context is available by checking if we can inject
        try {
            // Test if Koin injection works
            permissionHandler.hasAudioPermission()
        } catch (e: Exception) {
            // If Koin injection fails, the service will fall back to direct permission checks
        }
    }
    
    override fun onStartListening() {
        super.onStartListening()
        try {
            // Initialize tile state immediately
            updateTileState(AudioRecordingService.RecordingState.IDLE)
            bindToAudioService()
        } catch (e: Exception) {
            // If anything fails, at least set the tile to available with basic permissions
            qsTile?.let { tile ->
                tile.state = if (hasAudioPermissionDirect()) Tile.STATE_INACTIVE else Tile.STATE_UNAVAILABLE
                tile.icon = Icon.createWithResource(this, R.drawable.ic_mic)
                tile.label = getString(R.string.tile_label_idle)
                tile.contentDescription = getString(R.string.tile_description_idle)
                tile.updateTile()
            }
        }
    }
    
    override fun onStopListening() {
        super.onStopListening()
        unbindFromAudioService()
    }
    
    override fun onClick() {
        super.onClick()
        
        if (!hasAllPermissions()) {
            showPermissionDialog()
            return
        }
        
        val currentState = audioService?.getCurrentState() ?: AudioRecordingService.RecordingState.IDLE
        
        when (currentState) {
            AudioRecordingService.RecordingState.IDLE -> {
                startRecording()
            }
            AudioRecordingService.RecordingState.RECORDING,
            AudioRecordingService.RecordingState.PAUSED -> {
                stopRecording()
            }
            AudioRecordingService.RecordingState.PROCESSING -> {
                // Do nothing while processing
            }
        }
    }
    
    private fun bindToAudioService() {
        if (!isServiceBound) {
            try {
                val intent = Intent(this, AudioRecordingService::class.java)
                bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            } catch (e: Exception) {
                updateTileState(AudioRecordingService.RecordingState.IDLE)
            }
        }
    }
    
    private fun unbindFromAudioService() {
        if (isServiceBound) {
            try {
                audioService?.removeStateListener(serviceStateListener)
                unbindService(serviceConnection)
            } catch (e: Exception) {
                // Ignore unbind errors
            } finally {
                audioService = null
                isServiceBound = false
            }
        }
    }
    
    private fun hasRequiredPermissions(): Boolean {
        // For the tile to be available, we only need audio permission
        return try {
            permissionHandler.hasAudioPermission()
        } catch (e: Exception) {
            // Fallback to direct permission check if Koin injection fails
            ContextCompat.checkSelfPermission(
                this, 
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    private fun hasAllPermissions(): Boolean {
        // Check if all permissions are granted for full functionality
        return try {
            permissionHandler.hasAudioPermission() &&
            permissionHandler.hasOverlayPermission() &&
            hasAccessibilityPermission()
        } catch (e: Exception) {
            // Fallback to direct permission checks if Koin injection fails
            hasAudioPermissionDirect() &&
            hasOverlayPermissionDirect() &&
            hasAccessibilityPermission()
        }
    }
    
    private fun hasAudioPermissionDirect(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, 
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun hasOverlayPermissionDirect(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }
    
    internal open fun hasAccessibilityPermission(): Boolean {
        val accessibilityManager = getSystemService(Context.ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return enabledServices?.contains(packageName) == true
    }
    
    private fun showPermissionDialog() {
        val intent = Intent(this, me.shadykhalifa.whispertop.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("request_permissions", true)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
    
    private fun startRecording() {
        scope.launch {
            try {
                if (!isServiceBound) {
                    bindToAudioService()
                    return@launch
                }
                
                audioService?.startRecording()
            } catch (e: Exception) {
                updateTileState(AudioRecordingService.RecordingState.IDLE)
            }
        }
    }
    
    private fun stopRecording() {
        scope.launch {
            try {
                audioService?.stopRecording()
            } catch (e: Exception) {
                updateTileState(AudioRecordingService.RecordingState.IDLE)
            }
        }
    }
    
    private fun updateTileState(recordingState: AudioRecordingService.RecordingState) {
        qsTile?.let { tile ->
            when (recordingState) {
                AudioRecordingService.RecordingState.IDLE -> {
                    tile.state = if (hasRequiredPermissions()) Tile.STATE_INACTIVE else Tile.STATE_UNAVAILABLE
                    tile.icon = Icon.createWithResource(this, R.drawable.ic_mic)
                    tile.label = getString(R.string.tile_label_idle)
                    tile.contentDescription = getString(R.string.tile_description_idle)
                }
                AudioRecordingService.RecordingState.RECORDING -> {
                    tile.state = Tile.STATE_ACTIVE
                    tile.icon = Icon.createWithResource(this, R.drawable.ic_mic_recording)
                    tile.label = getString(R.string.tile_label_recording)
                    tile.contentDescription = getString(R.string.tile_description_recording)
                }
                AudioRecordingService.RecordingState.PAUSED -> {
                    tile.state = Tile.STATE_ACTIVE
                    tile.icon = Icon.createWithResource(this, R.drawable.ic_mic_paused)
                    tile.label = getString(R.string.tile_label_paused)
                    tile.contentDescription = getString(R.string.tile_description_paused)
                }
                AudioRecordingService.RecordingState.PROCESSING -> {
                    tile.state = Tile.STATE_UNAVAILABLE
                    tile.icon = Icon.createWithResource(this, R.drawable.ic_mic_processing)
                    tile.label = getString(R.string.tile_label_processing)
                    tile.contentDescription = getString(R.string.tile_description_processing)
                }
            }
            tile.updateTile()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        unbindFromAudioService()
    }
}