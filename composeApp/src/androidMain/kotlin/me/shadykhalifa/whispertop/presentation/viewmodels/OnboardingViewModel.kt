package me.shadykhalifa.whispertop.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import me.shadykhalifa.whispertop.managers.OnboardingPermissionManager
import me.shadykhalifa.whispertop.presentation.models.OnboardingProgress
import me.shadykhalifa.whispertop.presentation.models.OnboardingStep
import me.shadykhalifa.whispertop.presentation.models.PermissionState
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class OnboardingViewModel : ViewModel(), KoinComponent {
    
    private val permissionManager: OnboardingPermissionManager by inject()
    
    private val _onboardingProgress = MutableStateFlow(OnboardingProgress())
    val onboardingProgress: StateFlow<OnboardingProgress> = _onboardingProgress.asStateFlow()
    
    private val _showRationale = MutableStateFlow<String?>(null)
    val showRationale: StateFlow<String?> = _showRationale.asStateFlow()
    
    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent: SharedFlow<NavigationEvent> = _navigationEvent.asSharedFlow()
    
    val permissionState = permissionManager.onboardingPermissionState
    
    init {
        observePermissionChanges()
    }
    
    private fun observePermissionChanges() {
        viewModelScope.launch {
            permissionState.collect { permissions ->
                updateOnboardingProgress(permissions)
            }
        }
    }
    
    private fun updateOnboardingProgress(permissions: me.shadykhalifa.whispertop.presentation.models.OnboardingPermissionState) {
        val currentProgress = _onboardingProgress.value
        val completedSteps = mutableSetOf<OnboardingStep>()
        
        // Always mark welcome as completed if we've moved past it
        if (currentProgress.currentStep.stepNumber > OnboardingStep.WELCOME.stepNumber) {
            completedSteps.add(OnboardingStep.WELCOME)
        }
        
        // Check audio permission completion
        if (permissions.audioRecording.state == PermissionState.Granted &&
            permissions.foregroundService.state == PermissionState.Granted &&
            (!permissions.notifications.isRequired || permissions.notifications.state == PermissionState.Granted) &&
            (!permissions.foregroundServiceMicrophone.isRequired || permissions.foregroundServiceMicrophone.state == PermissionState.Granted)
        ) {
            completedSteps.add(OnboardingStep.AUDIO_PERMISSION)
        }
        
        // Check overlay permission completion
        if (permissions.overlay.state == PermissionState.Granted) {
            completedSteps.add(OnboardingStep.OVERLAY_PERMISSION)
        }
        
        // Check accessibility service completion
        if (permissions.accessibility.state == PermissionState.Granted) {
            completedSteps.add(OnboardingStep.ACCESSIBILITY_SERVICE)
        }
        
        // Check if all steps are completed
        if (completedSteps.containsAll(
                listOf(
                    OnboardingStep.WELCOME,
                    OnboardingStep.AUDIO_PERMISSION,
                    OnboardingStep.OVERLAY_PERMISSION,
                    OnboardingStep.ACCESSIBILITY_SERVICE
                )
            )
        ) {
            completedSteps.add(OnboardingStep.COMPLETE)
        }
        
        val canProceed = when (currentProgress.currentStep) {
            OnboardingStep.WELCOME -> true
            OnboardingStep.AUDIO_PERMISSION -> OnboardingStep.AUDIO_PERMISSION in completedSteps
            OnboardingStep.OVERLAY_PERMISSION -> OnboardingStep.OVERLAY_PERMISSION in completedSteps
            OnboardingStep.ACCESSIBILITY_SERVICE -> OnboardingStep.ACCESSIBILITY_SERVICE in completedSteps
            OnboardingStep.BATTERY_OPTIMIZATION -> OnboardingStep.BATTERY_OPTIMIZATION in completedSteps
            OnboardingStep.COMPLETE -> true
        }
        
        _onboardingProgress.value = currentProgress.copy(
            completedSteps = completedSteps,
            canProceed = canProceed
        )
    }
    
    fun navigateToStep(step: OnboardingStep) {
        val currentProgress = _onboardingProgress.value
        if (currentProgress.canNavigateToStep(step)) {
            _onboardingProgress.value = currentProgress.copy(currentStep = step)
        }
    }
    
    fun proceedToNextStep() {
        val currentProgress = _onboardingProgress.value
        val currentStepNumber = currentProgress.currentStep.stepNumber
        val nextStep = OnboardingStep.getStepByNumber(currentStepNumber + 1)
        
        if (nextStep != null && currentProgress.canProceed) {
            _onboardingProgress.value = currentProgress.copy(currentStep = nextStep)
        }
    }
    
    fun goToPreviousStep() {
        val currentProgress = _onboardingProgress.value
        val currentStepNumber = currentProgress.currentStep.stepNumber
        val previousStep = OnboardingStep.getStepByNumber(currentStepNumber - 1)
        
        if (previousStep != null) {
            _onboardingProgress.value = currentProgress.copy(currentStep = previousStep)
        }
    }
    
    fun requestAudioPermission() {
        viewModelScope.launch {
            try {
                permissionManager.requestAudioPermission()
            } catch (e: Exception) {
                _navigationEvent.emit(NavigationEvent.ShowError("Failed to request audio permission: ${e.message}"))
            }
        }
    }
    
    fun requestOverlayPermission() {
        viewModelScope.launch {
            try {
                permissionManager.requestOverlayPermission()
            } catch (e: Exception) {
                _navigationEvent.emit(NavigationEvent.ShowError("Failed to request overlay permission: ${e.message}"))
            }
        }
    }
    
    fun requestAccessibilityPermission() {
        viewModelScope.launch {
            try {
                permissionManager.requestAccessibilityPermission()
            } catch (e: Exception) {
                _navigationEvent.emit(NavigationEvent.ShowError("Failed to request accessibility permission: ${e.message}"))
            }
        }
    }
    
    fun openAppSettings() {
        viewModelScope.launch {
            try {
                permissionManager.openAppSettings()
            } catch (e: Exception) {
                _navigationEvent.emit(NavigationEvent.ShowError("Failed to open app settings: ${e.message}"))
            }
        }
    }
    
    fun skipCurrentStep() {
        val currentProgress = _onboardingProgress.value
        when (currentProgress.currentStep) {
            OnboardingStep.OVERLAY_PERMISSION -> {
                markStepSkipped(OnboardingStep.OVERLAY_PERMISSION)
                proceedToNextStep()
            }
            OnboardingStep.ACCESSIBILITY_SERVICE -> {
                markStepSkipped(OnboardingStep.ACCESSIBILITY_SERVICE)
                proceedToNextStep()
            }
            else -> {
                // Audio permission cannot be skipped as it's required
                viewModelScope.launch {
                    _navigationEvent.emit(NavigationEvent.ShowError("This permission is required and cannot be skipped"))
                }
            }
        }
    }
    
    private fun markStepSkipped(step: OnboardingStep) {
        val currentProgress = _onboardingProgress.value
        val completedSteps = currentProgress.completedSteps.toMutableSet()
        completedSteps.add(step)
        
        _onboardingProgress.value = currentProgress.copy(
            completedSteps = completedSteps,
            canProceed = true
        )
    }
    
    fun refreshPermissions() {
        permissionManager.refreshAllPermissionStates()
    }
    
    fun completeOnboarding() {
        viewModelScope.launch {
            _navigationEvent.emit(NavigationEvent.CompleteOnboarding)
        }
    }
    
    fun showPermissionRationale(permission: String) {
        _showRationale.value = permission
    }
    
    fun hidePermissionRationale() {
        _showRationale.value = null
    }
    
    fun getPermissionDenialReason(permission: String): OnboardingPermissionManager.PermissionDenialReason {
        return permissionManager.getPermissionDenialReason(permission)
    }
    
    sealed class NavigationEvent {
        object CompleteOnboarding : NavigationEvent()
        data class ShowError(val message: String) : NavigationEvent()
    }
}