package me.shadykhalifa.whispertop.presentation.viewmodels

import android.Manifest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import me.shadykhalifa.whispertop.managers.OnboardingPermissionManager
import me.shadykhalifa.whispertop.presentation.models.IndividualPermissionState
import me.shadykhalifa.whispertop.presentation.models.OnboardingPermissionState
import me.shadykhalifa.whispertop.presentation.models.OnboardingStep
import me.shadykhalifa.whispertop.presentation.models.PermissionState
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class OnboardingViewModelTest : KoinTest {
    
    private lateinit var mockPermissionManager: OnboardingPermissionManager
    private lateinit var viewModel: OnboardingViewModel
    private val testScope = TestScope(UnconfinedTestDispatcher())
    
    @Before
    fun setup() {
        stopKoin()
        
        mockPermissionManager = mock()
        
        startKoin {
            modules(
                module {
                    single { mockPermissionManager }
                }
            )
        }
        
        viewModel = OnboardingViewModel()
    }
    
    @Test
    fun `initial state should be correct`() = testScope.runTest {
        val initialProgress = viewModel.onboardingProgress.first()
        
        assertEquals(OnboardingStep.WELCOME, initialProgress.currentStep)
        assertTrue(initialProgress.completedSteps.isEmpty())
        assertFalse(initialProgress.isCompleted)
    }
    
    @Test
    fun `should navigate to next step when current step is completed`() = testScope.runTest {
        // Mock welcome step as completed
        whenever(mockPermissionManager.onboardingPermissionState).thenReturn(
            kotlinx.coroutines.flow.flowOf(createMockPermissionState())
        )
        
        viewModel.proceedToNextStep()
        
        val progress = viewModel.onboardingProgress.first()
        assertEquals(OnboardingStep.AUDIO_PERMISSION, progress.currentStep)
    }
    
    @Test
    fun `should update progress when audio permissions are granted`() = testScope.runTest {
        val permissionState = createMockPermissionState(
            audioRecording = PermissionState.Granted,
            foregroundService = PermissionState.Granted,
            notifications = PermissionState.Granted
        )
        
        whenever(mockPermissionManager.onboardingPermissionState).thenReturn(
            kotlinx.coroutines.flow.flowOf(permissionState)
        )
        
        // Re-create viewModel to trigger state observation
        viewModel = OnboardingViewModel()
        
        val progress = viewModel.onboardingProgress.first()
        assertTrue(progress.completedSteps.contains(OnboardingStep.AUDIO_PERMISSION))
    }
    
    @Test
    fun `should update progress when overlay permission is granted`() = testScope.runTest {
        val permissionState = createMockPermissionState(
            overlay = PermissionState.Granted
        )
        
        whenever(mockPermissionManager.onboardingPermissionState).thenReturn(
            kotlinx.coroutines.flow.flowOf(permissionState)
        )
        
        viewModel = OnboardingViewModel()
        
        val progress = viewModel.onboardingProgress.first()
        assertTrue(progress.completedSteps.contains(OnboardingStep.OVERLAY_PERMISSION))
    }
    
    @Test
    fun `should update progress when accessibility service is enabled`() = testScope.runTest {
        val permissionState = createMockPermissionState(
            accessibility = PermissionState.Granted
        )
        
        whenever(mockPermissionManager.onboardingPermissionState).thenReturn(
            kotlinx.coroutines.flow.flowOf(permissionState)
        )
        
        viewModel = OnboardingViewModel()
        
        val progress = viewModel.onboardingProgress.first()
        assertTrue(progress.completedSteps.contains(OnboardingStep.ACCESSIBILITY_SERVICE))
    }
    
    @Test
    fun `should mark onboarding as complete when all permissions are granted`() = testScope.runTest {
        val permissionState = createMockPermissionState(
            audioRecording = PermissionState.Granted,
            foregroundService = PermissionState.Granted,
            notifications = PermissionState.Granted,
            overlay = PermissionState.Granted,
            accessibility = PermissionState.Granted
        )
        
        whenever(mockPermissionManager.onboardingPermissionState).thenReturn(
            kotlinx.coroutines.flow.flowOf(permissionState)
        )
        
        viewModel = OnboardingViewModel()
        
        val progress = viewModel.onboardingProgress.first()
        assertTrue(progress.completedSteps.contains(OnboardingStep.COMPLETE))
    }
    
    @Test
    fun `should request audio permission when requestAudioPermission is called`() = testScope.runTest {
        viewModel.requestAudioPermission()
        
        verify(mockPermissionManager).requestAudioPermission()
    }
    
    @Test
    fun `should request overlay permission when requestOverlayPermission is called`() = testScope.runTest {
        viewModel.requestOverlayPermission()
        
        verify(mockPermissionManager).requestOverlayPermission()
    }
    
    @Test
    fun `should request accessibility permission when requestAccessibilityPermission is called`() = testScope.runTest {
        viewModel.requestAccessibilityPermission()
        
        verify(mockPermissionManager).requestAccessibilityPermission()
    }
    
    @Test
    fun `should open app settings when openAppSettings is called`() = testScope.runTest {
        viewModel.openAppSettings()
        
        verify(mockPermissionManager).openAppSettings()
    }
    
    @Test
    fun `should skip optional steps correctly`() = testScope.runTest {
        // Navigate to overlay permission step
        viewModel.navigateToStep(OnboardingStep.OVERLAY_PERMISSION)
        
        viewModel.skipCurrentStep()
        
        val progress = viewModel.onboardingProgress.first()
        assertTrue(progress.completedSteps.contains(OnboardingStep.OVERLAY_PERMISSION))
        assertEquals(OnboardingStep.ACCESSIBILITY_SERVICE, progress.currentStep)
    }
    
    @Test
    fun `should not allow skipping audio permission step`() = testScope.runTest {
        viewModel.navigateToStep(OnboardingStep.AUDIO_PERMISSION)
        
        viewModel.skipCurrentStep()
        
        // Should emit error event and not proceed
        val progress = viewModel.onboardingProgress.first()
        assertEquals(OnboardingStep.AUDIO_PERMISSION, progress.currentStep)
        assertFalse(progress.completedSteps.contains(OnboardingStep.AUDIO_PERMISSION))
    }
    
    @Test
    fun `should refresh permissions when refreshPermissions is called`() = testScope.runTest {
        viewModel.refreshPermissions()
        
        verify(mockPermissionManager).refreshAllPermissionStates()
    }
    
    @Test
    fun `should emit complete onboarding navigation event`() = testScope.runTest {
        viewModel.completeOnboarding()
        
        val event = viewModel.navigationEvent.first()
        assertEquals(OnboardingViewModel.NavigationEvent.CompleteOnboarding, event)
    }
    
    @Test
    fun `should go to previous step correctly`() = testScope.runTest {
        viewModel.navigateToStep(OnboardingStep.AUDIO_PERMISSION)
        
        viewModel.goToPreviousStep()
        
        val progress = viewModel.onboardingProgress.first()
        assertEquals(OnboardingStep.WELCOME, progress.currentStep)
    }
    
    @Test
    fun `should get permission denial reason correctly`() = testScope.runTest {
        val expectedReason = OnboardingPermissionManager.PermissionDenialReason.PermanentlyDenied
        whenever(mockPermissionManager.getPermissionDenialReason(Manifest.permission.RECORD_AUDIO))
            .thenReturn(expectedReason)
        
        val result = viewModel.getPermissionDenialReason(Manifest.permission.RECORD_AUDIO)
        
        assertEquals(expectedReason, result)
    }
    
    private fun createMockPermissionState(
        audioRecording: PermissionState = PermissionState.NotRequested,
        overlay: PermissionState = PermissionState.NotRequested,
        accessibility: PermissionState = PermissionState.NotRequested,
        notifications: PermissionState = PermissionState.NotRequested,
        foregroundService: PermissionState = PermissionState.NotRequested,
        foregroundServiceMicrophone: PermissionState = PermissionState.NotRequested
    ): OnboardingPermissionState {
        return OnboardingPermissionState(
            audioRecording = IndividualPermissionState(Manifest.permission.RECORD_AUDIO, audioRecording),
            overlay = IndividualPermissionState(Manifest.permission.SYSTEM_ALERT_WINDOW, overlay),
            accessibility = IndividualPermissionState("accessibility_service", accessibility),
            notifications = IndividualPermissionState(Manifest.permission.POST_NOTIFICATIONS, notifications),
            foregroundService = IndividualPermissionState(Manifest.permission.FOREGROUND_SERVICE, foregroundService),
            foregroundServiceMicrophone = IndividualPermissionState(Manifest.permission.FOREGROUND_SERVICE_MICROPHONE, foregroundServiceMicrophone)
        )
    }
}