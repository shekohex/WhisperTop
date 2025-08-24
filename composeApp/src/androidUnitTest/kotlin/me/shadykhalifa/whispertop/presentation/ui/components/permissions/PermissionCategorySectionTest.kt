package me.shadykhalifa.whispertop.presentation.ui.components.permissions

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import me.shadykhalifa.whispertop.domain.models.AppPermission
import me.shadykhalifa.whispertop.domain.models.PermissionState
import me.shadykhalifa.whispertop.presentation.ui.theme.WhisperTopTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
class PermissionCategorySectionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun categorizePermissions_groupsPermissionsCorrectly() {
        val permissions = mapOf(
            AppPermission.RECORD_AUDIO to PermissionState(
                permission = AppPermission.RECORD_AUDIO,
                isGranted = true
            ),
            AppPermission.SYSTEM_ALERT_WINDOW to PermissionState(
                permission = AppPermission.SYSTEM_ALERT_WINDOW,
                isGranted = false
            ),
            AppPermission.ACCESSIBILITY_SERVICE to PermissionState(
                permission = AppPermission.ACCESSIBILITY_SERVICE,
                isGranted = false
            )
        )

        val categorized = categorizePermissions(permissions)

        assertEquals(3, categorized.size)
        
        val audioCategory = categorized[PermissionCategory.Audio]
        assertEquals(1, audioCategory?.size)
        assertEquals(AppPermission.RECORD_AUDIO, audioCategory?.first()?.first)
        
        val systemCategory = categorized[PermissionCategory.System]
        assertEquals(1, systemCategory?.size)
        assertEquals(AppPermission.SYSTEM_ALERT_WINDOW, systemCategory?.first()?.first)
        
        val accessibilityCategory = categorized[PermissionCategory.Accessibility]
        assertEquals(1, accessibilityCategory?.size)
        assertEquals(AppPermission.ACCESSIBILITY_SERVICE, accessibilityCategory?.first()?.first)
    }

    @Test
    fun permissionCategorySection_displaysCorrectGrantedCount() {
        val permissions = listOf(
            AppPermission.RECORD_AUDIO to PermissionState(
                permission = AppPermission.RECORD_AUDIO,
                isGranted = true
            ),
            AppPermission.SYSTEM_ALERT_WINDOW to PermissionState(
                permission = AppPermission.SYSTEM_ALERT_WINDOW,
                isGranted = false
            )
        )
        val onRequestPermission = mock<(AppPermission) -> Unit>()
        val onOpenSettings = mock<(AppPermission) -> Unit>()

        composeTestRule.setContent {
            WhisperTopTheme {
                PermissionCategorySection(
                    category = PermissionCategory.System,
                    permissions = permissions,
                    onRequestPermission = onRequestPermission,
                    onOpenSettings = onOpenSettings
                )
            }
        }

        composeTestRule.onNodeWithText("System").assertIsDisplayed()
        composeTestRule.onNodeWithText("1 of 2 granted").assertIsDisplayed()
    }

    @Test
    fun permissionCategorySection_expandsAndCollapses() {
        val permissions = listOf(
            AppPermission.RECORD_AUDIO to PermissionState(
                permission = AppPermission.RECORD_AUDIO,
                isGranted = false
            )
        )
        val onRequestPermission = mock<(AppPermission) -> Unit>()
        val onOpenSettings = mock<(AppPermission) -> Unit>()

        composeTestRule.setContent {
            WhisperTopTheme {
                PermissionCategorySection(
                    category = PermissionCategory.Audio,
                    permissions = permissions,
                    onRequestPermission = onRequestPermission,
                    onOpenSettings = onOpenSettings,
                    initiallyExpanded = false
                )
            }
        }

        // Initially collapsed - permission card should not be visible
        composeTestRule.onNodeWithText("Record Audio").assertDoesNotExist()

        // Click to expand
        composeTestRule.onNodeWithText("Audio").performClick()
        
        // Now permission card should be visible
        composeTestRule.onNodeWithText("Record Audio").assertIsDisplayed()

        // Click to collapse again
        composeTestRule.onNodeWithText("Audio").performClick()
        
        // Permission card should be hidden again
        composeTestRule.onNodeWithText("Record Audio").assertDoesNotExist()
    }

    @Test
    fun permissionCategorySection_showsCorrectContentDescription() {
        val permissions = listOf<Pair<AppPermission, PermissionState>>()
        val onRequestPermission = mock<(AppPermission) -> Unit>()
        val onOpenSettings = mock<(AppPermission) -> Unit>()

        composeTestRule.setContent {
            WhisperTopTheme {
                PermissionCategorySection(
                    category = PermissionCategory.Audio,
                    permissions = permissions,
                    onRequestPermission = onRequestPermission,
                    onOpenSettings = onOpenSettings,
                    initiallyExpanded = true
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Collapse Audio section").assertIsDisplayed()
    }
}