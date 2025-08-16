package me.shadykhalifa.whispertop.presentation.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.shadykhalifa.whispertop.presentation.models.OnboardingStep
import me.shadykhalifa.whispertop.presentation.ui.components.onboarding.*
import me.shadykhalifa.whispertop.presentation.viewmodels.OnboardingViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel
) {
    val onboardingProgress by viewModel.onboardingProgress.collectAsState()
    val permissionState by viewModel.permissionState.collectAsState()
    val showRationale by viewModel.showRationale.collectAsState()
    
    val pagerState = rememberPagerState(
        initialPage = onboardingProgress.currentStep.stepNumber - 1,
        pageCount = { OnboardingStep.totalSteps }
    )
    val coroutineScope = rememberCoroutineScope()
    
    // Sync pager with view model state
    LaunchedEffect(onboardingProgress.currentStep) {
        val targetPage = onboardingProgress.currentStep.stepNumber - 1
        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }
    
    // Sync view model with pager state
    LaunchedEffect(pagerState.currentPage) {
        val step = OnboardingStep.getStepByNumber(pagerState.currentPage + 1)
        if (step != null && step != onboardingProgress.currentStep) {
            viewModel.navigateToStep(step)
        }
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Progress indicator
        OnboardingProgressIndicator(
            currentStep = onboardingProgress.currentStep.stepNumber,
            totalSteps = OnboardingStep.totalSteps,
            completedSteps = onboardingProgress.completedSteps.map { it.stepNumber }.toSet(),
            modifier = Modifier.padding(16.dp)
        )
        
        // Content
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            userScrollEnabled = false // Controlled programmatically
        ) { page ->
            val step = OnboardingStep.getStepByNumber(page + 1) ?: OnboardingStep.WELCOME
            
            when (step) {
                OnboardingStep.WELCOME -> {
                    WelcomeStep(
                        onContinue = {
                            coroutineScope.launch {
                                viewModel.proceedToNextStep()
                            }
                        }
                    )
                }
                OnboardingStep.AUDIO_PERMISSION -> {
                    AudioPermissionStep(
                        permissionState = permissionState.audioRecording,
                        notificationPermissionState = permissionState.notifications,
                        foregroundServicePermissionState = permissionState.foregroundService,
                        foregroundServiceMicrophonePermissionState = permissionState.foregroundServiceMicrophone,
                        onRequestPermission = viewModel::requestAudioPermission,
                        onOpenSettings = viewModel::openAppSettings,
                        onContinue = viewModel::proceedToNextStep,
                        onBack = viewModel::goToPreviousStep,
                        canProceed = onboardingProgress.canProceed
                    )
                }
                OnboardingStep.OVERLAY_PERMISSION -> {
                    OverlayPermissionStep(
                        permissionState = permissionState.overlay,
                        onRequestPermission = viewModel::requestOverlayPermission,
                        onOpenSettings = viewModel::openAppSettings,
                        onSkip = viewModel::skipCurrentStep,
                        onContinue = viewModel::proceedToNextStep,
                        onBack = viewModel::goToPreviousStep,
                        canProceed = onboardingProgress.canProceed
                    )
                }
                OnboardingStep.ACCESSIBILITY_SERVICE -> {
                    AccessibilityPermissionStep(
                        permissionState = permissionState.accessibility,
                        onRequestPermission = viewModel::requestAccessibilityPermission,
                        onOpenSettings = viewModel::openAppSettings,
                        onSkip = viewModel::skipCurrentStep,
                        onContinue = viewModel::proceedToNextStep,
                        onBack = viewModel::goToPreviousStep,
                        canProceed = onboardingProgress.canProceed
                    )
                }
                OnboardingStep.COMPLETE -> {
                    CompleteStep(
                        onGetStarted = viewModel::completeOnboarding,
                        onBack = viewModel::goToPreviousStep
                    )
                }
            }
        }
    }
    
    // Permission rationale dialog
    showRationale?.let { permission ->
        PermissionRationaleDialog(
            permission = permission,
            onDismiss = viewModel::hidePermissionRationale,
            onConfirm = {
                viewModel.hidePermissionRationale()
                when (permission) {
                    android.Manifest.permission.RECORD_AUDIO -> viewModel.requestAudioPermission()
                    android.Manifest.permission.SYSTEM_ALERT_WINDOW -> viewModel.requestOverlayPermission()
                    "accessibility_service" -> viewModel.requestAccessibilityPermission()
                }
            }
        )
    }
}