package me.shadykhalifa.whispertop.service

import me.shadykhalifa.whispertop.service.OverlayService.OverlayState
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OverlayServiceStateTest {
    
    @Test
    fun testOverlayStateEnumValues() {
        val states = OverlayState.values()
        assertEquals(3, states.size)
        assertTrue(states.contains(OverlayState.IDLE))
        assertTrue(states.contains(OverlayState.ACTIVE))
        assertTrue(states.contains(OverlayState.ERROR))
    }
    
    @Test
    fun testOverlayStateStringRepresentation() {
        assertEquals("IDLE", OverlayState.IDLE.name)
        assertEquals("ACTIVE", OverlayState.ACTIVE.name)
        assertEquals("ERROR", OverlayState.ERROR.name)
    }
    
    @Test
    fun testOverlayStateOrdinals() {
        assertEquals(0, OverlayState.IDLE.ordinal)
        assertEquals(1, OverlayState.ACTIVE.ordinal)
        assertEquals(2, OverlayState.ERROR.ordinal)
    }
}