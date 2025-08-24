package me.shadykhalifa.whispertop.presentation.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import me.shadykhalifa.whispertop.domain.models.TranscriptionHistoryItem
import me.shadykhalifa.whispertop.presentation.viewmodels.TranscriptionDetailUiEvent
import me.shadykhalifa.whispertop.presentation.viewmodels.TranscriptionDetailUiState
import me.shadykhalifa.whispertop.presentation.viewmodels.TranscriptionDetailViewModel
import me.shadykhalifa.whispertop.presentation.ui.theme.WhisperTopTheme
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest

class TranscriptionDetailScreenTest : KoinTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    private lateinit var mockViewModel: TranscriptionDetailViewModel
    private val uiStateFlow = MutableStateFlow(TranscriptionDetailUiState())
    private val uiEventsFlow = MutableStateFlow<TranscriptionDetailUiEvent?>(null)
    
    private val sampleTranscription = TranscriptionHistoryItem(
        id = "test-id",
        text = "This is a test transcription with a URL https://example.com and an email test@example.com",
        timestamp = 1234567890000L,
        duration = 30.5f,
        audioFilePath = "/path/to/audio.wav",
        confidence = 0.95f,
        customPrompt = "Custom prompt for testing",
        temperature = 0.3f,
        language = "en",
        model = "whisper-1"
    )
    
    @Before
    fun setup() {
        mockViewModel = mockk(relaxed = true)
        
        every { mockViewModel.uiState } returns uiStateFlow
        every { mockViewModel.uiEvents } returns uiEventsFlow
        
        startKoin {
            modules(
                module {
                    single { mockViewModel }
                }
            )
        }
    }
    
    @After
    fun tearDown() {
        stopKoin()
        clearAllMocks()
    }
    
    @Test
    fun loadingState_showsProgressIndicator() {
        // Given
        uiStateFlow.value = TranscriptionDetailUiState(isLoading = true)
        
        // When
        composeTestRule.setContent {
            WhisperTopTheme {
                TranscriptionDetailScreen(
                    transcriptionId = "test-id",
                    onNavigateBack = {}
                )
            }
        }
        
        // Then
        composeTestRule.onNodeWithText("Loading transcription...")
            .assertIsDisplayed()
        composeTestRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
            .assertIsDisplayed()
    }
    
    @Test
    fun errorState_showsErrorMessage() {
        // Given
        uiStateFlow.value = TranscriptionDetailUiState(error = "Transcription not found")
        
        // When
        composeTestRule.setContent {
            WhisperTopTheme {
                TranscriptionDetailScreen(
                    transcriptionId = "test-id",
                    onNavigateBack = {}
                )
            }
        }
        
        // Then
        composeTestRule.onNodeWithText("Transcription not found")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Go Back")
            .assertIsDisplayed()
    }
    
    @Test
    fun successState_displaysTranscriptionContent() {
        // Given
        uiStateFlow.value = TranscriptionDetailUiState(transcription = sampleTranscription)
        
        // When
        composeTestRule.setContent {
            WhisperTopTheme {
                TranscriptionDetailScreen(
                    transcriptionId = "test-id",
                    onNavigateBack = {}
                )
            }
        }
        
        // Then
        composeTestRule.onNodeWithText(sampleTranscription.text)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Details")
            .assertIsDisplayed()
    }
    
    @Test
    fun topAppBar_displaysCorrectTitle() {
        // Given
        uiStateFlow.value = TranscriptionDetailUiState(transcription = sampleTranscription)
        
        // When
        composeTestRule.setContent {
            WhisperTopTheme {
                TranscriptionDetailScreen(
                    transcriptionId = "test-id",
                    onNavigateBack = {}
                )
            }
        }
        
        // Then
        composeTestRule.onNodeWithText("Transcription Details")
            .assertIsDisplayed()
    }
    
    @Test
    fun actionButtons_areDisplayedWhenTranscriptionLoaded() {
        // Given
        uiStateFlow.value = TranscriptionDetailUiState(transcription = sampleTranscription)
        
        // When
        composeTestRule.setContent {
            WhisperTopTheme {
                TranscriptionDetailScreen(
                    transcriptionId = "test-id",
                    onNavigateBack = {}
                )
            }
        }
        
        // Then
        composeTestRule.onNodeWithContentDescription("Copy text")
            .assertIsDisplayed()
            .assertIsEnabled()
        composeTestRule.onNodeWithContentDescription("Share text")
            .assertIsDisplayed()
            .assertIsEnabled()
        composeTestRule.onNodeWithContentDescription("Delete transcription")
            .assertIsDisplayed()
            .assertIsEnabled()
    }
    
    @Test
    fun actionButtons_areDisabledWhenNoTranscriptionLoaded() {
        // Given
        uiStateFlow.value = TranscriptionDetailUiState() // No transcription
        
        // When
        composeTestRule.setContent {
            WhisperTopTheme {
                TranscriptionDetailScreen(
                    transcriptionId = "test-id",
                    onNavigateBack = {}
                )
            }
        }
        
        // Then
        composeTestRule.onNodeWithContentDescription("Copy text")
            .assertIsDisplayed()
            .assertIsNotEnabled()
        composeTestRule.onNodeWithContentDescription("Share text")
            .assertIsDisplayed()
            .assertIsNotEnabled()
        composeTestRule.onNodeWithContentDescription("Delete transcription")
            .assertIsDisplayed()
            .assertIsNotEnabled()
    }
    
    @Test
    fun copyButton_callsViewModelCopyToClipboard() {
        // Given
        uiStateFlow.value = TranscriptionDetailUiState(transcription = sampleTranscription)
        
        // When
        composeTestRule.setContent {
            WhisperTopTheme {
                TranscriptionDetailScreen(
                    transcriptionId = "test-id",
                    onNavigateBack = {}
                )
            }
        }
        
        composeTestRule.onNodeWithContentDescription("Copy text")
            .performClick()
        
        // Then
        verify { mockViewModel.copyToClipboard() }
    }
    
    @Test
    fun shareButton_callsViewModelShareTranscription() {
        // Given
        uiStateFlow.value = TranscriptionDetailUiState(transcription = sampleTranscription)
        
        // When
        composeTestRule.setContent {
            WhisperTopTheme {
                TranscriptionDetailScreen(
                    transcriptionId = "test-id",
                    onNavigateBack = {}
                )
            }
        }
        
        composeTestRule.onNodeWithContentDescription("Share text")
            .performClick()
        
        // Then
        verify { mockViewModel.shareTranscription() }
    }
    
    @Test
    fun deleteButton_showsConfirmationDialog() {
        // Given
        uiStateFlow.value = TranscriptionDetailUiState(transcription = sampleTranscription)
        
        // When
        composeTestRule.setContent {
            WhisperTopTheme {
                TranscriptionDetailScreen(
                    transcriptionId = "test-id",
                    onNavigateBack = {}
                )
            }
        }
        
        composeTestRule.onNodeWithContentDescription("Delete transcription")
            .performClick()
        
        // Then
        composeTestRule.onNodeWithText("Delete Transcription")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Are you sure you want to delete this transcription?")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Delete")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel")
            .assertIsDisplayed()
    }
    
    @Test
    fun deleteConfirmation_callsViewModelDeleteTranscription() {
        // Given
        uiStateFlow.value = TranscriptionDetailUiState(transcription = sampleTranscription)
        
        // When
        composeTestRule.setContent {
            WhisperTopTheme {
                TranscriptionDetailScreen(
                    transcriptionId = "test-id",
                    onNavigateBack = {}
                )
            }
        }
        
        composeTestRule.onNodeWithContentDescription("Delete transcription")
            .performClick()
        
        composeTestRule.onNodeWithText("Delete")
            .performClick()
        
        // Then
        verify { mockViewModel.deleteTranscription() }
    }
    
    @Test
    fun metadataSection_displaysTranscriptionDetails() {
        // Given
        uiStateFlow.value = TranscriptionDetailUiState(transcription = sampleTranscription)
        
        // When
        composeTestRule.setContent {
            WhisperTopTheme {
                TranscriptionDetailScreen(
                    transcriptionId = "test-id",
                    onNavigateBack = {}
                )
            }
        }
        
        // Then
        composeTestRule.onNodeWithText("Date & Time")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Duration")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Word Count")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Character Count")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Language")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Model")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Confidence")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Custom Prompt")
            .assertIsDisplayed()
    }
    
    @Test
    fun textSelection_isAvailable() {
        // Given
        uiStateFlow.value = TranscriptionDetailUiState(transcription = sampleTranscription)
        
        // When
        composeTestRule.setContent {
            WhisperTopTheme {
                TranscriptionDetailScreen(
                    transcriptionId = "test-id",
                    onNavigateBack = {}
                )
            }
        }
        
        // Then - Text should be selectable (SelectionContainer enables this)
        composeTestRule.onNodeWithText(sampleTranscription.text)
            .assertIsDisplayed()
            .performTouchInput { 
                longClick() // This should trigger text selection
            }
    }
    
    @Test
    fun backButton_callsNavigateBack() {
        // Given
        uiStateFlow.value = TranscriptionDetailUiState(transcription = sampleTranscription)
        val mockNavigateBack = mockk<() -> Unit>(relaxed = true)
        
        // When
        composeTestRule.setContent {
            WhisperTopTheme {
                TranscriptionDetailScreen(
                    transcriptionId = "test-id",
                    onNavigateBack = mockNavigateBack
                )
            }
        }
        
        composeTestRule.onNodeWithContentDescription("Go back")
            .performClick()
        
        // Then
        verify { mockNavigateBack() }
    }
    
    @Test
    fun loadTranscription_isCalledOnLaunch() {
        // Given
        val transcriptionId = "test-transcription-id"
        
        // When
        composeTestRule.setContent {
            WhisperTopTheme {
                TranscriptionDetailScreen(
                    transcriptionId = transcriptionId,
                    onNavigateBack = {}
                )
            }
        }
        
        // Then
        verify { mockViewModel.loadTranscription(transcriptionId) }
    }
    
    @Test
    fun accessibility_contentDescriptionsAreSet() {
        // Given
        uiStateFlow.value = TranscriptionDetailUiState(transcription = sampleTranscription)
        
        // When
        composeTestRule.setContent {
            WhisperTopTheme {
                TranscriptionDetailScreen(
                    transcriptionId = "test-id",
                    onNavigateBack = {}
                )
            }
        }
        
        // Then
        composeTestRule.onNodeWithContentDescription("Navigate back to transcription history")
            .assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Copy transcription text to clipboard")
            .assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Share transcription text")
            .assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Delete this transcription permanently")
            .assertIsDisplayed()
    }
}