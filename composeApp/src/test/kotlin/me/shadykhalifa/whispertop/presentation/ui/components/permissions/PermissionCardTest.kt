package me.shadykhalifa.whispertop.presentation.ui.components.permissions

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import me.shadykhalifa.whispertop.domain.models.AppPermission
import me.shadykhalifa.whispertop.domain.models.PermissionState
import me.shadykhalifa.whispertop.presentation.ui.theme.WhisperTopTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class PermissionCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun permissionCard_displaysPermissionInfo_correctly() {
        val appPermission = AppPermission.RECORD_AUDIO
        val permissionState = PermissionState(
            permission = appPermission,
            isGranted = false,
            isPermanentlyDenied = false
        )
        val onRequestPermission = mock<(AppPermission) -> Unit>()
        val onOpenSettings = mock<(AppPermission) -> Unit>()

        composeTestRule.setContent {
            WhisperTopTheme {
                PermissionCard(
                    appPermission = appPermission,
                    permissionState = permissionState,
                    onRequestPermission = onRequestPermission,
                    onOpenSettings = onOpenSettings
                )
            }
        }

        composeTestRule.onNodeWithText("Record Audio").assertIsDisplayed()
        composeTestRule.onNodeWithText("Required to capture voice for transcription").assertIsDisplayed()
        composeTestRule.onNodeWithText("Required").assertIsDisplayed()
    }

    @Test
    fun permissionCard_showsGrantButton_whenNotGranted() {
        val appPermission = AppPermission.RECORD_AUDIO
        val permissionState = PermissionState(
            permission = appPermission,
            isGranted = false,
            isPermanentlyDenied = false
        )
        val onRequestPermission = mock<(AppPermission) -> Unit>()
        val onOpenSettings = mock<(AppPermission) -> Unit>()

        composeTestRule.setContent {
            WhisperTopTheme {
                PermissionCard(
                    appPermission = appPermission,
                    permissionState = permissionState,
                    onRequestPermission = onRequestPermission,
                    onOpenSettings = onOpenSettings
                )
            }
        }

        composeTestRule.onNodeWithText("Grant").assertIsDisplayed()
    }

    @Test
    fun permissionCard_showsSettingsButton_whenPermanentlyDenied() {
        val appPermission = AppPermission.SYSTEM_ALERT_WINDOW
        val permissionState = PermissionState(
            permission = appPermission,
            isGranted = false,
            isPermanentlyDenied = true
        )
        val onRequestPermission = mock<(AppPermission) -> Unit>()
        val onOpenSettings = mock<(AppPermission) -> Unit>()

        composeTestRule.setContent {
            WhisperTopTheme {
                PermissionCard(
                    appPermission = appPermission,
                    permissionState = permissionState,
                    onRequestPermission = onRequestPermission,
                    onOpenSettings = onOpenSettings
                )
            }
        }

        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun permissionCard_hidesActionButton_whenGranted() {
        val appPermission = AppPermission.ACCESSIBILITY_SERVICE
        val permissionState = PermissionState(
            permission = appPermission,
            isGranted = true
        )
        val onRequestPermission = mock<(AppPermission) -> Unit>()
        val onOpenSettings = mock<(AppPermission) -> Unit>()

        composeTestRule.setContent {
            WhisperTopTheme {
                PermissionCard(
                    appPermission = appPermission,
                    permissionState = permissionState,
                    onRequestPermission = onRequestPermission,
                    onOpenSettings = onOpenSettings
                )
            }
        }

        composeTestRule.onNodeWithText("Grant").assertDoesNotExist()
        composeTestRule.onNodeWithText("Settings").assertDoesNotExist()
    }

    @Test
    fun permissionCard_callsRequestPermission_whenGrantButtonClicked() {
        val appPermission = AppPermission.RECORD_AUDIO
        val permissionState = PermissionState(
            permission = appPermission,
            isGranted = false
        )
        val onRequestPermission = mock<(AppPermission) -> Unit>()
        val onOpenSettings = mock<(AppPermission) -> Unit>()

        composeTestRule.setContent {
            WhisperTopTheme {
                PermissionCard(
                    appPermission = appPermission,
                    permissionState = permissionState,
                    onRequestPermission = onRequestPermission,
                    onOpenSettings = onOpenSettings
                )
            }
        }

        composeTestRule.onNodeWithText("Grant").performClick()
        verify(onRequestPermission).invoke(appPermission)
    }

    @Test
    fun permissionCard_callsOpenSettings_whenSettingsButtonClicked() {
        val appPermission = AppPermission.SYSTEM_ALERT_WINDOW
        val permissionState = PermissionState(
            permission = appPermission,
            isGranted = false,
            isPermanentlyDenied = true
        )
        val onRequestPermission = mock<(AppPermission) -> Unit>()
        val onOpenSettings = mock<(AppPermission) -> Unit>()

        composeTestRule.setContent {
            WhisperTopTheme {
                PermissionCard(
                    appPermission = appPermission,
                    permissionState = permissionState,
                    onRequestPermission = onRequestPermission,
                    onOpenSettings = onOpenSettings
                )
            }
        }

        composeTestRule.onNodeWithText("Settings").performClick()
        verify(onOpenSettings).invoke(appPermission)
    }

    @Test
    fun permissionCard_showsCooldownMessage_whenOnCooldown() {
        val appPermission = AppPermission.RECORD_AUDIO
        val permissionState = PermissionState(
            permission = appPermission,
            isGranted = false,
            denialCount = 2,
            lastDeniedTimestamp = System.currentTimeMillis() + 10000 // 10 seconds in future
        )
        val onRequestPermission = mock<(AppPermission) -> Unit>()
        val onOpenSettings = mock<(AppPermission) -> Unit>()

        composeTestRule.setContent {
            WhisperTopTheme {
                PermissionCard(
                    appPermission = appPermission,
                    permissionState = permissionState,
                    onRequestPermission = onRequestPermission,
                    onOpenSettings = onOpenSettings
                )
            }
        }

        composeTestRule.onNodeWithText("Try again in").assertIsDisplayed()
    }
}