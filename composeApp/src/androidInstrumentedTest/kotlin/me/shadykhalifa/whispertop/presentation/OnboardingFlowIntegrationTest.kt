package me.shadykhalifa.whispertop.presentation

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import me.shadykhalifa.whispertop.managers.OnboardingPermissionManager
import me.shadykhalifa.whispertop.presentation.models.IndividualPermissionState
import me.shadykhalifa.whispertop.presentation.models.OnboardingPermissionState
import me.shadykhalifa.whispertop.presentation.models.PermissionState
import me.shadykhalifa.whispertop.presentation.ui.screens.OnboardingScreen
import me.shadykhalifa.whispertop.presentation.viewmodels.OnboardingViewModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class OnboardingFlowIntegrationTest : KoinTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    private lateinit var mockPermissionManager: OnboardingPermissionManager
    private val permissionStateFlow = MutableStateFlow(OnboardingPermissionState())
    
    @Before
    fun setup() {
        stopKoin()
        
        mockPermissionManager = mock()
        whenever(mockPermissionManager.onboardingPermissionState).thenReturn(permissionStateFlow)
        
        startKoin {
            modules(
                module {
                    single { mockPermissionManager }
                }
            )
        }
    }
    
    @Test
    fun onboardingFlow_welcomeScreen_shouldDisplayCorrectContent() {
        val viewModel = OnboardingViewModel()
        
        composeTestRule.setContent {
            OnboardingScreen(viewModel = viewModel)
        }
        
        composeTestRule.onNodeWithText("Welcome to WhisperTop").assertExists()
        composeTestRule.onNodeWithText("Get Started").assertExists()
        composeTestRule.onNodeWithText("Step 1 of 5").assertExists()
    }
    
    @Test
    fun onboardingFlow_audioPermissionScreen_shouldShowWhenNavigated() {
        val viewModel = OnboardingViewModel()
        
        composeTestRule.setContent {
            OnboardingScreen(viewModel = viewModel)
        }
        
        // Click Get Started to move to audio permission screen
        composeTestRule.onNodeWithText("Get Started").performClick()
        
        composeTestRule.onNodeWithText("Microphone Access").assertExists()
        composeTestRule.onNodeWithText("Grant Permission").assertExists()
        composeTestRule.onNodeWithText("Step 2 of 5").assertExists()
    }
    
    @Test
    fun onboardingFlow_audioPermissionGranted_shouldProceedToOverlay() {
        val viewModel = OnboardingViewModel()
        
        composeTestRule.setContent {
            OnboardingScreen(viewModel = viewModel)
        }
        
        // Navigate to audio permission screen
        composeTestRule.onNodeWithText("Get Started").performClick()
        
        // Simulate audio permission granted
        val grantedPermissionState = OnboardingPermissionState(
            audioRecording = IndividualPermissionState(
                android.Manifest.permission.RECORD_AUDIO,
                PermissionState.Granted
            ),
            foregroundService = IndividualPermissionState(
                android.Manifest.permission.FOREGROUND_SERVICE,
                PermissionState.Granted
            ),
            notifications = IndividualPermissionState(
                android.Manifest.permission.POST_NOTIFICATIONS,
                PermissionState.Granted
            )
        )
        permissionStateFlow.value = grantedPermissionState
        
        // Should now show continue button and allow proceeding
        composeTestRule.onNodeWithText("Continue").assertExists()
        composeTestRule.onNodeWithText("Continue").performClick()
        
        // Should navigate to overlay permission screen
        composeTestRule.onNodeWithText("Overlay Permission").assertExists()
        composeTestRule.onNodeWithText("Step 3 of 5").assertExists()
    }
    
    @Test
    fun onboardingFlow_overlayPermissionScreen_shouldAllowSkipping() {
        val viewModel = OnboardingViewModel()
        
        composeTestRule.setContent {
            OnboardingScreen(viewModel = viewModel)
        }
        
        // Navigate to overlay permission screen manually
        viewModel.navigateToStep(me.shadykhalifa.whispertop.presentation.models.OnboardingStep.OVERLAY_PERMISSION)
        
        composeTestRule.onNodeWithText("Overlay Permission").assertExists()
        composeTestRule.onNodeWithText("Skip This Step").assertExists()
        
        // Skip overlay permission
        composeTestRule.onNodeWithText("Skip This Step").performClick()
        
        // Should navigate to accessibility permission screen
        composeTestRule.onNodeWithText("Text Insertion Service").assertExists()
        composeTestRule.onNodeWithText("Step 4 of 5").assertExists()
    }
    
    @Test
    fun onboardingFlow_accessibilityPermissionScreen_shouldAllowSkipping() {
        val viewModel = OnboardingViewModel()
        
        composeTestRule.setContent {
            OnboardingScreen(viewModel = viewModel)
        }
        
        // Navigate to accessibility permission screen manually
        viewModel.navigateToStep(me.shadykhalifa.whispertop.presentation.models.OnboardingStep.ACCESSIBILITY_SERVICE)
        
        composeTestRule.onNodeWithText("Text Insertion Service").assertExists()
        composeTestRule.onNodeWithText("Skip This Step").assertExists()
        
        // Skip accessibility permission
        composeTestRule.onNodeWithText("Skip This Step").performClick()
        
        // Should navigate to complete screen
        composeTestRule.onNodeWithText("Setup Complete!").assertExists()
        composeTestRule.onNodeWithText("Step 5 of 5").assertExists()
    }
    
    @Test
    fun onboardingFlow_completeScreen_shouldShowWhenAllStepsCompleted() {
        val viewModel = OnboardingViewModel()
        
        composeTestRule.setContent {
            OnboardingScreen(viewModel = viewModel)
        }
        
        // Navigate to complete screen manually
        viewModel.navigateToStep(me.shadykhalifa.whispertop.presentation.models.OnboardingStep.COMPLETE)
        
        composeTestRule.onNodeWithText("Setup Complete!").assertExists()
        composeTestRule.onNodeWithText("Get Started").assertExists()
        composeTestRule.onNodeWithText("You're all set!").assertExists()
    }
    
    @Test
    fun onboardingFlow_progressIndicator_shouldShowCorrectProgress() {
        val viewModel = OnboardingViewModel()
        
        composeTestRule.setContent {
            OnboardingScreen(viewModel = viewModel)
        }
        
        // Check initial progress
        composeTestRule.onNodeWithText("Step 1 of 5").assertExists()
        
        // Navigate to next step
        composeTestRule.onNodeWithText("Get Started").performClick()
        composeTestRule.onNodeWithText("Step 2 of 5").assertExists()
    }
    
    @Test
    fun onboardingFlow_backNavigation_shouldWorkCorrectly() {
        val viewModel = OnboardingViewModel()
        
        composeTestRule.setContent {
            OnboardingScreen(viewModel = viewModel)
        }
        
        // Navigate forward
        composeTestRule.onNodeWithText("Get Started").performClick()
        composeTestRule.onNodeWithText("Microphone Access").assertExists()
        
        // Navigate back
        composeTestRule.onNodeWithText("Back").performClick()
        composeTestRule.onNodeWithText("Welcome to WhisperTop").assertExists()
        composeTestRule.onNodeWithText("Step 1 of 5").assertExists()
    }
    
    @Test
    fun onboardingFlow_permissionDenied_shouldShowTryAgainOption() {
        val viewModel = OnboardingViewModel()
        
        composeTestRule.setContent {
            OnboardingScreen(viewModel = viewModel)
        }
        
        // Navigate to audio permission screen
        composeTestRule.onNodeWithText("Get Started").performClick()
        
        // Simulate audio permission denied
        val deniedPermissionState = OnboardingPermissionState(
            audioRecording = IndividualPermissionState(
                android.Manifest.permission.RECORD_AUDIO,
                PermissionState.Denied
            )
        )
        permissionStateFlow.value = deniedPermissionState
        
        composeTestRule.onNodeWithText("Try Again").assertExists()
        composeTestRule.onNodeWithText("WhisperTop needs microphone access").assertExists()
    }
    
    @Test
    fun onboardingFlow_permissionPermanentlyDenied_shouldShowOpenSettingsOption() {
        val viewModel = OnboardingViewModel()
        
        composeTestRule.setContent {
            OnboardingScreen(viewModel = viewModel)
        }
        
        // Navigate to audio permission screen
        composeTestRule.onNodeWithText("Get Started").performClick()
        
        // Simulate audio permission permanently denied
        val permanentlyDeniedPermissionState = OnboardingPermissionState(
            audioRecording = IndividualPermissionState(
                android.Manifest.permission.RECORD_AUDIO,
                PermissionState.PermanentlyDenied
            )
        )
        permissionStateFlow.value = permanentlyDeniedPermissionState
        
        composeTestRule.onNodeWithText("Open Settings").assertExists()
        composeTestRule.onNodeWithText("Permission was permanently denied").assertExists()
    }
}