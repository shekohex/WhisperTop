package me.shadykhalifa.whispertop.presentation.ui.components

import android.app.Application
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.koin.core.context.startKoin
import org.junit.After
import org.junit.Before
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class CustomModelInputDebouncingTest {

    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Before
    fun setUp() {
        // Clean up any existing Koin instance before each test
        try {
            stopKoin()
        } catch (e: Exception) {
            // Ignore if Koin wasn't started
        }
    }
    
    @After
    fun tearDown() {
        // Clean up Koin after each test
        try {
            stopKoin()
        } catch (e: Exception) {
            // Ignore if Koin wasn't started
        }
    }

    @Test
    fun `should display initial state correctly`() {
        // Given
        composeTestRule.setContent {
            CustomModelInput(
                customModelName = "",
                onCustomModelNameChange = {},
                onAddCustomModel = {},
                onCancel = {},
                isValid = true
            )
        }

        // Then - Should show empty text field
        composeTestRule.onNodeWithText("Add Custom Model").assertExists()
        composeTestRule.onNodeWithText("Add Model").assertExists()
        composeTestRule.onNodeWithText("Cancel").assertExists()
        
        // Initially, Add Model button should be disabled for empty input
        composeTestRule.onNodeWithText("Add Model").assertIsNotEnabled()
    }

    @Test
    fun `should enable button when text is entered`() {
        // Given
        composeTestRule.setContent {
            CustomModelInput(
                customModelName = "whisper-large-v3",
                onCustomModelNameChange = {},
                onAddCustomModel = {},
                onCancel = {},
                isValid = true
            )
        }

        // Then - Add Model button should be enabled with valid text
        composeTestRule.onNodeWithText("Add Model").assertIsEnabled()
    }

    @Test
    fun `should show error state when isValid is false`() {
        // Given
        composeTestRule.setContent {
            CustomModelInput(
                customModelName = "invalid-model",
                onCustomModelNameChange = {},
                onAddCustomModel = {},
                onCancel = {},
                isValid = false
            )
        }

        // Then - Should show error message
        composeTestRule.onNodeWithText("Please enter a valid model ID").assertExists()
        
        // And Add Model button should be disabled even with text
        composeTestRule.onNodeWithText("Add Model").assertIsNotEnabled()
    }

    @Test
    fun `should handle cancel action`() {
        // Given
        var cancelCalled = false
        
        composeTestRule.setContent {
            CustomModelInput(
                customModelName = "test-model",
                onCustomModelNameChange = {},
                onAddCustomModel = {},
                onCancel = { cancelCalled = true },
                isValid = true
            )
        }

        // When clicking cancel
        composeTestRule.onNodeWithText("Cancel").performClick()

        // Then - Cancel callback should be invoked
        assert(cancelCalled)
    }

    @Test
    fun `should handle add model action when valid`() {
        // Given
        var addModelCalled = false
        
        composeTestRule.setContent {
            CustomModelInput(
                customModelName = "valid-model",
                onCustomModelNameChange = {},
                onAddCustomModel = { addModelCalled = true },
                onCancel = {},
                isValid = true
            )
        }

        // When clicking add model
        composeTestRule.onNodeWithText("Add Model").performClick()

        // Then - Add model callback should be invoked
        assert(addModelCalled)
    }
}