package me.shadykhalifa.whispertop.ui.overlay

import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class OverlayViewTest {
    
    private lateinit var context: Context
    private lateinit var testOverlayView: TestOverlayView
    private var expandStateChanges = mutableListOf<Boolean>()
    private var positionChanges = mutableListOf<Pair<Int, Int>>()
    private var clickEvents = mutableListOf<String>()
    private var dismissEvents = mutableListOf<String>()
    
    private val testListener = object : OverlayView.OverlayViewListener {
        override fun onExpandStateChanged(isExpanded: Boolean) {
            expandStateChanges.add(isExpanded)
        }
        
        override fun onPositionChanged(x: Int, y: Int) {
            positionChanges.add(Pair(x, y))
        }
        
        override fun onOverlayClicked() {
            clickEvents.add("clicked")
        }
        
        override fun onOverlayDismissed() {
            dismissEvents.add("dismissed")
        }
    }
    
    // Test implementation of OverlayView
    private class TestOverlayView(context: Context) : OverlayView(context) {
        public override fun createCollapsedView(): View {
            return TextView(context).apply {
                text = "Collapsed"
                layoutParams = LayoutParams(100, 50)
            }
        }
        
        public override fun createExpandedView(): View {
            return TextView(context).apply {
                text = "Expanded"
                layoutParams = LayoutParams(200, 100)
            }
        }
    }
    
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        testOverlayView = TestOverlayView(context)
        
        expandStateChanges.clear()
        positionChanges.clear()
        clickEvents.clear()
        dismissEvents.clear()
    }
    
    @Test
    fun testInitialState() {
        assertFalse(testOverlayView.isExpanded())
        assertFalse(testOverlayView.isDraggable())
        
        val position = testOverlayView.getCurrentPosition()
        assertEquals(Pair(0, 0), position)
    }
    
    @Test
    fun testCreateLayoutParams() {
        val params = OverlayView.createLayoutParams(
            width = 150,
            height = 100,
            x = 50,
            y = 75
        )
        
        assertNotNull(params)
        assertEquals(150, params.width)
        assertEquals(100, params.height)
        assertEquals(50, params.x)
        assertEquals(75, params.y)
        assertEquals(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, params.type)
    }
    
    @Test
    fun testExpandCollapse() {
        testOverlayView.addOverlayListener(testListener)
        
        // Test expand
        testOverlayView.expand()
        assertTrue(testOverlayView.isExpanded())
        assertTrue(expandStateChanges.contains(true))
        
        // Test collapse
        testOverlayView.collapse()
        assertFalse(testOverlayView.isExpanded())
        assertTrue(expandStateChanges.contains(false))
        
        testOverlayView.removeOverlayListener(testListener)
    }
    
    @Test
    fun testToggleExpanded() {
        testOverlayView.addOverlayListener(testListener)
        
        // Initially collapsed, toggle should expand
        testOverlayView.toggleExpanded()
        assertTrue(testOverlayView.isExpanded())
        
        // Now expanded, toggle should collapse
        testOverlayView.toggleExpanded()
        assertFalse(testOverlayView.isExpanded())
        
        testOverlayView.removeOverlayListener(testListener)
    }
    
    @Test
    fun testDraggableProperty() {
        assertFalse(testOverlayView.isDraggable())
        
        testOverlayView.setDraggable(true)
        assertTrue(testOverlayView.isDraggable())
        
        testOverlayView.setDraggable(false)
        assertFalse(testOverlayView.isDraggable())
    }
    
    @Test
    fun testUpdatePosition() {
        val params = OverlayView.createLayoutParams()
        testOverlayView.setLayoutParams(params)
        testOverlayView.addOverlayListener(testListener)
        
        testOverlayView.updatePosition(100, 200)
        
        val position = testOverlayView.getCurrentPosition()
        assertEquals(Pair(100, 200), position)
        assertTrue(positionChanges.contains(Pair(100, 200)))
        
        testOverlayView.removeOverlayListener(testListener)
    }
    
    @Test
    fun testClickWhenNotDraggable() {
        testOverlayView.addOverlayListener(testListener)
        testOverlayView.setDraggable(false)
        
        // Simulate touch events
        val downEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 50f, 50f, 0)
        val upEvent = MotionEvent.obtain(0, 100, MotionEvent.ACTION_UP, 50f, 50f, 0)
        
        testOverlayView.dispatchTouchEvent(downEvent)
        testOverlayView.dispatchTouchEvent(upEvent)
        
        assertTrue(clickEvents.contains("clicked"))
        
        downEvent.recycle()
        upEvent.recycle()
        testOverlayView.removeOverlayListener(testListener)
    }
    
    @Test
    fun testDragMovement() {
        val params = OverlayView.createLayoutParams()
        testOverlayView.setLayoutParams(params)
        testOverlayView.addOverlayListener(testListener)
        testOverlayView.setDraggable(true)
        
        // Simulate drag movement
        val downEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 50f, 50f, 0)
        val moveEvent = MotionEvent.obtain(0, 50, MotionEvent.ACTION_MOVE, 100f, 100f, 0)
        val upEvent = MotionEvent.obtain(0, 100, MotionEvent.ACTION_UP, 100f, 100f, 0)
        
        testOverlayView.dispatchTouchEvent(downEvent)
        testOverlayView.dispatchTouchEvent(moveEvent)
        testOverlayView.dispatchTouchEvent(upEvent)
        
        // Should not trigger click when dragging
        assertFalse(clickEvents.contains("clicked"))
        
        downEvent.recycle()
        moveEvent.recycle()
        upEvent.recycle()
        testOverlayView.removeOverlayListener(testListener)
    }
    
    @Test
    fun testClickWhenDraggableButNoMovement() {
        testOverlayView.addOverlayListener(testListener)
        testOverlayView.setDraggable(true)
        
        // Simulate touch without movement (should still trigger click)
        val downEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 50f, 50f, 0)
        val upEvent = MotionEvent.obtain(0, 50, MotionEvent.ACTION_UP, 50f, 50f, 0)
        
        testOverlayView.dispatchTouchEvent(downEvent)
        testOverlayView.dispatchTouchEvent(upEvent)
        
        assertTrue(clickEvents.contains("clicked"))
        
        downEvent.recycle()
        upEvent.recycle()
        testOverlayView.removeOverlayListener(testListener)
    }
    
    @Test
    fun testDismiss() {
        testOverlayView.addOverlayListener(testListener)
        
        testOverlayView.dismiss()
        
        assertTrue(dismissEvents.contains("dismissed"))
        
        testOverlayView.removeOverlayListener(testListener)
    }
    
    @Test
    fun testListenerManagement() {
        // Test adding and removing listeners
        testOverlayView.addOverlayListener(testListener)
        testOverlayView.expand()
        
        assertTrue(expandStateChanges.isNotEmpty())
        
        expandStateChanges.clear()
        testOverlayView.removeOverlayListener(testListener)
        testOverlayView.collapse()
        
        // Should not receive events after removing listener
        assertTrue(expandStateChanges.isEmpty())
    }
    
    @Test
    fun testViewCreation() {
        // Test that views are created correctly
        val collapsedView = testOverlayView.createCollapsedView()
        assertNotNull(collapsedView)
        assertTrue(collapsedView is TextView)
        assertEquals("Collapsed", (collapsedView as TextView).text)
        
        val expandedView = testOverlayView.createExpandedView()
        assertNotNull(expandedView)
        assertTrue(expandedView is TextView)
        assertEquals("Expanded", (expandedView as TextView).text)
    }
}