# UI Components Guide

This document provides comprehensive documentation for all UI components and screens in the WhisperTop application, built with Jetpack Compose and following Material Design 3 principles.

## Table of Contents

- [Core Components](#core-components)
- [Screen Components](#screen-components)  
- [Navigation](#navigation)
- [Component Architecture](#component-architecture)
- [Usage Guidelines](#usage-guidelines)
- [Testing Components](#testing-components)

---

## Core Components

### StatisticsPreferencesSection

**Location:** `composeApp/src/commonMain/kotlin/me/shadykhalifa/whispertop/presentation/ui/components/StatisticsPreferencesSection.kt`

**Purpose:** Main section for user statistics, dashboard configuration, export/import functionality, notifications, and privacy preferences.

**Key Features:**
- Data collection toggle with retention settings
- Export/import functionality with format selection
- Dashboard metrics configuration
- Notification preferences
- Privacy controls and data management

**Usage Example:**
```kotlin
@Composable
fun SettingsContent() {
    LazyColumn {
        item {
            StatisticsPreferencesSection(
                onExportData = { format -> /* Handle export */ },
                onImportData = { uri -> /* Handle import */ },
                onPrivacySettingsChanged = { settings -> /* Update privacy */ }
            )
        }
    }
}
```

### BottomNavigationComponent

**Location:** `composeApp/src/androidMain/kotlin/me/shadykhalifa/whispertop/presentation/ui/components/BottomNavigationComponent.kt`

**Purpose:** Bottom navigation bar for main app screens with Material Design 3 styling.

**Navigation Items:**
- Home: Main dashboard and quick actions
- History: Transcription history and management
- Settings: App configuration and preferences

**Usage Example:**
```kotlin
@Composable
fun MainAppContent(navController: NavHostController) {
    Scaffold(
        bottomBar = {
            BottomNavigationComponent(
                selectedScreen = currentScreen,
                onScreenSelected = { screen ->
                    navController.navigate(screen.route)
                }
            )
        }
    ) {
        // Screen content
    }
}
```

### StateComponents

**Location:** `composeApp/src/androidMain/kotlin/me/shadykhalifa/whispertop/presentation/ui/components/StateComponents.kt`

**Purpose:** Standardized UI components for displaying various app states with consistent styling.

**Available States:**
- Loading: Progress indicators with optional text
- Error: Error messages with retry actions
- Empty: Empty state illustrations with actions
- Success: Confirmation states with next steps

**Usage Example:**
```kotlin
@Composable
fun TranscriptionsList(uiState: TranscriptionsUiState) {
    when (uiState) {
        is TranscriptionsUiState.Loading -> LoadingState()
        is TranscriptionsUiState.Error -> ErrorState(
            message = uiState.message,
            onRetry = { /* Retry logic */ }
        )
        is TranscriptionsUiState.Empty -> EmptyState(
            title = "No transcriptions yet",
            description = "Start recording to see your transcriptions here",
            action = "Start Recording"
        )
        is TranscriptionsUiState.Success -> TranscriptionsList(uiState.data)
    }
}
```

### ShimmerComponents

**Location:** `composeApp/src/androidMain/kotlin/me/shadykhalifa/whispertop/presentation/ui/components/ShimmerComponents.kt`

**Purpose:** Shimmer loading placeholders that maintain layout structure during data loading.

**Available Shimmers:**
- Text shimmer: For text content placeholders
- Card shimmer: For card-based content
- List shimmer: For list item placeholders
- Chart shimmer: For data visualization placeholders

**Usage Example:**
```kotlin
@Composable
fun DashboardContent(isLoading: Boolean, data: DashboardData?) {
    if (isLoading) {
        Column {
            repeat(3) {
                ShimmerCard()
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    } else {
        // Actual content
    }
}
```

### TrendChartComponent

**Location:** `composeApp/src/androidMain/kotlin/me/shadykhalifa/whispertop/presentation/ui/components/TrendChartComponent.kt`

**Purpose:** Interactive chart component for visualizing usage trends and statistics.

**Chart Types:**
- Line charts: For trend visualization
- Bar charts: For comparison metrics  
- Pie charts: For distribution data
- Time series: For temporal data

**Usage Example:**
```kotlin
@Composable
fun UsageAnalytics(data: List<UsageData>) {
    TrendChartComponent(
        data = data,
        chartType = ChartType.Line,
        timeRange = TimeRange.Week,
        onDataPointClick = { point -> /* Handle selection */ }
    )
}
```

### LanguagePreferenceSection

**Location:** `composeApp/src/commonMain/kotlin/me/shadykhalifa/whispertop/presentation/ui/components/LanguagePreferenceSection.kt`

**Purpose:** Language selection and localization preferences with support for Whisper's multilingual capabilities.

**Features:**
- Primary language selection
- Secondary language support
- Auto-detection preferences
- Region-specific settings

---

## Screen Components

### AndroidHomeScreen

**Location:** `composeApp/src/androidMain/kotlin/me/shadykhalifa/whispertop/presentation/ui/screens/AndroidHomeScreen.kt`

**Purpose:** Main home screen providing quick access to core functionality and status overview.

**Key Elements:**
- Floating action button for quick recording
- Status indicators for services
- Recent transcriptions preview
- Quick settings access

### DashboardScreen

**Location:** `composeApp/src/androidMain/kotlin/me/shadykhalifa/whispertop/presentation/ui/screens/DashboardScreen.kt`

**Purpose:** Analytics and statistics dashboard with comprehensive usage insights.

**Features:**
- Usage statistics and trends
- Performance metrics
- Data export/import tools
- Privacy dashboard integration

### SettingsScreen

**Location:** `composeApp/src/commonMain/kotlin/me/shadykhalifa/whispertop/presentation/ui/screens/SettingsScreen.kt`

**Purpose:** Comprehensive settings screen for app configuration and customization.

**Settings Categories:**
- API Configuration (OpenAI key, model selection)
- Audio Settings (quality, formats)
- UI Preferences (theme, language)
- Privacy & Security settings
- Advanced options

### HistoryScreen

**Location:** `composeApp/src/androidMain/kotlin/me/shadykhalifa/whispertop/presentation/ui/screens/HistoryScreen.kt`

**Purpose:** Complete transcription history with search, filter, and management capabilities.

**Features:**
- Chronological transcription list
- Search and filter functionality
- Bulk operations (delete, export)
- Detailed view navigation

### OnboardingScreen

**Location:** `composeApp/src/androidMain/kotlin/me/shadykhalifa/whispertop/presentation/ui/screens/OnboardingScreen.kt`

**Purpose:** First-time user experience with setup guidance and feature introduction.

**Onboarding Flow:**
1. Welcome and app introduction
2. Permissions setup (microphone, overlay, accessibility)
3. API key configuration
4. Feature walkthrough
5. Typing test for baseline WPM

### PermissionsDashboardScreen

**Location:** `composeApp/src/androidMain/kotlin/me/shadykhalifa/whispertop/presentation/ui/screens/PermissionsDashboardScreen.kt`

**Purpose:** Centralized permissions management with status indicators and troubleshooting.

**Permission Categories:**
- Essential permissions (microphone, overlay)
- Optional permissions (storage, notifications)
- System permissions (accessibility service)
- Permission troubleshooting guide

---

## Navigation

### MainNavGraph

**Location:** `composeApp/src/androidMain/kotlin/me/shadykhalifa/whispertop/presentation/navigation/MainNavGraph.kt`

**Purpose:** Navigation graph defining screen relationships and transitions.

**Navigation Structure:**
```
Home (default)
├── Dashboard
├── History
│   └── TranscriptionDetail
├── Settings
│   ├── LanguagePreferences  
│   ├── PrivacyDashboard
│   └── DataExport
└── Onboarding (first-run only)
    └── OnboardingWpm
```

**Navigation Features:**
- Type-safe navigation with arguments
- Shared element transitions
- Deep linking support
- Back stack management

---

## Component Architecture

### Design Principles

1. **Single Responsibility**: Each component has one clear purpose
2. **Composition over Inheritance**: Components are built through composition
3. **State Hoisting**: State management follows Compose best practices
4. **Accessibility First**: All components support accessibility features

### Component Structure

```kotlin
@Composable
fun ComponentName(
    // Data parameters
    data: DataClass,
    
    // State parameters  
    isLoading: Boolean = false,
    
    // Callback parameters
    onAction: (ActionType) -> Unit,
    
    // Modifier for customization
    modifier: Modifier = Modifier
) {
    // Component implementation
}
```

### State Management

Components follow the unidirectional data flow pattern:

```kotlin
// ViewModel provides state
@Composable 
fun Screen(viewModel: ScreenViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    
    ScreenContent(
        uiState = uiState,
        onUserAction = viewModel::handleAction
    )
}

// Component receives state and events
@Composable
fun ScreenContent(
    uiState: ScreenUiState,
    onUserAction: (UserAction) -> Unit
) {
    // UI implementation
}
```

---

## Usage Guidelines

### Component Selection

1. **State Components**: Use for loading, error, and empty states
2. **Shimmer Components**: Use during data loading to maintain layout
3. **Custom Components**: Use for app-specific functionality

### Accessibility Guidelines

- All components support semantic properties
- Interactive elements have proper content descriptions
- Focus management follows accessibility guidelines
- High contrast and large text support

### Performance Considerations

- Components use `remember` for expensive calculations
- List components use `LazyColumn`/`LazyRow` for large datasets
- Images use `AsyncImage` with proper loading states

---

## Testing Components

### Unit Testing

```kotlin
@Test
fun testComponentRendering() {
    composeTestRule.setContent {
        ComponentName(
            data = testData,
            onAction = mockAction
        )
    }
    
    composeTestRule
        .onNodeWithText("Expected Text")
        .assertIsDisplayed()
}
```

### Integration Testing

```kotlin
@Test
fun testUserInteraction() {
    composeTestRule.setContent {
        ComponentName(onAction = mockAction)
    }
    
    composeTestRule
        .onNodeWithContentDescription("Action Button")
        .performClick()
        
    verify { mockAction.invoke(any()) }
}
```

### Accessibility Testing

```kotlin
@Test
fun testAccessibility() {
    composeTestRule.setContent {
        ComponentName()
    }
    
    // Test semantic properties
    composeTestRule
        .onNodeWithContentDescription("Button description")
        .assertHasClickAction()
}
```

---

## Best Practices

1. **Component Reusability**: Design components to be reusable across screens
2. **Consistent Styling**: Use Material Design 3 theming throughout
3. **Error Handling**: All components handle error states gracefully
4. **Loading States**: Provide appropriate loading indicators
5. **Responsive Design**: Components adapt to different screen sizes
6. **Dark Mode Support**: All components support light/dark themes

---

For implementation details and advanced usage patterns, refer to the individual component files and their associated tests.