package me.shadykhalifa.whispertop.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.datetime.Clock
import me.shadykhalifa.whispertop.domain.models.TranscriptionHistory
import me.shadykhalifa.whispertop.presentation.components.TranscriptionHistoryItem
import me.shadykhalifa.whispertop.ui.theme.WhisperTopTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class ComposeUITest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun basicButton_clickAndDisplay() {
        var clicked = false
        
        composeTestRule.setContent {
            WhisperTopTheme {
                Button(
                    onClick = { clicked = true },
                    modifier = Modifier.testTag("test_button")
                ) {
                    Text("Click Me")
                }
            }
        }

        // Verify button is displayed
        composeTestRule
            .onNodeWithText("Click Me")
            .assertIsDisplayed()
            .assertHasClickAction()

        // Click button
        composeTestRule
            .onNodeWithText("Click Me")
            .performClick()

        // Verify click was handled
        assertTrue(clicked)
    }

    @Test
    fun textField_inputAndValidation() {
        var textValue = ""
        
        composeTestRule.setContent {
            WhisperTopTheme {
                TextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    label = { Text("Enter API Key") },
                    modifier = Modifier.testTag("api_key_field")
                )
            }
        }

        // Type text into field
        composeTestRule
            .onNodeWithTag("api_key_field")
            .performTextInput("MOCK_API_KEY_FOR_TESTING")

        // Verify text was entered
        composeTestRule
            .onNodeWithTag("api_key_field")
            .assertTextContains("MOCK_API_KEY_FOR_TESTING")
    }

    @Test
    fun floatingActionButton_stateChanges() {
        var isRecording by mutableStateOf(false)
        
        composeTestRule.setContent {
            WhisperTopTheme {
                FloatingActionButton(
                    onClick = { isRecording = !isRecording },
                    modifier = Modifier.testTag("record_fab")
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Settings else Icons.Default.PlayArrow,
                        contentDescription = if (isRecording) "Stop Recording" else "Start Recording"
                    )
                }
            }
        }

        // Initially should show play icon
        composeTestRule
            .onNodeWithContentDescription("Start Recording")
            .assertIsDisplayed()

        // Click to start recording
        composeTestRule
            .onNodeWithContentDescription("Start Recording")
            .performClick()

        // Should now show stop icon
        composeTestRule
            .onNodeWithContentDescription("Stop Recording")
            .assertIsDisplayed()
    }

    @Test
    fun lazyColumn_scrollingAndContent() {
        val items = (1..50).map { "Item $it" }
        
        composeTestRule.setContent {
            WhisperTopTheme {
                LazyColumn(
                    modifier = Modifier.testTag("item_list")
                ) {
                    items(items) { item ->
                        Text(
                            text = item,
                            modifier = Modifier
                                .padding(16.dp)
                                .testTag("list_item_$item")
                        )
                    }
                }
            }
        }

        // Verify first item is visible
        composeTestRule
            .onNodeWithText("Item 1")
            .assertIsDisplayed()

        // Scroll to bottom
        composeTestRule
            .onNodeWithTag("item_list")
            .performScrollToIndex(49)

        // Verify last item is now visible
        composeTestRule
            .onNodeWithText("Item 50")
            .assertIsDisplayed()
    }

    @Test
    fun transcriptionHistoryList_itemInteractions() {
        val transcriptions = listOf(
            TranscriptionHistory(
                id = "1",
                text = "First transcription",
                timestamp = Clock.System.now().toEpochMilliseconds(),
                duration = 5f,
                confidence = 0.95f,
                wordCount = 2
            ),
            TranscriptionHistory(
                id = "2", 
                text = "Second transcription",
                timestamp = Clock.System.now().toEpochMilliseconds() - 60000,
                duration = 3f,
                confidence = 0.88f,
                wordCount = 2
            )
        )
        
        var deletedId: String? = null
        var clickedId: String? = null
        
        composeTestRule.setContent {
            WhisperTopTheme {
                LazyColumn {
                    items(transcriptions) { transcription ->
                        TranscriptionHistoryItem(
                            transcription = transcription,
                            onItemClick = { clickedId = it.id },
                            onDeleteClick = { deletedId = it.id },
                            modifier = Modifier.testTag("item_${transcription.id}")
                        )
                    }
                }
            }
        }

        // Verify items are displayed
        composeTestRule
            .onNodeWithText("First transcription")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Second transcription")
            .assertIsDisplayed()

        // Click on first item
        composeTestRule
            .onNodeWithTag("item_1")
            .performClick()

        assertEquals("1", clickedId)
    }

    @Test
    fun materialTheme_colorSchemeApplication() {
        composeTestRule.setContent {
            WhisperTopTheme {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("themed_surface")
                ) {
                    Text(
                        text = "Themed Text",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }
        }

        // Verify themed components are displayed
        composeTestRule
            .onNodeWithText("Themed Text")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithTag("themed_surface")
            .assertIsDisplayed()
    }

    @Test
    fun buttonStates_enabledDisabled() {
        var isEnabled by mutableStateOf(true)
        
        composeTestRule.setContent {
            WhisperTopTheme {
                Column {
                    Button(
                        onClick = { /* no-op */ },
                        enabled = isEnabled,
                        modifier = Modifier.testTag("state_button")
                    ) {
                        Text("State Button")
                    }
                    
                    Button(
                        onClick = { isEnabled = !isEnabled }
                    ) {
                        Text("Toggle State")
                    }
                }
            }
        }

        // Initially enabled
        composeTestRule
            .onNodeWithTag("state_button")
            .assertIsEnabled()

        // Toggle to disabled
        composeTestRule
            .onNodeWithText("Toggle State")
            .performClick()

        // Should now be disabled
        composeTestRule
            .onNodeWithTag("state_button")
            .assertIsNotEnabled()
    }

    @Test
    fun complexLayout_multipleComponents() {
        var selectedTab by mutableStateOf(0)
        
        composeTestRule.setContent {
            WhisperTopTheme {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Header
                    Text(
                        text = "WhisperTop Dashboard",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier
                            .padding(16.dp)
                            .testTag("header")
                    )
                    
                    // Tab buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("Recent", "All", "Settings").forEachIndexed { index, title ->
                            Button(
                                onClick = { selectedTab = index },
                                modifier = Modifier.testTag("tab_$index")
                            ) {
                                Text(title)
                            }
                        }
                    }
                    
                    // Content based on selected tab
                    when (selectedTab) {
                        0 -> Text(
                            "Recent Transcriptions",
                            modifier = Modifier.testTag("content_recent")
                        )
                        1 -> Text(
                            "All Transcriptions", 
                            modifier = Modifier.testTag("content_all")
                        )
                        2 -> Text(
                            "Settings Panel",
                            modifier = Modifier.testTag("content_settings")
                        )
                    }
                }
            }
        }

        // Verify header
        composeTestRule
            .onNodeWithTag("header")
            .assertIsDisplayed()
            .assertTextEquals("WhisperTop Dashboard")

        // Verify initial content
        composeTestRule
            .onNodeWithTag("content_recent")
            .assertIsDisplayed()

        // Switch to All tab
        composeTestRule
            .onNodeWithTag("tab_1")
            .performClick()

        composeTestRule
            .onNodeWithTag("content_all")
            .assertIsDisplayed()

        // Switch to Settings tab
        composeTestRule
            .onNodeWithTag("tab_2")
            .performClick()

        composeTestRule
            .onNodeWithTag("content_settings")
            .assertIsDisplayed()
    }

    @Test
    fun gestureInteractions_swipeAndTouch() {
        var swipeDirection: String? = null
        
        composeTestRule.setContent {
            WhisperTopTheme {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .testTag("swipeable_card")
                ) {
                    Text(
                        text = "Swipe me!",
                        modifier = Modifier
                            .padding(32.dp)
                            .semantics {
                                contentDescription = "Swipeable card content"
                            }
                    )
                }
            }
        }

        // Perform swipe gesture
        composeTestRule
            .onNodeWithTag("swipeable_card")
            .performTouchInput {
                swipeUp()
            }

        // Verify card is still displayed after swipe
        composeTestRule
            .onNodeWithText("Swipe me!")
            .assertIsDisplayed()
    }

    @Test
    fun dynamicContentUpdate_recomposition() {
        var counter by mutableStateOf(0)
        
        composeTestRule.setContent {
            WhisperTopTheme {
                Column {
                    Text(
                        text = "Count: $counter",
                        modifier = Modifier.testTag("counter_display")
                    )
                    
                    Button(
                        onClick = { counter++ },
                        modifier = Modifier.testTag("increment_button")
                    ) {
                        Text("Increment")
                    }
                    
                    OutlinedButton(
                        onClick = { counter = 0 },
                        modifier = Modifier.testTag("reset_button")
                    ) {
                        Text("Reset")
                    }
                }
            }
        }

        // Initial state
        composeTestRule
            .onNodeWithTag("counter_display")
            .assertTextEquals("Count: 0")

        // Increment counter
        repeat(5) {
            composeTestRule
                .onNodeWithTag("increment_button")
                .performClick()
        }

        composeTestRule
            .onNodeWithTag("counter_display")
            .assertTextEquals("Count: 5")

        // Reset counter
        composeTestRule
            .onNodeWithTag("reset_button")
            .performClick()

        composeTestRule
            .onNodeWithTag("counter_display")
            .assertTextEquals("Count: 0")
    }

    @Test
    fun conditionalRendering_showHideContent() {
        var showContent by mutableStateOf(false)
        
        composeTestRule.setContent {
            WhisperTopTheme {
                Column {
                    Button(
                        onClick = { showContent = !showContent },
                        modifier = Modifier.testTag("toggle_button")
                    ) {
                        Text(if (showContent) "Hide" else "Show")
                    }
                    
                    if (showContent) {
                        Card(
                            modifier = Modifier.testTag("conditional_content")
                        ) {
                            Text(
                                text = "This content is conditionally rendered",
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Initially hidden
        composeTestRule
            .onNodeWithTag("conditional_content")
            .assertDoesNotExist()

        // Show content
        composeTestRule
            .onNodeWithTag("toggle_button")
            .performClick()

        composeTestRule
            .onNodeWithTag("conditional_content")
            .assertIsDisplayed()

        // Hide content again
        composeTestRule
            .onNodeWithTag("toggle_button")
            .performClick()

        composeTestRule
            .onNodeWithTag("conditional_content")
            .assertDoesNotExist()
    }

    @Test
    fun multipleNodes_sameContent() {
        composeTestRule.setContent {
            WhisperTopTheme {
                Column {
                    repeat(3) { index ->
                        Button(
                            onClick = { },
                            modifier = Modifier.testTag("button_$index")
                        ) {
                            Text("Button")
                        }
                    }
                }
            }
        }

        // Verify all buttons with same text exist
        val buttons = composeTestRule.onAllNodesWithText("Button")
        buttons.assertCountEquals(3)

        // Verify each button by tag
        repeat(3) { index ->
            composeTestRule
                .onNodeWithTag("button_$index")
                .assertIsDisplayed()
                .assertHasClickAction()
        }
    }

    @Test
    fun textField_clearAndReplace() {
        var textValue by mutableStateOf("Initial Text")
        
        composeTestRule.setContent {
            WhisperTopTheme {
                TextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    modifier = Modifier.testTag("editable_field")
                )
            }
        }

        // Verify initial text
        composeTestRule
            .onNodeWithTag("editable_field")
            .assertTextContains("Initial Text")

        // Clear and replace
        composeTestRule
            .onNodeWithTag("editable_field")
            .performTextClearance()
            .performTextInput("New Text")

        composeTestRule
            .onNodeWithTag("editable_field")
            .assertTextContains("New Text")
    }
}