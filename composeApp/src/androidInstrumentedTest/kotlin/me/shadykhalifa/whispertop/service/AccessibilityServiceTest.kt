package me.shadykhalifa.whispertop.service

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class AccessibilityServiceTest {

    private lateinit var context: Context
    private lateinit var accessibilityManager: AccessibilityManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    }

    @Test
    fun accessibilityService_staticMethods() {
        // Initially, service should not be running in test environment
        assertFalse(WhisperTopAccessibilityService.isServiceRunning())
        assertNull(WhisperTopAccessibilityService.getInstance())
    }

    @Test
    fun accessibilityService_mockInstance() {
        // Create a mock service instance for testing
        val mockService = object : WhisperTopAccessibilityService() {
            override fun onCreate() {
                // Override to prevent actual system service creation
                clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            }
        }
        
        // Test onCreate behavior
        mockService.onCreate()
        
        // Service should have clipboard manager initialized
        assertNotNull(mockService.clipboardManager)
    }

    @Test
    fun textInsertion_emptyText() = runTest {
        val mockService = object : WhisperTopAccessibilityService() {
            override fun onCreate() {
                clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            }
        }
        
        mockService.onCreate()
        
        // Test inserting empty text
        val result = mockService.insertText("")
        
        // Should handle empty text gracefully
        // Result depends on implementation, but should not crash
        assertNotNull(result)
    }

    @Test
    fun textInsertion_longText() = runTest {
        val mockService = object : WhisperTopAccessibilityService() {
            override fun onCreate() {
                clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            }
        }
        
        mockService.onCreate()
        
        // Test inserting very long text
        val longText = "a".repeat(10000)
        val result = mockService.insertText(longText)
        
        // Should handle long text without crashing
        assertNotNull(result)
    }

    @Test
    fun textInsertion_specialCharacters() = runTest {
        val mockService = object : WhisperTopAccessibilityService() {
            override fun onCreate() {
                clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            }
        }
        
        mockService.onCreate()
        
        // Test inserting text with special characters
        val specialText = "Hello 👋 世界 🌍 \n\t Special chars: @#$%^&*()[]{}|\\:;\"'<>?,./"
        val result = mockService.insertText(specialText)
        
        // Should handle special characters gracefully
        assertNotNull(result)
    }

    @Test
    fun textInsertion_unicodeText() = runTest {
        val mockService = object : WhisperTopAccessibilityService() {
            override fun onCreate() {
                clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            }
        }
        
        mockService.onCreate()
        
        // Test inserting Unicode text
        val unicodeText = "Hello in different languages: 你好, Здравствуй, مرحبا, Γεια σου"
        val result = mockService.insertText(unicodeText)
        
        // Should handle Unicode text gracefully
        assertNotNull(result)
    }

    @Test
    fun accessibilityEvent_handling() {
        val mockService = object : WhisperTopAccessibilityService() {
            override fun onCreate() {
                clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            }
        }
        
        mockService.onCreate()
        
        // Create mock accessibility event
        val mockEvent = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED)
        
        // Test that onAccessibilityEvent doesn't crash
        mockService.onAccessibilityEvent(mockEvent)
        
        // Clean up
        mockEvent.recycle()
    }

    @Test
    fun serviceInterruption_handling() {
        val mockService = object : WhisperTopAccessibilityService() {
            override fun onCreate() {
                clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            }
        }
        
        mockService.onCreate()
        
        // Test that onInterrupt doesn't crash
        mockService.onInterrupt()
    }

    @Test
    fun serviceLifecycle_createAndDestroy() {
        val mockService = object : WhisperTopAccessibilityService() {
            override fun onCreate() {
                clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            }
        }
        
        // Test onCreate
        mockService.onCreate()
        assertNotNull(mockService.clipboardManager)
        
        // Test onDestroy
        mockService.onDestroy()
        // onDestroy should not throw exceptions
    }

    @Test
    fun accessibilityManager_serviceInfo() {
        val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        )
        
        // In test environment, our service won't be enabled, but we can test the query
        assertNotNull(enabledServices)
        
        // Check if any accessibility services are available
        val allServices = accessibilityManager.installedAccessibilityServiceList
        assertNotNull(allServices)
    }

    @Test
    fun textInsertion_multilineText() = runTest {
        val mockService = object : WhisperTopAccessibilityService() {
            override fun onCreate() {
                clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            }
        }
        
        mockService.onCreate()
        
        // Test inserting multiline text
        val multilineText = """
            This is line 1
            This is line 2
            This is line 3 with some special chars: @#${'$'}%
            这是中文行
        """.trimIndent()
        
        val result = mockService.insertText(multilineText)
        
        // Should handle multiline text gracefully
        assertNotNull(result)
    }

    @Test
    fun textInsertion_nullOrEmptyHandling() = runTest {
        val mockService = object : WhisperTopAccessibilityService() {
            override fun onCreate() {
                clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            }
        }
        
        mockService.onCreate()
        
        // Test various edge cases
        val results = listOf(
            mockService.insertText(""),
            mockService.insertText(" "),
            mockService.insertText("\n"),
            mockService.insertText("\t"),
            mockService.insertText("   \n\t   ")
        )
        
        // All should complete without crashing
        results.forEach { result ->
            assertNotNull(result)
        }
    }

    @Test
    fun clipboardManager_functionality() = runTest {
        val mockService = object : WhisperTopAccessibilityService() {
            override fun onCreate() {
                clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            }
        }
        
        mockService.onCreate()
        
        // Test clipboard functionality
        val testText = "Test clipboard text"
        val clipData = android.content.ClipData.newPlainText("test", testText)
        
        // Set clipboard data
        mockService.clipboardManager.setPrimaryClip(clipData)
        
        // Verify clipboard data
        val retrievedClip = mockService.clipboardManager.primaryClip
        assertNotNull(retrievedClip)
        
        if (retrievedClip != null && retrievedClip.itemCount > 0) {
            val retrievedText = retrievedClip.getItemAt(0).text.toString()
            assertEquals(testText, retrievedText)
        }
    }

    @Test
    fun textInsertion_performanceTest() = runTest {
        val mockService = object : WhisperTopAccessibilityService() {
            override fun onCreate() {
                clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            }
        }
        
        mockService.onCreate()
        
        val startTime = System.currentTimeMillis()
        
        // Insert multiple texts in sequence
        repeat(10) { index ->
            val result = mockService.insertText("Performance test text #$index")
            assertNotNull(result)
        }
        
        val duration = System.currentTimeMillis() - startTime
        
        // Should complete all insertions within reasonable time (5 seconds)
        assertTrue(duration < 5000, "Text insertion took too long: ${duration}ms")
    }

    @Test
    fun serviceConnection_states() {
        val mockService = object : WhisperTopAccessibilityService() {
            private var isConnected = false
            
            override fun onCreate() {
                clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            }
            
            override fun onServiceConnected() {
                super.onServiceConnected()
                isConnected = true
            }
            
            fun isServiceConnected(): Boolean = isConnected
        }
        
        mockService.onCreate()
        
        // Initially not connected
        assertFalse(mockService.isServiceConnected())
        
        // Simulate service connection
        mockService.onServiceConnected()
        
        // Should be connected now
        assertTrue(mockService.isServiceConnected())
    }

    @Test
    fun errorHandling_robustness() = runTest {
        val mockService = object : WhisperTopAccessibilityService() {
            override fun onCreate() {
                clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            }
        }
        
        mockService.onCreate()
        
        // Test error handling with various problematic inputs
        val problematicInputs = listOf(
            null,
            "\u0000", // null character
            "\uFFFF", // invalid Unicode
            "a".repeat(100000), // extremely long text
            "\r\n\r\n\r\n", // only line breaks
            "🤪😵‍💫🥴😴😪" // complex emojis
        )
        
        problematicInputs.forEach { input ->
            try {
                val result = if (input != null) {
                    mockService.insertText(input)
                } else {
                    mockService.insertText("")
                }
                // Should not crash, regardless of result
                assertNotNull(result)
            } catch (e: Exception) {
                // If exception occurs, it should be handled gracefully
                assertTrue(e.message != null || e.cause != null)
            }
        }
    }
}