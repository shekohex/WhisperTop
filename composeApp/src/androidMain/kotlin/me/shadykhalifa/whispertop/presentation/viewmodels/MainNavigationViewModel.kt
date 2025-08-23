package me.shadykhalifa.whispertop.presentation.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.shadykhalifa.whispertop.presentation.navigation.NavigationTab

private const val SELECTED_TAB_KEY = "selected_tab"
private const val DEFAULT_TAB = "home"

data class MainNavigationUiState(
    val selectedTab: NavigationTab = NavigationTab.Home,
    val canNavigateBack: Boolean = false,
    val backStackCount: Int = 0
)

class MainNavigationViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MainNavigationUiState())
    val uiState: StateFlow<MainNavigationUiState> = _uiState.asStateFlow()
    
    init {
        val savedTab = savedStateHandle.get<String>(SELECTED_TAB_KEY) ?: DEFAULT_TAB
        val navigationTab = NavigationTab.fromRoute(savedTab) ?: NavigationTab.Home
        _uiState.value = _uiState.value.copy(selectedTab = navigationTab)
    }
    
    fun selectTab(tab: NavigationTab) {
        savedStateHandle[SELECTED_TAB_KEY] = tab.route
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }
    
    fun updateBackStackState(canNavigateBack: Boolean, backStackCount: Int) {
        _uiState.value = _uiState.value.copy(
            canNavigateBack = canNavigateBack,
            backStackCount = backStackCount
        )
    }
    
    fun handleDeepLink(route: String): NavigationTab? {
        return NavigationTab.fromRoute(route)?.also { tab ->
            selectTab(tab)
        }
    }
    
    fun shouldShowBottomNavigation(): Boolean {
        return true
    }
}