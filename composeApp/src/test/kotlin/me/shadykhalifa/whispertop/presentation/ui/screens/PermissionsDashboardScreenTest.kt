package me.shadykhalifa.whispertop.presentation.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.flowOf
import me.shadykhalifa.whispertop.domain.models.AppPermission
import me.shadykhalifa.whispertop.domain.models.PermissionState
import me.shadykhalifa.whispertop.presentation.ui.theme.WhisperTopTheme
import me.shadykhalifa.whispertop.presentation.viewmodels.PermissionsViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class PermissionsDashboardScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun permissionsDashboardScreen_displaysTitle() {
        val mockViewModel = mock<PermissionsViewModel>()
        whenever(mockViewModel.permissionStates).thenReturn(flowOf(emptyMap()))
        
        // Mock the StateFlow by creating a mock that returns a value
        val mockUiState = mock<kotlinx.coroutines.flow.StateFlow<Any>>()
        whenever(mockViewModel.uiState).thenReturn(mockUiState)
        whenever(mockUiState.value).thenReturn(mock())

        composeTestRule.setContent {
            WhisperTopTheme {
                PermissionsDashboardScreen(
                    onNavigateBack = {},
                    viewModel = mockViewModel
                )
            }
        }

        composeTestRule.onNodeWithText("Permissions Dashboard").assertIsDisplayed()
    }

    @Test
    fun permissionsDashboardScreen_displaysPermissionCount_correctly() {
        val mockViewModel = mock<PermissionsViewModel>()
        val permissions = mapOf(
            AppPermission.RECORD_AUDIO to PermissionState(
                permission = AppPermission.RECORD_AUDIO,
                isGranted = true
            ),
            AppPermission.SYSTEM_ALERT_WINDOW to PermissionState(
                permission = AppPermission.SYSTEM_ALERT_WINDOW,
                isGranted = false
            )
        )
        whenever(mockViewModel.permissionStates).thenReturn(flowOf(permissions))
        
        val mockUiState = mock<kotlinx.coroutines.flow.StateFlow<Any>>()
        whenever(mockViewModel.uiState).thenReturn(mockUiState)
        whenever(mockUiState.value).thenReturn(mock())

        composeTestRule.setContent {
            WhisperTopTheme {
                PermissionsDashboardScreen(
                    onNavigateBack = {},
                    viewModel = mockViewModel
                )
            }
        }

        composeTestRule.onNodeWithText("1 of 2 granted").assertIsDisplayed()
    }

    @Test
    fun permissionsDashboardScreen_callsNavigateBack_whenBackButtonPressed() {
        val mockViewModel = mock<PermissionsViewModel>()
        whenever(mockViewModel.permissionStates).thenReturn(flowOf(emptyMap()))
        
        val mockUiState = mock<kotlinx.coroutines.flow.StateFlow<Any>>()
        whenever(mockViewModel.uiState).thenReturn(mockUiState)
        whenever(mockUiState.value).thenReturn(mock())

        val onNavigateBack = mock<() -> Unit>()

        composeTestRule.setContent {
            WhisperTopTheme {
                PermissionsDashboardScreen(
                    onNavigateBack = onNavigateBack,
                    viewModel = mockViewModel
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Navigate back").performClick()
        verify(onNavigateBack).invoke()
    }
}