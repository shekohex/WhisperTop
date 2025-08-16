package me.shadykhalifa.whispertop.presentation.models

enum class OnboardingStep(
    val stepNumber: Int,
    val route: String,
    val title: String,
    val description: String,
    val icon: String // We'll use material icons instead
) {
    WELCOME(
        stepNumber = 1,
        route = "onboarding_welcome",
        title = "Welcome to WhisperTop",
        description = "Quick and accurate speech-to-text transcription anywhere on your device",
        icon = "waving_hand"
    ),
    AUDIO_PERMISSION(
        stepNumber = 2,
        route = "onboarding_audio",
        title = "Microphone Access",
        description = "Allow WhisperTop to access your microphone for speech recording",
        icon = "mic"
    ),
    OVERLAY_PERMISSION(
        stepNumber = 3,
        route = "onboarding_overlay",
        title = "Overlay Permission",
        description = "Enable floating microphone button that works on top of any app",
        icon = "picture_in_picture_alt"
    ),
    ACCESSIBILITY_SERVICE(
        stepNumber = 4,
        route = "onboarding_accessibility",
        title = "Text Insertion",
        description = "Allow automatic text insertion after transcription",
        icon = "accessibility_new"
    ),
    BATTERY_OPTIMIZATION(
        stepNumber = 5,
        route = "onboarding_battery",
        title = "Battery Optimization",
        description = "Ensure reliable background recording by exempting from battery optimization",
        icon = "battery_full"
    ),
    COMPLETE(
        stepNumber = 6,
        route = "onboarding_complete",
        title = "Setup Complete!",
        description = "You're all set! Start using WhisperTop for voice transcription.",
        icon = "check_circle"
    );

    companion object {
        fun fromRoute(route: String): OnboardingStep? {
            return values().find { it.route == route }
        }
        
        fun getStepByNumber(stepNumber: Int): OnboardingStep? {
            return values().find { it.stepNumber == stepNumber }
        }
        
        val totalSteps: Int get() = values().size
    }
}

data class OnboardingProgress(
    val currentStep: OnboardingStep = OnboardingStep.WELCOME,
    val completedSteps: Set<OnboardingStep> = emptySet(),
    val canProceed: Boolean = false
) {
    val progressPercentage: Float
        get() = (completedSteps.size.toFloat() / OnboardingStep.totalSteps) * 100f
        
    val isCompleted: Boolean
        get() = completedSteps.size == OnboardingStep.totalSteps
        
    fun isStepCompleted(step: OnboardingStep): Boolean = step in completedSteps
    
    fun canNavigateToStep(step: OnboardingStep): Boolean {
        return step.stepNumber <= currentStep.stepNumber + 1
    }
}