package me.shadykhalifa.whispertop.ui.overlay

import androidx.compose.ui.graphics.Color
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class MicButtonStateTest {

    @Test
    fun `test IDLE state properties`() {
        val state = MicButtonState.IDLE
        assertEquals(Color(0xFF9E9E9E), state.color)
        assertEquals("Microphone idle", state.description)
    }

    @Test
    fun `test RECORDING state properties`() {
        val state = MicButtonState.RECORDING
        assertEquals(Color(0xFFE53E3E), state.color)
        assertEquals("Recording audio", state.description)
    }

    @Test
    fun `test PROCESSING state properties`() {
        val state = MicButtonState.PROCESSING
        assertEquals(Color(0xFF3182CE), state.color)
        assertEquals("Processing audio", state.description)
    }

    @Test
    fun `test all states have different colors`() {
        val idleColor = MicButtonState.IDLE.color
        val recordingColor = MicButtonState.RECORDING.color
        val processingColor = MicButtonState.PROCESSING.color
        
        assertNotEquals(idleColor, recordingColor)
        assertNotEquals(idleColor, processingColor)
        assertNotEquals(recordingColor, processingColor)
    }

    @Test
    fun `test all states have descriptions`() {
        MicButtonState.values().forEach { state ->
            assert(state.description.isNotBlank()) { "State ${state.name} should have a description" }
        }
    }

    @Test
    fun `test state enumeration order`() {
        val states = MicButtonState.values()
        assertEquals(3, states.size)
        assertEquals(MicButtonState.IDLE, states[0])
        assertEquals(MicButtonState.RECORDING, states[1])
        assertEquals(MicButtonState.PROCESSING, states[2])
    }
}