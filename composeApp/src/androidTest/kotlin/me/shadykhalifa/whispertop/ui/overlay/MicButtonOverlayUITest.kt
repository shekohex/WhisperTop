package me.shadykhalifa.whispertop.ui.overlay

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.WindowManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class MicButtonOverlayUITest {

    private lateinit var context: Context
    private lateinit var windowManager: WindowManager
    private lateinit var micButtonOverlay: MicButtonOverlay
    private val overlayViews = mutableListOf<MicButtonOverlay>()

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        if (!hasOverlayPermission()) {
            requestOverlayPermission()
        }
        
        micButtonOverlay = MicButtonOverlay(context)
        micButtonOverlay.setDraggable(true)
    }

    @After
    fun cleanup() {
        overlayViews.forEach { overlay ->
            try {
                windowManager.removeView(overlay)
            } catch (e: Exception) {
            }
        }
        overlayViews.clear()
    }

    @Test
    fun testMicButtonCreation() {
        assertNotNull(micButtonOverlay)
        assertEquals(MicButtonState.IDLE, micButtonOverlay.getCurrentState())
        assertTrue(micButtonOverlay.isDraggable())
    }

    @Test
    fun testStateTransitions() = runBlocking {
        var stateChangeCount = 0
        val listener = object : MicButtonOverlay.MicButtonListener {
            override fun onStateChanged(newState: MicButtonState) {
                stateChangeCount++
            }
            override fun onMicButtonClicked() {}
            override fun onPositionSnapped(x: Int, y: Int) {}
        }
        
        micButtonOverlay.addMicButtonListener(listener)
        
        micButtonOverlay.setState(MicButtonState.RECORDING)
        delay(100)
        assertEquals(MicButtonState.RECORDING, micButtonOverlay.getCurrentState())
        assertEquals(1, stateChangeCount)
        
        micButtonOverlay.setState(MicButtonState.PROCESSING)
        delay(100)
        assertEquals(MicButtonState.PROCESSING, micButtonOverlay.getCurrentState())
        assertEquals(2, stateChangeCount)
        
        micButtonOverlay.setState(MicButtonState.IDLE)
        delay(100)
        assertEquals(MicButtonState.IDLE, micButtonOverlay.getCurrentState())
        assertEquals(3, stateChangeCount)
    }

    @Test
    fun testOverlayAdditionAndRemoval() {
        if (!hasOverlayPermission()) {
            return
        }
        
        val layoutParams = createTestLayoutParams()
        
        try {
            windowManager.addView(micButtonOverlay, layoutParams)
            overlayViews.add(micButtonOverlay)
            
            delay(500)
            
            windowManager.removeView(micButtonOverlay)
            overlayViews.remove(micButtonOverlay)
            
        } catch (e: Exception) {
            throw AssertionError("Failed to add/remove overlay: ${e.message}", e)
        }
    }

    @Test
    fun testDraggingBehavior() = runBlocking {
        if (!hasOverlayPermission()) {
            return
        }
        
        var positionSnapCount = 0
        val listener = object : MicButtonOverlay.MicButtonListener {
            override fun onStateChanged(newState: MicButtonState) {}
            override fun onMicButtonClicked() {}
            override fun onPositionSnapped(x: Int, y: Int) {
                positionSnapCount++
            }
        }
        
        micButtonOverlay.addMicButtonListener(listener)
        
        val layoutParams = createTestLayoutParams()
        windowManager.addView(micButtonOverlay, layoutParams)
        overlayViews.add(micButtonOverlay)
        
        delay(500)
        
        micButtonOverlay.updatePosition(100, 200)
        delay(100)
        
        val (x, y) = micButtonOverlay.getCurrentPosition()
        assertTrue(x >= 0)
        assertTrue(y >= 0)
    }

    @Test
    fun testStateBasedVisualChanges() = runBlocking {
        micButtonOverlay.setState(MicButtonState.IDLE)
        delay(100)
        
        micButtonOverlay.setState(MicButtonState.RECORDING)
        delay(100)
        
        micButtonOverlay.setState(MicButtonState.PROCESSING)
        delay(100)
        
        micButtonOverlay.setState(MicButtonState.IDLE)
        delay(100)
    }

    @Test
    fun testClickInteraction() = runBlocking {
        var clickCount = 0
        val listener = object : MicButtonOverlay.MicButtonListener {
            override fun onStateChanged(newState: MicButtonState) {}
            override fun onMicButtonClicked() {
                clickCount++
            }
            override fun onPositionSnapped(x: Int, y: Int) {}
        }
        
        micButtonOverlay.addMicButtonListener(listener)
        
        if (hasOverlayPermission()) {
            val layoutParams = createTestLayoutParams()
            windowManager.addView(micButtonOverlay, layoutParams)
            overlayViews.add(micButtonOverlay)
            
            delay(500)
            
            micButtonOverlay.performClick()
            delay(100)
            
            assertEquals(1, clickCount)
        }
    }

    private fun createTestLayoutParams(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            x = 100
            y = 100
        }
    }

    private fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    private suspend fun delay(millis: Long) {
        kotlinx.coroutines.delay(millis)
    }
}