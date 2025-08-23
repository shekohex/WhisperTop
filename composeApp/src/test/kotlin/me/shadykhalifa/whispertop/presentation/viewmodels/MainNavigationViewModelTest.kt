package me.shadykhalifa.whispertop.presentation.viewmodels

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import me.shadykhalifa.whispertop.presentation.navigation.NavigationTab
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class MainNavigationViewModelTest {

    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var viewModel: MainNavigationViewModel

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        savedStateHandle = SavedStateHandle()
        viewModel = MainNavigationViewModel(savedStateHandle)
    }

    @Test
    fun `initial state should have home tab selected by default`() = testScope.runTest {
        val state = viewModel.uiState.value
        
        assertEquals(NavigationTab.Home, state.selectedTab)
        assertEquals(false, state.canNavigateBack)
        assertEquals(0, state.backStackCount)
    }

    @Test
    fun `selectTab should update selected tab and save to SavedStateHandle`() = testScope.runTest {
        viewModel.selectTab(NavigationTab.Settings)
        
        val state = viewModel.uiState.value
        assertEquals(NavigationTab.Settings, state.selectedTab)
        
        // Verify saved state
        val savedTab = savedStateHandle.get<String>("selected_tab")
        assertEquals("settings", savedTab)
    }

    @Test
    fun `selectTab should work for all navigation tabs`() = testScope.runTest {
        NavigationTab.bottomNavigationTabs.forEach { tab ->
            viewModel.selectTab(tab)
            
            val state = viewModel.uiState.value
            assertEquals(tab, state.selectedTab)
            
            val savedTab = savedStateHandle.get<String>("selected_tab")
            assertEquals(tab.route, savedTab)
        }
    }

    @Test
    fun `updateBackStackState should update navigation state correctly`() = testScope.runTest {
        viewModel.updateBackStackState(canNavigateBack = true, backStackCount = 3)
        
        val state = viewModel.uiState.value
        assertTrue(state.canNavigateBack)
        assertEquals(3, state.backStackCount)
    }

    @Test
    fun `handleDeepLink should return correct tab for valid routes`() = testScope.runTest {
        val homeTab = viewModel.handleDeepLink("home")
        assertEquals(NavigationTab.Home, homeTab)
        
        val historyTab = viewModel.handleDeepLink("history")
        assertEquals(NavigationTab.History, historyTab)
        
        val settingsTab = viewModel.handleDeepLink("settings")
        assertEquals(NavigationTab.Settings, settingsTab)
        
        val permissionsTab = viewModel.handleDeepLink("permissions")
        assertEquals(NavigationTab.Permissions, permissionsTab)
    }

    @Test
    fun `handleDeepLink should return null for invalid routes`() = testScope.runTest {
        val result = viewModel.handleDeepLink("invalid_route")
        assertNull(result)
    }

    @Test
    fun `handleDeepLink should select the tab for valid routes`() = testScope.runTest {
        viewModel.handleDeepLink("history")
        
        val state = viewModel.uiState.value
        assertEquals(NavigationTab.History, state.selectedTab)
        
        val savedTab = savedStateHandle.get<String>("selected_tab")
        assertEquals("history", savedTab)
    }

    @Test
    fun `shouldShowBottomNavigation should return true`() = testScope.runTest {
        assertTrue(viewModel.shouldShowBottomNavigation())
    }

    @Test
    fun `state restoration from SavedStateHandle should work`() = testScope.runTest {
        // Setup saved state
        savedStateHandle["selected_tab"] = "settings"
        
        // Create new ViewModel instance (simulating recreation)
        val newViewModel = MainNavigationViewModel(savedStateHandle)
        
        val state = newViewModel.uiState.value
        assertEquals(NavigationTab.Settings, state.selectedTab)
    }

    @Test
    fun `state restoration with invalid saved tab should fallback to home`() = testScope.runTest {
        // Setup invalid saved state
        savedStateHandle["selected_tab"] = "invalid_tab"
        
        // Create new ViewModel instance
        val newViewModel = MainNavigationViewModel(savedStateHandle)
        
        val state = newViewModel.uiState.value
        assertEquals(NavigationTab.Home, state.selectedTab)
    }

    @Test
    fun `state restoration with null saved tab should fallback to home`() = testScope.runTest {
        // No saved state (null)
        val newViewModel = MainNavigationViewModel(SavedStateHandle())
        
        val state = newViewModel.uiState.value
        assertEquals(NavigationTab.Home, state.selectedTab)
    }
}