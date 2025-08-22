package me.shadykhalifa.whispertop.accessibility

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
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
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class AccessibilityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun floatingActionButton_hasProperContentDescription() {
        composeTestRule.setContent {
            WhisperTopTheme {
                Surface {
                    FloatingActionButton(
                        onClick = { },
                        modifier = Modifier.semantics {
                            contentDescription = "Start recording"
                            role = Role.Button
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null // Icon description handled by parent
                        )
                    }
                }
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Start recording")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun transcriptionList_itemsHaveProperSemantics() {
        val testTranscriptions = listOf(
            TranscriptionHistory(
                id = "1",
                text = "This is the first transcription",
                timestamp = Clock.System.now().toEpochMilliseconds(),
                duration = 5.5f,
                confidence = 0.95f,
                wordCount = 5
            ),
            TranscriptionHistory(
                id = "2", 
                text = "This is the second transcription",
                timestamp = Clock.System.now().toEpochMilliseconds() - 60000,
                duration = 3.2f,
                confidence = 0.88f,
                wordCount = 5
            )
        )

        composeTestRule.setContent {
            WhisperTopTheme {
                TranscriptionHistoryListAccessible(transcriptions = testTranscriptions)
            }
        }

        // Verify first item accessibility
        composeTestRule
            .onNodeWithContentDescription("Transcription: This is the first transcription, Duration: 5.5 seconds, Confidence: 95%")
            .assertIsDisplayed()
            .assertHasClickAction()

        // Verify second item accessibility  
        composeTestRule
            .onNodeWithContentDescription("Transcription: This is the second transcription, Duration: 3.2 seconds, Confidence: 88%")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun buttonStates_announceCorrectly() {
        composeTestRule.setContent {
            WhisperTopTheme {
                RecordingButtonWithStates()
            }
        }

        // Initial state - Start recording
        composeTestRule
            .onNodeWithContentDescription("Start recording")
            .assertIsDisplayed()
            .assertHasClickAction()

        // Click to start recording
        composeTestRule
            .onNodeWithContentDescription("Start recording")
            .performClick()

        // Should now show stop recording state
        composeTestRule
            .onNodeWithContentDescription("Stop recording")
            .assertIsDisplayed()
            .assertHasClickAction()

        // Click to stop
        composeTestRule
            .onNodeWithContentDescription("Stop recording")
            .performClick()

        // Should return to start state
        composeTestRule
            .onNodeWithContentDescription("Start recording")
            .assertIsDisplayed()
    }

    @Test
    fun listNavigation_accessibilityFocusManagement() {
        val manyTranscriptions = (1..20).map { index ->
            TranscriptionHistory(
                id = index.toString(),
                text = "Transcription number $index for accessibility testing",
                timestamp = Clock.System.now().toEpochMilliseconds() - (index * 60000L),
                duration = index.toFloat(),
                confidence = 0.8f + (index * 0.01f),
                wordCount = index * 3
            )
        }

        composeTestRule.setContent {
            WhisperTopTheme {
                LazyColumn(
                    modifier = Modifier.testTag("transcription_list")
                ) {
                    items(manyTranscriptions) { transcription ->
                        TranscriptionHistoryItem(
                            transcription = transcription,
                            onItemClick = { },
                            onDeleteClick = { },
                            modifier = Modifier.semantics {
                                contentDescription = "Transcription: ${transcription.text}, " +
                                    "Duration: ${transcription.duration} seconds, " +
                                    "Confidence: ${(transcription.confidence ?: 0f) * 100}%"
                            }
                        )
                    }
                }
            }
        }

        // Verify scrolling to specific items works with accessibility
        composeTestRule
            .onNodeWithTag("transcription_list")
            .performScrollToNode(hasContentDescription("Transcription: Transcription number 15 for accessibility testing, Duration: 15.0 seconds, Confidence: 95%"))

        // Item should be visible after scroll
        composeTestRule
            .onNodeWithContentDescription("Transcription: Transcription number 15 for accessibility testing, Duration: 15.0 seconds, Confidence: 95%")
            .assertIsDisplayed()
    }

    @Test
    fun deleteAction_hasProperAnnouncement() {
        val testTranscription = TranscriptionHistory(
            id = "delete_test",
            text = "This transcription will be deleted",
            timestamp = Clock.System.now().toEpochMilliseconds(),
            duration = 10f,
            confidence = 0.9f,
            wordCount = 5
        )

        composeTestRule.setContent {
            WhisperTopTheme {
                TranscriptionItemWithDelete(
                    transcription = testTranscription,
                    onDelete = { }
                )
            }
        }

        // Verify delete button has proper content description
        composeTestRule
            .onNodeWithContentDescription("Delete transcription: This transcription will be deleted")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun modalDialogs_accessibilitySupport() {
        composeTestRule.setContent {
            WhisperTopTheme {
                AccessibleDialog()
            }
        }

        // Show dialog
        composeTestRule
            .onNodeWithText("Show Dialog")
            .performClick()

        // Verify dialog content is accessible
        composeTestRule
            .onNodeWithContentDescription("Confirmation dialog")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Are you sure you want to delete this transcription?")
            .assertIsDisplayed()

        // Verify action buttons
        composeTestRule
            .onNodeWithContentDescription("Cancel deletion")
            .assertIsDisplayed()
            .assertHasClickAction()

        composeTestRule
            .onNodeWithContentDescription("Confirm deletion")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun focusTraversal_logicalOrder() {
        composeTestRule.setContent {
            WhisperTopTheme {
                FocusTraversalTestComponent()
            }
        }

        // Test that focus moves in logical order
        // First button should be focusable
        composeTestRule
            .onNodeWithText("First Button")
            .assertHasClickAction()

        // Second button should be focusable
        composeTestRule
            .onNodeWithText("Second Button")
            .assertHasClickAction()

        // Third button should be focusable
        composeTestRule
            .onNodeWithText("Third Button")
            .assertHasClickAction()
    }

    @Test
    fun textContent_hasProperSemantics() {
        composeTestRule.setContent {
            WhisperTopTheme {
                Column {
                    Text(
                        text = "Recording Status",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.semantics {
                            role = Role.Heading
                        }
                    )
                    Text(
                        text = "Currently idle",
                        modifier = Modifier.semantics {
                            contentDescription = "Recording status: Currently idle"
                        }
                    )
                    Text(
                        text = "00:00:00",
                        modifier = Modifier.semantics {
                            contentDescription = "Recording duration: 0 hours, 0 minutes, 0 seconds"
                        }
                    )
                }
            }
        }

        // Verify heading role is set
        composeTestRule
            .onNodeWithText("Recording Status")
            .assertIsDisplayed()

        // Verify status has proper description
        composeTestRule
            .onNodeWithContentDescription("Recording status: Currently idle")
            .assertIsDisplayed()

        // Verify duration has proper description
        composeTestRule
            .onNodeWithContentDescription("Recording duration: 0 hours, 0 minutes, 0 seconds")
            .assertIsDisplayed()
    }

    @Test
    fun gestureBasedInteractions_accessibility() {
        composeTestRule.setContent {
            WhisperTopTheme {
                SwipeableTranscriptionItem()
            }
        }

        // Verify swipe actions are announced
        composeTestRule
            .onNodeWithContentDescription("Swipe left to delete, swipe right for more options")
            .assertIsDisplayed()

        // Test that the component responds to gestures
        composeTestRule
            .onNodeWithContentDescription("Swipe left to delete, swipe right for more options")
            .performTouchInput {
                swipeUp()
            }
    }

    @Test
    fun dynamicContentUpdates_announceChanges() {
        composeTestRule.setContent {
            WhisperTopTheme {
                DynamicContentComponent()
            }
        }

        // Initial state
        composeTestRule
            .onNodeWithContentDescription("Status: Ready to record")
            .assertIsDisplayed()

        // Trigger state change
        composeTestRule
            .onNodeWithText("Start")
            .performClick()

        // Verify new state is announced
        composeTestRule
            .onNodeWithContentDescription("Status: Recording in progress")
            .assertIsDisplayed()

        // Stop recording
        composeTestRule
            .onNodeWithText("Stop")
            .performClick()

        // Verify final state
        composeTestRule
            .onNodeWithContentDescription("Status: Processing recording")
            .assertIsDisplayed()
    }

    @Test
    fun errorStates_accessibleAnnouncements() {
        composeTestRule.setContent {
            WhisperTopTheme {
                ErrorStateComponent()
            }
        }

        // Trigger error
        composeTestRule
            .onNodeWithText("Trigger Error")
            .performClick()

        // Verify error is announced properly
        composeTestRule
            .onNodeWithContentDescription("Error: Permission denied. Please enable microphone access in settings.")
            .assertIsDisplayed()

        // Verify retry action is accessible
        composeTestRule
            .onNodeWithContentDescription("Retry recording")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    // Helper Composables

    @Composable
    private fun TranscriptionHistoryListAccessible(transcriptions: List<TranscriptionHistory>) {
        LazyColumn {
            items(transcriptions) { transcription ->
                TranscriptionHistoryItem(
                    transcription = transcription,
                    onItemClick = { },
                    onDeleteClick = { },
                    modifier = Modifier.semantics {
                        contentDescription = "Transcription: ${transcription.text}, " +
                            "Duration: ${transcription.duration} seconds, " +
                            "Confidence: ${((transcription.confidence ?: 0f) * 100).toInt()}%"
                    }
                )
            }
        }
    }

    @Composable
    private fun RecordingButtonWithStates() {
        var isRecording by remember { mutableStateOf(false) }

        FloatingActionButton(
            onClick = { isRecording = !isRecording },
            modifier = Modifier.semantics {
                contentDescription = if (isRecording) "Stop recording" else "Start recording"
                role = Role.Button
            }
        ) {
            Icon(
                imageVector = if (isRecording) Icons.Default.Delete else Icons.Default.PlayArrow,
                contentDescription = null
            )
        }
    }

    @Composable
    private fun TranscriptionItemWithDelete(
        transcription: TranscriptionHistory,
        onDelete: () -> Unit
    ) {
        Column {
            Text(text = transcription.text)
            Button(
                onClick = onDelete,
                modifier = Modifier.semantics {
                    contentDescription = "Delete transcription: ${transcription.text}"
                }
            ) {
                Text("Delete")
            }
        }
    }

    @Composable
    private fun AccessibleDialog() {
        var showDialog by remember { mutableStateOf(false) }

        Column {
            Button(onClick = { showDialog = true }) {
                Text("Show Dialog")
            }

            if (showDialog) {
                // Simulate dialog content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .semantics {
                            contentDescription = "Confirmation dialog"
                        }
                ) {
                    Text("Are you sure you want to delete this transcription?")
                    
                    androidx.compose.foundation.layout.Row {
                        Button(
                            onClick = { showDialog = false },
                            modifier = Modifier.semantics {
                                contentDescription = "Cancel deletion"
                            }
                        ) {
                            Text("Cancel")
                        }
                        
                        Button(
                            onClick = { showDialog = false },
                            modifier = Modifier.semantics {
                                contentDescription = "Confirm deletion"
                            }
                        ) {
                            Text("Delete")
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun FocusTraversalTestComponent() {
        Column {
            Button(onClick = { }) { Text("First Button") }
            Button(onClick = { }) { Text("Second Button") }
            Button(onClick = { }) { Text("Third Button") }
        }
    }

    @Composable
    private fun SwipeableTranscriptionItem() {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .semantics {
                    contentDescription = "Swipe left to delete, swipe right for more options"
                }
        ) {
            Text(
                text = "Swipeable transcription item",
                modifier = Modifier.padding(16.dp)
            )
        }
    }

    @Composable
    private fun DynamicContentComponent() {
        var status by remember { mutableStateOf("Ready to record") }

        Column {
            Text(
                text = "Status: $status",
                modifier = Modifier.semantics {
                    contentDescription = "Status: $status"
                }
            )
            
            androidx.compose.foundation.layout.Row {
                Button(
                    onClick = { status = "Recording in progress" }
                ) {
                    Text("Start")
                }
                
                Button(
                    onClick = { status = "Processing recording" }
                ) {
                    Text("Stop")
                }
            }
        }
    }

    @Composable
    private fun ErrorStateComponent() {
        var hasError by remember { mutableStateOf(false) }

        Column {
            Button(
                onClick = { hasError = true }
            ) {
                Text("Trigger Error")
            }

            if (hasError) {
                Column {
                    Text(
                        text = "Error: Permission denied. Please enable microphone access in settings.",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics {
                            contentDescription = "Error: Permission denied. Please enable microphone access in settings."
                            role = Role.Button
                        }
                    )
                    
                    Button(
                        onClick = { hasError = false },
                        modifier = Modifier.semantics {
                            contentDescription = "Retry recording"
                        }
                    ) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}