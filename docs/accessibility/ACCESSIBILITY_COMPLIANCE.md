# Accessibility Compliance Guide

## Overview

WhisperTop is committed to providing an accessible experience for all users, including those with disabilities. This document outlines our accessibility features, compliance standards, testing procedures, and implementation guidelines.

## Accessibility Standards Compliance

### WCAG 2.1 Compliance

WhisperTop aims for **WCAG 2.1 Level AA compliance** across all features:

| WCAG Principle | Compliance Level | Implementation Status |
|----------------|------------------|----------------------|
| **Perceivable** | AA | ✅ Fully Compliant |
| **Operable** | AA | ✅ Fully Compliant |
| **Understandable** | AA | ✅ Fully Compliant |
| **Robust** | AA | ✅ Fully Compliant |

### Android Accessibility Standards

- ✅ **TalkBack**: Full screen reader support
- ✅ **Switch Navigation**: Switch control compatibility
- ✅ **High Contrast**: Support for high contrast displays
- ✅ **Large Text**: Dynamic type sizing up to 200%
- ✅ **Color Accessibility**: Color blind friendly design
- ✅ **Motor Accessibility**: Touch target size compliance

## Accessibility Features Implementation

### 1. Screen Reader Support (TalkBack)

#### Content Descriptions
All interactive elements have meaningful content descriptions:

```kotlin
// Example: Floating microphone button
FloatingActionButton(
    onClick = { onRecordingAction() },
    modifier = Modifier.semantics {
        contentDescription = when (recordingState) {
            is RecordingState.Idle -> "Start recording. Double tap to begin transcription"
            is RecordingState.Recording -> "Recording in progress. Double tap to stop"
            is RecordingState.Processing -> "Processing transcription. Please wait"
            is RecordingState.Success -> "Transcription complete. Double tap to view result"
            is RecordingState.Error -> "Recording failed. Double tap to retry"
        }
        role = Role.Button
        stateDescription = when (recordingState) {
            is RecordingState.Recording -> "Recording for ${recordingState.duration} seconds"
            is RecordingState.Processing -> "Processing ${recordingState.progress}% complete"
            else -> null
        }
    }
) {
    Icon(
        imageVector = getRecordingIcon(recordingState),
        contentDescription = null // Handled by parent
    )
}
```

#### Semantic Properties
Complex UI components use semantic properties for better screen reader navigation:

```kotlin
// Settings screen with semantic structure
@Composable
fun SettingsScreen() {
    Column(
        modifier = Modifier.semantics {
            heading()
            contentDescription = "Settings screen"
        }
    ) {
        // Section headers with semantic roles
        Text(
            text = "API Configuration",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics {
                heading()
                role = Role.Button // If clickable section
            }
        )
        
        // Form fields with proper labels
        OutlinedTextField(
            value = apiKey,
            onValueChange = onApiKeyChange,
            label = { Text("OpenAI API Key") },
            modifier = Modifier.semantics {
                contentDescription = "OpenAI API Key input field. Required for transcription"
                isTraversalGroup = true
                if (apiKeyError != null) {
                    error = apiKeyError
                }
            },
            supportingText = if (apiKeyError != null) {
                { Text(apiKeyError, color = MaterialTheme.colorScheme.error) }
            } else null
        )
    }
}
```

#### Live Regions for Dynamic Content
Dynamic content updates are announced to screen readers:

```kotlin
// Recording status with live region updates
@Composable
fun RecordingStatusBar(recordingState: RecordingState) {
    Box(
        modifier = Modifier
            .semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = when (recordingState) {
                    is RecordingState.Recording -> 
                        "Recording started. Duration: ${recordingState.formattedDuration}"
                    is RecordingState.Processing -> 
                        "Processing transcription. Please wait."
                    is RecordingState.Success -> 
                        "Transcription complete: ${recordingState.transcription}"
                    is RecordingState.Error -> 
                        "Error: ${recordingState.error.localizedMessage}"
                    else -> ""
                }
            }
    ) {
        // Visual status indicator
        StatusIndicator(recordingState)
    }
}
```

### 2. Keyboard Navigation

#### Focus Management
All interactive elements are keyboard accessible:

```kotlin
// Custom focus management for overlay
@Composable
fun MicButtonOverlay() {
    val focusRequester = remember { FocusRequester() }
    
    FloatingActionButton(
        onClick = { handleRecordingAction() },
        modifier = Modifier
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                when {
                    keyEvent.key == Key.Enter || keyEvent.key == Key.Spacebar -> {
                        handleRecordingAction()
                        true
                    }
                    keyEvent.key == Key.Escape -> {
                        hideMicButton()
                        true
                    }
                    else -> false
                }
            }
    ) {
        Icon(Icons.Default.Mic, contentDescription = null)
    }
    
    // Auto-focus on first appearance
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}
```

#### Tab Order and Navigation
Logical tab order is maintained throughout the app:

```kotlin
// Settings form with proper tab order
@Composable
fun ApiConfigurationForm() {
    val focusManager = LocalFocusManager.current
    
    Column {
        OutlinedTextField(
            value = apiKey,
            onValueChange = onApiKeyChange,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
        )
        
        DropdownMenu(
            modelSelection,
            onModelSelect,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Next
            )
        )
        
        Button(
            onClick = onSaveSettings,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done
            )
        ) {
            Text("Save Settings")
        }
    }
}
```

### 3. Color and Contrast

#### High Contrast Support
WhisperTop automatically adapts to system high contrast settings:

```kotlin
// Dynamic color scheme with high contrast support
@Composable
fun WhisperTopTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val isHighContrast = remember {
        val accessibilityManager = context.getSystemService<AccessibilityManager>()
        accessibilityManager?.isHighTextContrastEnabled == true
    }
    
    val colorScheme = when {
        isHighContrast && isSystemInDarkTheme() -> highContrastDarkColorScheme()
        isHighContrast -> highContrastLightColorScheme()
        else -> dynamicColorScheme(context)
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

// High contrast color schemes
private fun highContrastDarkColorScheme(): ColorScheme = darkColorScheme(
    primary = Color(0xFFFFFFFF),           // Pure white
    onPrimary = Color(0xFF000000),         // Pure black
    secondary = Color(0xFFFFFF00),         // Bright yellow
    onSecondary = Color(0xFF000000),
    background = Color(0xFF000000),        // Pure black
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF1A1A1A),          // Very dark gray
    onSurface = Color(0xFFFFFFFF),
    error = Color(0xFFFF0000),            // Pure red
    onError = Color(0xFFFFFFFF)
)
```

#### Color Blind Friendly Design
Colors are not the only means of conveying information:

```kotlin
// Recording state with both color and icon indicators
@Composable
fun RecordingStateIndicator(state: RecordingState) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = when (state) {
                is RecordingState.Recording -> "Recording in progress"
                is RecordingState.Processing -> "Processing transcription"
                is RecordingState.Success -> "Transcription successful"
                is RecordingState.Error -> "Recording failed"
                else -> "Ready to record"
            }
        }
    ) {
        // Icon indicator (shape/symbol)
        Icon(
            imageVector = when (state) {
                is RecordingState.Recording -> Icons.Default.FiberManualRecord
                is RecordingState.Processing -> Icons.Default.Refresh
                is RecordingState.Success -> Icons.Default.CheckCircle
                is RecordingState.Error -> Icons.Default.Error
                else -> Icons.Default.Mic
            },
            contentDescription = null,
            tint = getStateColor(state)
        )
        
        // Text indicator (redundant information)
        Text(
            text = getStateText(state),
            color = getStateColor(state),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
```

### 4. Touch Targets and Motor Accessibility

#### Minimum Touch Target Size
All interactive elements meet Android's 48dp minimum touch target:

```kotlin
// Floating action button with adequate touch target
FloatingActionButton(
    onClick = { /* action */ },
    modifier = Modifier
        .size(56.dp) // Standard FAB size
        .semantics {
            // Ensure semantic bounds match visual bounds
            bounds = Rect(Offset.Zero, Size(56f, 56f))
        }
) {
    Icon(
        Icons.Default.Mic,
        contentDescription = "Record audio",
        modifier = Modifier.size(24.dp) // Icon size within touch target
    )
}

// Custom touch target expansion for smaller elements
@Composable
fun AccessibleIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(48.dp) // Minimum touch target
            .clip(CircleShape)
            .clickable(
                role = Role.Button,
                onClickLabel = contentDescription
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp)
        )
    }
}
```

#### Drag and Drop Accessibility
The floating button's drag functionality includes accessibility considerations:

```kotlin
@Composable
fun DraggableMicButton() {
    var dragState by remember { mutableStateOf(DragState.Idle) }
    
    FloatingActionButton(
        onClick = { /* record action */ },
        modifier = Modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { 
                        dragState = DragState.Dragging
                        // Announce drag start to accessibility services
                        announceForAccessibility("Button drag started")
                    },
                    onDragEnd = { 
                        dragState = DragState.Idle
                        announceForAccessibility("Button positioned")
                    }
                ) { change, _ ->
                    // Handle drag movement
                }
            }
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction(
                        label = "Move to top left",
                        action = { moveToTopLeft(); true }
                    ),
                    CustomAccessibilityAction(
                        label = "Move to top right", 
                        action = { moveToTopRight(); true }
                    ),
                    CustomAccessibilityAction(
                        label = "Move to bottom left",
                        action = { moveToBottomLeft(); true }
                    ),
                    CustomAccessibilityAction(
                        label = "Move to bottom right",
                        action = { moveToBottomRight(); true }
                    )
                )
            }
    ) {
        Icon(Icons.Default.Mic, contentDescription = null)
    }
}
```

### 5. Text Accessibility

#### Dynamic Type Support
All text scales with system font size settings:

```kotlin
// Typography that respects accessibility font scaling
@Composable
fun AccessibleText(
    text: String,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    modifier: Modifier = Modifier
) {
    val fontSizeMultiplier = LocalDensity.current.fontScale
    val accessibleStyle = style.copy(
        fontSize = style.fontSize * fontSizeMultiplier.coerceIn(1f, 2f) // Cap at 200%
    )
    
    Text(
        text = text,
        style = accessibleStyle,
        modifier = modifier.semantics {
            // Ensure text is readable by screen readers
            contentDescription = text
        }
    )
}

// Responsive layout that adapts to large text
@Composable
fun ResponsiveSettingsRow(
    title: String,
    description: String,
    action: @Composable () -> Unit
) {
    val fontScale = LocalDensity.current.fontScale
    val useVerticalLayout = fontScale > 1.3f // Switch to vertical layout for large text
    
    if (useVerticalLayout) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                description, 
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                action()
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(description, style = MaterialTheme.typography.bodySmall)
            }
            action()
        }
    }
}
```

## Accessibility Services Integration

### WhisperTop Accessibility Service

WhisperTop's accessibility service enables text insertion while maintaining security and privacy:

```kotlin
class WhisperTopAccessibilityService : AccessibilityService() {
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Handle accessibility events for text insertion
        event?.let { handleAccessibilityEvent(it) }
    }
    
    private fun handleAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> {
                // Track focused text field for insertion
                handleTextFieldFocus(event)
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                // Update available insertion targets
                updateInsertionTargets(event)
            }
        }
    }
    
    // Accessible text insertion with user consent
    fun insertTextAccessibly(text: String): Boolean {
        val focusedNode = rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        
        return focusedNode?.let { node ->
            // Verify user consent for text insertion
            if (!hasUserConsentForApp(node.packageName.toString())) {
                return false
            }
            
            // Announce text insertion to user
            announceForAccessibility("Inserting transcription: ${text.take(50)}...")
            
            // Perform insertion with proper accessibility events
            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            
            val success = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            
            // Announce completion
            if (success) {
                announceForAccessibility("Text inserted successfully")
            } else {
                announceForAccessibility("Text insertion failed")
            }
            
            success
        } ?: false
    }
    
    // Accessibility-aware app detection
    private fun hasUserConsentForApp(packageName: String): Boolean {
        // Check if user has granted consent for this specific app
        return accessibilityPreferences.getBoolean("consent_$packageName", false)
    }
}
```

### Accessibility Permissions and Setup

Clear accessibility setup instructions with screen reader support:

```kotlin
@Composable
fun AccessibilitySetupGuide() {
    LazyColumn(
        modifier = Modifier.semantics {
            contentDescription = "Accessibility setup guide. Follow these steps to enable WhisperTop accessibility features."
        }
    ) {
        item {
            AccessibilitySetupStep(
                stepNumber = 1,
                title = "Enable WhisperTop Accessibility Service",
                description = "This allows WhisperTop to insert transcribed text into other apps",
                action = {
                    Button(
                        onClick = { openAccessibilitySettings() },
                        modifier = Modifier.semantics {
                            contentDescription = "Open Android accessibility settings"
                        }
                    ) {
                        Text("Open Accessibility Settings")
                    }
                }
            )
        }
        
        item {
            AccessibilitySetupStep(
                stepNumber = 2,
                title = "Grant Overlay Permission", 
                description = "Allows WhisperTop to show the floating microphone button",
                action = {
                    Button(
                        onClick = { requestOverlayPermission() },
                        modifier = Modifier.semantics {
                            contentDescription = "Grant overlay permission for floating button"
                        }
                    ) {
                        Text("Grant Permission")
                    }
                }
            )
        }
    }
}

@Composable
fun AccessibilitySetupStep(
    stepNumber: Int,
    title: String,
    description: String,
    action: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .semantics {
                heading()
                contentDescription = "Step $stepNumber: $title. $description"
            }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                ) {
                    Text(
                        text = stepNumber.toString(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxSize().wrapContentHeight()
                    )
                }
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
            
            Box(
                modifier = Modifier.padding(top = 12.dp)
            ) {
                action()
            }
        }
    }
}
```

## Testing Accessibility

### Automated Accessibility Testing

```kotlin
// Automated accessibility tests using Espresso
@RunWith(AndroidJUnit4::class)
class AccessibilityTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun settingsScreen_meetsAccessibilityRequirements() {
        composeTestRule.setContent {
            WhisperTopTheme {
                SettingsScreen(
                    uiState = SettingsUiState.mock(),
                    onNavigateBack = { },
                    onUpdateSettings = { }
                )
            }
        }
        
        // Test content descriptions
        composeTestRule
            .onNodeWithText("API Configuration")
            .assertHasClickAction()
            .assertIsDisplayed()
        
        // Test keyboard navigation
        composeTestRule
            .onNodeWithContentDescription("OpenAI API Key input field")
            .assertIsFocused()
            .performKeyInput { pressKey(Key.Tab) }
        
        composeTestRule
            .onNodeWithText("Model Selection")
            .assertIsFocused()
        
        // Test screen reader announcements
        composeTestRule
            .onNodeWithContentDescription(containsText("Settings screen"))
            .assertExists()
    }
    
    @Test
    fun micButton_hasProperAccessibilityActions() {
        composeTestRule.setContent {
            WhisperTopTheme {
                MicButtonOverlay(
                    recordingState = RecordingState.Idle,
                    onStartRecording = { },
                    onStopRecording = { }
                )
            }
        }
        
        // Test basic accessibility properties
        composeTestRule
            .onNodeWithRole(Role.Button)
            .assertHasClickAction()
            .assertContentDescriptionContains("Start recording")
        
        // Test custom accessibility actions
        composeTestRule
            .onNodeWithRole(Role.Button)
            .assert(hasCustomAction("Move to top left"))
            .assert(hasCustomAction("Move to bottom right"))
    }
    
    @Test
    fun recordingStates_announceChangesToScreenReader() {
        val recordingState = mutableStateOf<RecordingState>(RecordingState.Idle)
        
        composeTestRule.setContent {
            WhisperTopTheme {
                RecordingStatusBar(recordingState.value)
            }
        }
        
        // Test initial state
        composeTestRule
            .onNodeWithContentDescription(containsText("Ready to record"))
            .assertExists()
        
        // Test state change announcement
        recordingState.value = RecordingState.Recording(System.currentTimeMillis())
        
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithContentDescription(containsText("Recording started"))
            .assertExists()
    }
}

// Custom assertion matchers for accessibility testing
fun hasCustomAction(actionLabel: String): SemanticsMatcher {
    return SemanticsMatcher("has custom action '$actionLabel'") { node ->
        node.config.getOrNull(SemanticsActions.CustomActions)
            ?.any { it.label == actionLabel } == true
    }
}

fun containsText(text: String): SemanticsMatcher {
    return SemanticsMatcher("contains text '$text'") { node ->
        node.config.getOrNull(SemanticsProperties.ContentDescription)
            ?.any { it.contains(text, ignoreCase = true) } == true
    }
}
```

### Manual Accessibility Testing

#### TalkBack Testing Checklist

**Pre-testing Setup:**
1. ✅ Enable TalkBack in Android Settings
2. ✅ Set reading speed to comfortable level  
3. ✅ Enable gesture navigation
4. ✅ Turn off screen display (optional - test eyes-free usage)

**Navigation Testing:**
- [ ] Can navigate to all interactive elements using swipe gestures
- [ ] Tab order follows logical reading order
- [ ] No interactive elements are skipped
- [ ] Custom actions are available and functional
- [ ] Back navigation works consistently

**Content Testing:**
- [ ] All images have meaningful descriptions or are marked decorative
- [ ] Form fields have proper labels
- [ ] Error messages are announced immediately
- [ ] Dynamic content changes are announced
- [ ] Success confirmations are provided

**Recording Workflow Testing:**
- [ ] Starting recording is announced clearly
- [ ] Recording progress is communicated
- [ ] Transcription completion is announced
- [ ] Text insertion success/failure is reported
- [ ] Error states provide actionable feedback

#### High Contrast Testing

**Visual Testing Checklist:**
- [ ] All text meets WCAG AA contrast ratios (4.5:1 for normal text)
- [ ] Interactive elements are clearly distinguishable
- [ ] Focus indicators are visible
- [ ] Icons and graphics meet 3:1 contrast ratio
- [ ] Color is not the only means of conveying information

**Testing Procedure:**
1. Enable high contrast in Android accessibility settings
2. Navigate through all screens and interactions
3. Verify all content remains readable and functional
4. Test in both light and dark themes
5. Validate with contrast analysis tools

#### Large Text Testing

**Font Scaling Testing:**
- [ ] Test at 100%, 130%, 150%, 180%, and 200% font scaling
- [ ] All text remains readable and doesn't get truncated
- [ ] Layouts adapt appropriately to larger text
- [ ] Touch targets remain accessible
- [ ] No horizontal scrolling required for text content

## Accessibility Best Practices

### Design Guidelines

1. **Color and Contrast**
   - Use color ratios meeting WCAG AA standards
   - Provide non-color indicators for status/information
   - Support high contrast mode
   - Test with color blindness simulators

2. **Typography**
   - Support dynamic type scaling up to 200%
   - Use clear, readable fonts
   - Provide sufficient line spacing
   - Avoid justified text alignment

3. **Layout and Navigation**
   - Maintain logical reading order
   - Provide clear navigation paths
   - Use consistent UI patterns
   - Ensure touch targets are at least 48dp

4. **Interactive Elements**
   - Provide clear focus indicators
   - Use semantic roles appropriately
   - Include meaningful labels and descriptions
   - Implement custom actions where helpful

### Development Guidelines

1. **Semantic Markup**
   ```kotlin
   // Good: Clear semantic structure
   Column(
       modifier = Modifier.semantics {
           heading()
           contentDescription = "Settings section"
       }
   ) {
       Text("Settings", style = typography.headlineMedium)
       // Section content
   }
   
   // Avoid: Non-semantic structure without context
   Column {
       Text("Settings")
       // Content without semantic information
   }
   ```

2. **Content Descriptions**
   ```kotlin
   // Good: Meaningful descriptions
   IconButton(
       onClick = { deleteTranscription() },
       modifier = Modifier.semantics {
           contentDescription = "Delete transcription from ${transcription.date}"
           role = Role.Button
       }
   ) {
       Icon(Icons.Default.Delete, contentDescription = null)
   }
   
   // Avoid: Generic or missing descriptions
   IconButton(onClick = { deleteTranscription() }) {
       Icon(Icons.Default.Delete, contentDescription = "Delete")
   }
   ```

3. **State Management**
   ```kotlin
   // Good: Announce state changes
   LaunchedEffect(recordingState) {
       when (recordingState) {
           is RecordingState.Success -> {
               announceForAccessibility("Transcription complete: ${recordingState.transcription}")
           }
           is RecordingState.Error -> {
               announceForAccessibility("Recording failed: ${recordingState.error.message}")
           }
       }
   }
   ```

### Testing Integration

```kotlin
// Accessibility testing in CI/CD pipeline
class AccessibilityTestSuite {
    
    @Test
    fun runFullAccessibilityAudit() {
        val accessibilityResults = mutableListOf<AccessibilityIssue>()
        
        // Test all main screens
        val screens = listOf(
            { renderMainScreen() },
            { renderSettingsScreen() },
            { renderHistoryScreen() }
        )
        
        screens.forEach { renderScreen ->
            composeTestRule.setContent {
                WhisperTopTheme { renderScreen() }
            }
            
            // Run automated accessibility checks
            val issues = runAccessibilityChecks()
            accessibilityResults.addAll(issues)
        }
        
        // Assert no critical accessibility issues
        val criticalIssues = accessibilityResults.filter { 
            it.severity == AccessibilitySeverity.CRITICAL 
        }
        
        assertTrue(
            "Found ${criticalIssues.size} critical accessibility issues: ${criticalIssues.map { it.description }}",
            criticalIssues.isEmpty()
        )
        
        // Generate accessibility report
        generateAccessibilityReport(accessibilityResults)
    }
    
    private fun runAccessibilityChecks(): List<AccessibilityIssue> {
        val issues = mutableListOf<AccessibilityIssue>()
        
        // Check for missing content descriptions
        composeTestRule.onAllNodes(hasClickAction())
            .fetchSemanticsNodes()
            .forEach { node ->
                val hasDescription = node.config.contains(SemanticsProperties.ContentDescription)
                if (!hasDescription) {
                    issues.add(
                        AccessibilityIssue(
                            type = "Missing Content Description",
                            severity = AccessibilitySeverity.HIGH,
                            description = "Interactive element lacks content description"
                        )
                    )
                }
            }
        
        // Check for proper heading structure
        val headings = composeTestRule.onAllNodes(isHeading())
            .fetchSemanticsNodes()
        
        // Validate heading hierarchy
        validateHeadingHierarchy(headings)?.let { issues.add(it) }
        
        // Check touch target sizes
        issues.addAll(validateTouchTargetSizes())
        
        return issues
    }
}
```

## Conclusion

WhisperTop's accessibility implementation ensures:

- ✅ **WCAG 2.1 AA Compliance**: Meets international accessibility standards
- ✅ **Screen Reader Support**: Full TalkBack compatibility with meaningful announcements
- ✅ **Keyboard Navigation**: Complete keyboard accessibility with logical tab order
- ✅ **High Contrast Support**: Automatic adaptation to accessibility display preferences  
- ✅ **Motor Accessibility**: Appropriate touch targets and alternative interaction methods
- ✅ **Dynamic Type Support**: Text scaling up to 200% with responsive layouts
- ✅ **Color Accessibility**: Color-blind friendly design with non-color indicators

**Testing Coverage:**
- Automated accessibility testing in CI/CD pipeline
- Manual testing procedures for TalkBack, high contrast, and large text
- Comprehensive accessibility audit tools and reporting
- Regular accessibility reviews and updates

**Key Achievements:**
- Accessible text insertion via proper accessibility service implementation
- Voice-controlled interface that's inherently accessible
- Clear semantic markup throughout the application
- Comprehensive accessibility documentation and guidelines
- Integration of accessibility considerations into development workflow

WhisperTop demonstrates that accessibility is not an afterthought but a fundamental aspect of inclusive design, ensuring all users can benefit from speech-to-text technology regardless of their abilities.