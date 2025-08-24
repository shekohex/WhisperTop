package me.shadykhalifa.whispertop.data.services

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import me.shadykhalifa.whispertop.service.WhisperTopAccessibilityService
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.*

@RunWith(AndroidJUnit4::class)
class TextInsertionServiceImplTest {
    
    private val textInsertionService = TextInsertionServiceImpl()
    
    @Test
    fun `isServiceAvailable should return false when accessibility service not running`() {
        // When accessibility service is not running
        val isAvailable = textInsertionService.isServiceAvailable()
        
        // Then
        assertFalse(isAvailable)
    }
    
    @Test
    fun `insertText should return false when accessibility service not available`() = runTest {
        // Given
        val testText = "Hello world"
        
        // When accessibility service is not running
        val result = textInsertionService.insertText(testText)
        
        // Then
        assertFalse(result)
    }
    
    // Note: Testing with actual accessibility service would require:
    // 1. Enabling accessibility service in test environment
    // 2. Mocking the accessibility service instance
    // 3. Integration tests with actual UI components
    // These tests focus on the basic functionality when service is not available
}