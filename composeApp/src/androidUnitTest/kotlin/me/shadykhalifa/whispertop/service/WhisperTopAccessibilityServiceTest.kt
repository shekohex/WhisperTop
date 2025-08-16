package me.shadykhalifa.whispertop.service

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WhisperTopAccessibilityServiceTest {

    @Test
    fun testServiceInstanceManagement() {
        assertNull(WhisperTopAccessibilityService.getInstance())
        assertFalse(WhisperTopAccessibilityService.isServiceRunning())
    }

    @Test
    fun testInsertTextWithNullService() {
        val result = WhisperTopAccessibilityService.getInstance()?.insertText("test")
        assertNull(result)
    }

    @Test
    fun testFocusEditFieldWithNullService() {
        val result = WhisperTopAccessibilityService.getInstance()?.focusEditField()
        assertNull(result)
    }

    @Test
    fun testClearFocusedFieldWithNullService() {
        val result = WhisperTopAccessibilityService.getInstance()?.clearFocusedField()
        assertNull(result)
    }

    @Test
    fun testServiceClassExists() {
        val serviceClass = WhisperTopAccessibilityService::class.java
        assertTrue(serviceClass.isAssignableFrom(WhisperTopAccessibilityService::class.java))
    }

    @Test
    fun testServiceInstanceSingleton() {
        val instance1 = WhisperTopAccessibilityService.getInstance()
        val instance2 = WhisperTopAccessibilityService.getInstance()
        
        // Both should be null when service is not running
        assertNull(instance1)
        assertNull(instance2)
    }
}