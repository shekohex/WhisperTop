package me.shadykhalifa.whispertop.presentation.models

sealed class PermissionState {
    object NotRequested : PermissionState()
    object Granted : PermissionState()
    object Denied : PermissionState()
    object PermanentlyDenied : PermissionState()
}

data class IndividualPermissionState(
    val permission: String,
    val state: PermissionState,
    val isRequired: Boolean = true
)

data class OnboardingPermissionState(
    val audioRecording: IndividualPermissionState = IndividualPermissionState(
        android.Manifest.permission.RECORD_AUDIO,
        PermissionState.NotRequested
    ),
    val overlay: IndividualPermissionState = IndividualPermissionState(
        android.Manifest.permission.SYSTEM_ALERT_WINDOW,
        PermissionState.NotRequested
    ),
    val accessibility: IndividualPermissionState = IndividualPermissionState(
        "accessibility_service",
        PermissionState.NotRequested
    ),
    val notifications: IndividualPermissionState = IndividualPermissionState(
        android.Manifest.permission.POST_NOTIFICATIONS,
        PermissionState.NotRequested,
        isRequired = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
    ),
    val foregroundService: IndividualPermissionState = IndividualPermissionState(
        android.Manifest.permission.FOREGROUND_SERVICE,
        PermissionState.NotRequested
    ),
    val foregroundServiceMicrophone: IndividualPermissionState = IndividualPermissionState(
        android.Manifest.permission.FOREGROUND_SERVICE_MICROPHONE,
        PermissionState.NotRequested,
        isRequired = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE
    )
) {
    val allCriticalPermissionsGranted: Boolean
        get() = audioRecording.state == PermissionState.Granted &&
                overlay.state == PermissionState.Granted &&
                accessibility.state == PermissionState.Granted &&
                (!notifications.isRequired || notifications.state == PermissionState.Granted) &&
                foregroundService.state == PermissionState.Granted &&
                (!foregroundServiceMicrophone.isRequired || foregroundServiceMicrophone.state == PermissionState.Granted)

    val hasAnyDeniedPermissions: Boolean
        get() = listOf(audioRecording, overlay, accessibility, notifications, foregroundService, foregroundServiceMicrophone)
            .any { it.isRequired && it.state == PermissionState.Denied }

    val hasPermanentlyDeniedPermissions: Boolean
        get() = listOf(audioRecording, overlay, accessibility, notifications, foregroundService, foregroundServiceMicrophone)
            .any { it.isRequired && it.state == PermissionState.PermanentlyDenied }
}