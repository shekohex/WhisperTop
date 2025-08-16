package me.shadykhalifa.whispertop.presentation

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.MutableStateFlow
import me.shadykhalifa.whispertop.domain.models.AppSettings
import me.shadykhalifa.whispertop.presentation.navigation.NavGraph
import me.shadykhalifa.whispertop.presentation.viewmodels.SettingsUiState
import me.shadykhalifa.whispertop.presentation.viewmodels.SettingsViewModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever

class NavigationTest : KoinTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Mock
    private lateinit var settingsViewModel: SettingsViewModel
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        // Mock the SettingsViewModel
        whenever(settingsViewModel.uiState).thenReturn(
            MutableStateFlow(SettingsUiState(settings = AppSettings()))
        )
        whenever(settingsViewModel.isLoading).thenReturn(MutableStateFlow(false))
        whenever(settingsViewModel.errorMessage).thenReturn(MutableStateFlow(null))
        
        // Setup Koin for testing
        startKoin {
            modules(module {
                single { settingsViewModel }
            })
        }
    }
    
    @Test
    fun navigation_startsAtHomeScreen() {
        composeTestRule.setContent {
            NavGraph()
        }
        
        // Verify we start at the home screen
        composeTestRule.onNodeWithText("WhisperTop").assertIsDisplayed()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
    }
    
    @Test
    fun navigation_navigatesToSettingsFromHome() {
        composeTestRule.setContent {
            NavGraph()
        }
        
        // Click Settings button on home screen
        composeTestRule.onNodeWithText("Settings").performClick()
        
        // Verify we navigated to settings screen
        composeTestRule.onNodeWithText("API Configuration").assertIsDisplayed()
        composeTestRule.onNodeWithText("Model Selection").assertIsDisplayed()
    }
    
    @Test
    fun navigation_navigatesBackFromSettings() {
        composeTestRule.setContent {
            NavGraph()
        }
        
        // Navigate to settings
        composeTestRule.onNodeWithText("Settings").performClick()
        
        // Verify we're on settings screen
        composeTestRule.onNodeWithText("API Configuration").assertIsDisplayed()
        
        // Click back button
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        
        // Verify we're back on home screen
        composeTestRule.onNodeWithText("WhisperTop").assertIsDisplayed()
    }
    
    @Test
    fun navigation_settingsScreenDisplaysCorrectTitle() {
        composeTestRule.setContent {
            NavGraph()
        }
        
        // Navigate to settings
        composeTestRule.onNodeWithText("Settings").performClick()
        
        // Verify settings screen title
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
    }
    
    fun tearDown() {
        stopKoin()
    }
}