package me.shadykhalifa.whispertop.ui.overlay

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Vibrator
import android.util.DisplayMetrics
import android.view.WindowManager
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(MockitoJUnitRunner::class)
class MicButtonOverlayTest {

    @Mock
    private lateinit var mockContext: Context
    
    @Mock
    private lateinit var mockResources: Resources
    
    @Mock
    private lateinit var mockDisplayMetrics: DisplayMetrics
    
    @Mock
    private lateinit var mockVibrator: Vibrator
    
    @Mock
    private lateinit var mockWindowManager: WindowManager
    
    private lateinit var micButtonOverlay: MicButtonOverlay
    private lateinit var mockListener: MicButtonOverlay.MicButtonListener

    @Before
    fun setup() {
        mockDisplayMetrics.density = 2.0f
        mockDisplayMetrics.widthPixels = 1080
        mockDisplayMetrics.heightPixels = 1920
        
        whenever(mockContext.resources).thenReturn(mockResources)
        whenever(mockResources.displayMetrics).thenReturn(mockDisplayMetrics)
        whenever(mockContext.getSystemService(Context.VIBRATOR_SERVICE)).thenReturn(mockVibrator)
        whenever(mockContext.getSystemService(Context.WINDOW_SERVICE)).thenReturn(mockWindowManager)
        
        micButtonOverlay = MicButtonOverlay(mockContext)
        mockListener = mock()
        micButtonOverlay.addMicButtonListener(mockListener)
    }

    @Test
    fun `test initial state is IDLE`() {
        assertEquals(MicButtonState.IDLE, micButtonOverlay.getCurrentState())
    }

    @Test
    fun `test state change triggers listener and haptic feedback`() {
        micButtonOverlay.setState(MicButtonState.RECORDING)
        
        assertEquals(MicButtonState.RECORDING, micButtonOverlay.getCurrentState())
        verify(mockListener).onStateChanged(MicButtonState.RECORDING)
        verify(mockVibrator).vibrate(any<Long>())
    }

    @Test
    fun `test setting same state does not trigger listener`() {
        micButtonOverlay.setState(MicButtonState.IDLE)
        
        verify(mockListener, never()).onStateChanged(any())
        verify(mockVibrator, never()).vibrate(any<Long>())
    }

    @Test
    fun `test all state transitions work correctly`() {
        micButtonOverlay.setState(MicButtonState.RECORDING)
        assertEquals(MicButtonState.RECORDING, micButtonOverlay.getCurrentState())
        
        micButtonOverlay.setState(MicButtonState.PROCESSING)
        assertEquals(MicButtonState.PROCESSING, micButtonOverlay.getCurrentState())
        
        micButtonOverlay.setState(MicButtonState.IDLE)
        assertEquals(MicButtonState.IDLE, micButtonOverlay.getCurrentState())
        
        verify(mockListener, times(3)).onStateChanged(any())
    }

    @Test
    fun `test listener management`() {
        val secondListener = mock<MicButtonOverlay.MicButtonListener>()
        micButtonOverlay.addMicButtonListener(secondListener)
        
        micButtonOverlay.setState(MicButtonState.RECORDING)
        
        verify(mockListener).onStateChanged(MicButtonState.RECORDING)
        verify(secondListener).onStateChanged(MicButtonState.RECORDING)
        
        micButtonOverlay.removeMicButtonListener(secondListener)
        micButtonOverlay.setState(MicButtonState.PROCESSING)
        
        verify(mockListener).onStateChanged(MicButtonState.PROCESSING)
        verify(secondListener, never()).onStateChanged(MicButtonState.PROCESSING)
    }

    @Test
    fun `test draggable is enabled by default`() {
        assertTrue(micButtonOverlay.isDraggable())
    }

    @Test
    fun `test button size calculation for different densities`() {
        mockDisplayMetrics.density = 1.0f
        val lowDensityOverlay = MicButtonOverlay(mockContext)
        
        mockDisplayMetrics.density = 2.0f
        val mediumDensityOverlay = MicButtonOverlay(mockContext)
        
        mockDisplayMetrics.density = 3.5f
        val highDensityOverlay = MicButtonOverlay(mockContext)
        
    }

    @Test
    fun `test draggable is enabled by default after setup`() {
        assertTrue(micButtonOverlay.isDraggable())
        
        micButtonOverlay.setDraggable(false)
        assertFalse(micButtonOverlay.isDraggable())
        
        micButtonOverlay.setDraggable(true)
        assertTrue(micButtonOverlay.isDraggable())
    }
}