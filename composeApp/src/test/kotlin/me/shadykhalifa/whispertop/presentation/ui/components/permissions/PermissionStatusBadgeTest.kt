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

@RunWith(AndroidJUnit4::class)
class PermissionStatusBadgeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun permissionStatusBadge_showsCorrectContentDescription_forGrantedPermission() {
        val permissionState = PermissionState(
            permission = AppPermission.RECORD_AUDIO,
            isGranted = true
        )

        composeTestRule.setContent {
            WhisperTopTheme {
                PermissionStatusBadge(permissionState = permissionState)
            }
        }

        composeTestRule.onNodeWithContentDescription("Permission granted")
            .assertIsDisplayed()
    }

    @Test
    fun permissionStatusBadge_showsCorrectContentDescription_forDeniedPermission() {
        val permissionState = PermissionState(
            permission = AppPermission.RECORD_AUDIO,
            isGranted = false
        )

        composeTestRule.setContent {
            WhisperTopTheme {
                PermissionStatusBadge(permissionState = permissionState)
            }
        }

        composeTestRule.onNodeWithContentDescription("Permission not granted")
            .assertIsDisplayed()
    }

    @Test
    fun permissionStatusBadge_showsCorrectContentDescription_forPermanentlyDeniedPermission() {
        val permissionState = PermissionState(
            permission = AppPermission.RECORD_AUDIO,
            isGranted = false,
            isPermanentlyDenied = true
        )

        composeTestRule.setContent {
            WhisperTopTheme {
                PermissionStatusBadge(permissionState = permissionState)
            }
        }

        composeTestRule.onNodeWithContentDescription("Permission denied, requires settings")
            .assertIsDisplayed()
    }

    @Test
    fun permissionStatusBadge_showsCorrectContentDescription_forRationaleRequired() {
        val permissionState = PermissionState(
            permission = AppPermission.RECORD_AUDIO,
            isGranted = false,
            canShowRationale = true,
            isPermanentlyDenied = false
        )

        composeTestRule.setContent {
            WhisperTopTheme {
                PermissionStatusBadge(permissionState = permissionState)
            }
        }

        composeTestRule.onNodeWithContentDescription("Permission needs explanation")
            .assertIsDisplayed()
    }

    @Test
    fun permissionStatusBadge_showsCorrectContentDescription_forCooldownPeriod() {
        val permissionState = PermissionState(
            permission = AppPermission.RECORD_AUDIO,
            isGranted = false,
            denialCount = 2,
            lastDeniedTimestamp = System.currentTimeMillis() + 10000 // 10 seconds in future
        )

        composeTestRule.setContent {
            WhisperTopTheme {
                PermissionStatusBadge(permissionState = permissionState)
            }
        }

        composeTestRule.onNodeWithContentDescription("Permission request on cooldown")
            .assertIsDisplayed()
    }
}