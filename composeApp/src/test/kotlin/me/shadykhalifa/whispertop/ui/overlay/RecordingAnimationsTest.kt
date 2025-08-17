package me.shadykhalifa.whispertop.ui.overlay

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import me.shadykhalifa.whispertop.ui.feedback.HapticFeedbackManager
import me.shadykhalifa.whispertop.ui.overlay.components.AudioLevelVisualization
import me.shadykhalifa.whispertop.ui.overlay.components.PulsingRecordingRing
import me.shadykhalifa.whispertop.ui.overlay.components.ProcessingSpinner
import me.shadykhalifa.whispertop.ui.overlay.components.SuccessCheckmark
import me.shadykhalifa.whispertop.ui.overlay.components.ErrorPulse
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.Assume
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [26], // Use SDK 26 to match robolectric.properties and minSdk
    application = me.shadykhalifa.whispertop.TestApplication::class,
    instrumentedPackages = ["androidx.loader.content"]
)
class RecordingAnimationsTest {

    @get:Rule(order = 1)
    val addActivityToRobolectricRule = object : TestWatcher() {
        override fun starting(description: Description?) {
            super.starting(description)
            val appContext: Application = ApplicationProvider.getApplicationContext()
            Shadows.shadowOf(appContext.packageManager).addActivityIfNotPresent(
                ComponentName(
                    appContext.packageName,
                    ComponentActivity::class.java.name,
                )
            )
        }
    }

    @get:Rule(order = 2)
    val composeTestRule = createComposeRule()

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockVibrator: Vibrator

    private lateinit var hapticFeedbackManager: HapticFeedbackManager

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        whenever(mockContext.getSystemService(Context.VIBRATOR_SERVICE)).thenReturn(mockVibrator)
        whenever(mockVibrator.hasVibrator()).thenReturn(true)
        hapticFeedbackManager = HapticFeedbackManager(mockContext)
    }

    @Test
    fun `test pulsing ring animation properties`() {
        composeTestRule.setContent {
            PulsingRecordingRing(
                size = 56.dp,
                pulseColor = Color.White,
                animationDurationMs = 1000,
                scaleRange = 1f to 1.3f,
                alphaRange = 0.8f to 0.2f
            )
        }
        
        // Verify the component renders without crashing
        composeTestRule.waitForIdle()
        
        // Test animation timing by advancing clock
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.mainClock.advanceTimeBy(500) // Half animation duration
        composeTestRule.waitForIdle()
        
        // Animation should be running at this point
        composeTestRule.mainClock.autoAdvance = true
    }

    @Test
    fun `test processing spinner animation`() {
        composeTestRule.setContent {
            ProcessingSpinner(
                size = 48.dp,
                color = Color.Blue,
                animationDurationMs = 1200
            )
        }
        
        composeTestRule.waitForIdle()
    }

    @Test
    fun `test audio level visualization updates`() {
        var audioLevel by mutableFloatStateOf(0f)
        
        composeTestRule.setContent {
            AudioLevelVisualization(
                audioLevel = audioLevel,
                size = 32.dp,
                barColor = Color.White,
                barCount = 8
            )
        }
        
        composeTestRule.waitForIdle()
        
        // Test audio level updates
        audioLevel = 0.5f
        composeTestRule.waitForIdle()
        
        audioLevel = 1.0f
        composeTestRule.waitForIdle()
        
        // Verify component handles level changes without crashing
        audioLevel = 0f
        composeTestRule.waitForIdle()
    }

    @Test
    fun `test success checkmark animation`() {
        composeTestRule.setContent {
            SuccessCheckmark(
                size = 24.dp,
                color = Color.Green
            )
        }
        
        composeTestRule.waitForIdle()
    }

    @Test
    fun `test error pulse animation`() {
        composeTestRule.setContent {
            ErrorPulse(
                size = 56.dp,
                errorColor = Color.Red
            )
        }
        
        composeTestRule.waitForIdle()
    }

    @Test
    fun `test haptic feedback manager with vibrator support`() = runTest {
        // Test supported haptic feedback
        assertTrue(hapticFeedbackManager.isHapticFeedbackSupported())
        
        // Test that feedback calls work with vibrator
        hapticFeedbackManager.performFeedback(HapticFeedbackManager.FeedbackPattern.StartRecording)
        hapticFeedbackManager.performFeedback(HapticFeedbackManager.FeedbackPattern.StopRecording)
        hapticFeedbackManager.performFeedback(HapticFeedbackManager.FeedbackPattern.Success)
        hapticFeedbackManager.performFeedback(HapticFeedbackManager.FeedbackPattern.Error)
        
        // Verify vibrator was used
        verify(mockVibrator, org.mockito.kotlin.atLeastOnce()).hasVibrator()
    }
    
    @Test
    fun `test haptic feedback manager cleanup`() = runTest {
        assertTrue(hapticFeedbackManager.isHapticFeedbackSupported())
        
        // Cleanup should disable further vibrations
        hapticFeedbackManager.cleanup()
        
        assertFalse(hapticFeedbackManager.isHapticFeedbackSupported())
        
        // Further calls should be ignored
        hapticFeedbackManager.performFeedback(HapticFeedbackManager.FeedbackPattern.StartRecording)
        
        // Verify cancel was called during cleanup
        verify(mockVibrator).cancel()
    }
    
    @Test 
    fun `test haptic feedback manager with no vibrator`() = runTest {
        whenever(mockContext.getSystemService(Context.VIBRATOR_SERVICE)).thenReturn(null)
        val noVibratorManager = HapticFeedbackManager(mockContext)
        
        // Test that manager handles null vibrator gracefully
        assertFalse(noVibratorManager.isHapticFeedbackSupported())
        
        // Test that feedback calls don't crash
        noVibratorManager.performFeedback(HapticFeedbackManager.FeedbackPattern.StartRecording)
        noVibratorManager.performFeedback(HapticFeedbackManager.FeedbackPattern.StopRecording)
        noVibratorManager.performFeedback(HapticFeedbackManager.FeedbackPattern.Success)
        noVibratorManager.performFeedback(HapticFeedbackManager.FeedbackPattern.Error)
    }

    @Test
    fun `test mic button state color transitions`() {
        val idleColor = MicButtonState.IDLE.color
        val recordingColor = MicButtonState.RECORDING.color
        val processingColor = MicButtonState.PROCESSING.color
        
        // Verify colors are different for each state
        assertFalse(idleColor == recordingColor)
        assertFalse(recordingColor == processingColor)
        assertFalse(idleColor == processingColor)
        
        // Verify specific color values
        assertEquals(Color(0xFF9E9E9E), idleColor)
        assertEquals(Color(0xFFE53E3E), recordingColor)
        assertEquals(Color(0xFF3182CE), processingColor)
    }

    @Test
    fun `test mic button state descriptions`() {
        assertEquals("Microphone idle", MicButtonState.IDLE.description)
        assertEquals("Recording audio", MicButtonState.RECORDING.description)
        assertEquals("Processing audio", MicButtonState.PROCESSING.description)
    }
}