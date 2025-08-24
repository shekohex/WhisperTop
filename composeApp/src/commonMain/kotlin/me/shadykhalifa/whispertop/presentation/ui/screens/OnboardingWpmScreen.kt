package me.shadykhalifa.whispertop.presentation.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.shadykhalifa.whispertop.presentation.viewmodels.OnboardingWpmViewModel
import me.shadykhalifa.whispertop.presentation.viewmodels.OnboardingWpmUiState
import me.shadykhalifa.whispertop.presentation.viewmodels.WpmOnboardingStep
import me.shadykhalifa.whispertop.presentation.viewmodels.AgeGroup
import me.shadykhalifa.whispertop.presentation.ui.components.TypingTestComponent
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingWpmScreen(
    onNavigateBack: () -> Unit,
    onCompleteOnboarding: () -> Unit,
    viewModel: OnboardingWpmViewModel = koinInject()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorInfo by viewModel.errorInfo.collectAsStateWithLifecycle()
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(errorInfo) {
        errorInfo?.let { error ->
            snackbarHostState.showSnackbar(
                message = error.message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }
    
    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted) {
            onCompleteOnboarding()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Setup Typing Speed") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.currentStep == WpmOnboardingStep.WELCOME) {
                            onNavigateBack()
                        } else {
                            viewModel.goToPreviousStep()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            OnboardingBottomBar(
                uiState = uiState,
                isLoading = isLoading,
                onNextClick = { viewModel.proceedToNextStep() },
                onSaveClick = { viewModel.saveWpmConfiguration() },
                canProceed = viewModel.canProceedFromCurrentStep()
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedContent(
                targetState = uiState.currentStep,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(300)) with
                    slideOutHorizontally(
                        targetOffsetX = { -it },
                        animationSpec = tween(300)
                    ) + fadeOut(animationSpec = tween(300))
                }
            ) { step ->
                when (step) {
                    WpmOnboardingStep.WELCOME -> WelcomeStep()
                    WpmOnboardingStep.AGE_SELECTION -> AgeSelectionStep(
                        uiState = uiState,
                        onAgeSelected = viewModel::setAge
                    )
                    WpmOnboardingStep.WPM_INPUT -> WpmInputStep(
                        uiState = uiState,
                        onManualInputToggle = viewModel::toggleManualInputMode,
                        onManualWpmInput = viewModel::setManualWpmInput,
                        onShowTypingTestToggle = viewModel::setShowTypingTest
                    )
                    WpmOnboardingStep.TYPING_TEST -> TypingTestStep(
                        uiState = uiState,
                        onStartTest = viewModel::startTypingTest,
                        onUpdateProgress = viewModel::updateTypingTestProgress,
                        onSkipTest = viewModel::skipTypingTest
                    )
                    WpmOnboardingStep.CONFIRMATION -> ConfirmationStep(
                        uiState = uiState,
                        recommendationText = viewModel.getWpmRecommendationText(uiState.selectedAge)
                    )
                    WpmOnboardingStep.COMPLETE -> CompleteStep(
                        onRestartOnboarding = viewModel::restartOnboarding
                    )
                }
            }
            
            // Progress indicator
            OnboardingProgressIndicator(
                currentStep = uiState.currentStep,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }
    }
}

@Composable
private fun WelcomeStep() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Speed,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Welcome to WPM Setup",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Let's configure your typing speed to provide accurate time-saved calculations. This helps us show you how much time WhisperTop saves compared to manual typing.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Did you know?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "The average mobile typing speed is 36 WPM, while speaking is typically 150-200 words per minute. That's why speech-to-text can save you significant time!",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun AgeSelectionStep(
    uiState: OnboardingWpmUiState,
    onAgeSelected: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "What's your age range?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "We'll suggest an appropriate typing speed based on research data for your age group.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        LazyColumn(
            modifier = Modifier.height(300.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(AgeGroup.AGE_GROUPS) { ageGroup ->
                AgeGroupCard(
                    ageGroup = ageGroup,
                    isSelected = uiState.selectedAge?.let { age ->
                        age in ageGroup.minAge..ageGroup.maxAge
                    } ?: false,
                    onClick = { onAgeSelected(ageGroup.minAge + (ageGroup.maxAge - ageGroup.minAge) / 2) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        uiState.validationErrors["age"]?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun AgeGroupCard(
    ageGroup: AgeGroup,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(
                2.dp,
                MaterialTheme.colorScheme.primary
            )
        } else null
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ages ${ageGroup.range}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${ageGroup.suggestedWpm} WPM",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = ageGroup.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun WpmInputStep(
    uiState: OnboardingWpmUiState,
    onManualInputToggle: () -> Unit,
    onManualWpmInput: (String) -> Unit,
    onShowTypingTestToggle: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Typing Speed Configuration",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Based on your age, we recommend ${uiState.suggestedWpm} WPM. You can accept this or enter your own value.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Suggested WPM Card
        if (!uiState.isManualInputMode) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Recommended Speed",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${uiState.suggestedWpm} WPM",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Words Per Minute",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            // Manual Input
            OutlinedTextField(
                value = uiState.manualWpmInput,
                onValueChange = onManualWpmInput,
                label = { Text("Enter your typing speed (WPM)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.validationErrors["wpm"] != null,
                supportingText = {
                    uiState.validationErrors["wpm"]?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error
                        )
                    } ?: Text("Enter a value between ${OnboardingWpmViewModel.MIN_WPM} and ${OnboardingWpmViewModel.MAX_WPM}")
                }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Toggle Manual Input
        TextButton(
            onClick = onManualInputToggle,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = if (uiState.isManualInputMode) Icons.Default.AutoAwesome else Icons.Default.Edit,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (uiState.isManualInputMode) "Use Recommended Speed" else "Enter Custom Speed"
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Optional Typing Test
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Take Typing Test",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Measure your actual typing speed (optional)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    Switch(
                        checked = uiState.showTypingTest,
                        onCheckedChange = onShowTypingTestToggle
                    )
                }
            }
        }
    }
}

@Composable
private fun TypingTestStep(
    uiState: OnboardingWpmUiState,
    onStartTest: () -> Unit,
    onUpdateProgress: (Int, Int) -> Unit,
    onSkipTest: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "Typing Speed Test",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Type the text below as quickly and accurately as possible for 60 seconds.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (uiState.typingTestResult != null) {
            // Show Results
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Test Complete!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${uiState.typingTestResult} WPM",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your measured typing speed",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            // Show typing test component
            TypingTestComponent(
                text = OnboardingWpmViewModel.TYPING_TEST_TEXT,
                isInProgress = uiState.isTypingTestInProgress,
                progress = uiState.typingTestProgress,
                timeRemaining = uiState.typingTestTimeRemaining,
                onStartTest = onStartTest,
                onUpdateProgress = onUpdateProgress,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (!uiState.isTypingTestInProgress) {
                OutlinedButton(
                    onClick = onSkipTest,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Skip Test")
                }
            }
        }
    }
}

@Composable
private fun ConfirmationStep(
    uiState: OnboardingWpmUiState,
    recommendationText: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Configuration Summary",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = recommendationText,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Your Typing Speed",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "${uiState.finalWpm} WPM",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Words Per Minute",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "What this means",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "WhisperTop will use this speed to calculate how much time you save by using speech-to-text instead of typing manually. You can always change this in Settings.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun CompleteStep(
    onRestartOnboarding: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Celebration,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Setup Complete!",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Your typing speed has been configured successfully. WhisperTop is now ready to show you accurate time-saved calculations.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedButton(
            onClick = onRestartOnboarding
        ) {
            Text("Redo Setup")
        }
    }
}

@Composable
private fun OnboardingBottomBar(
    uiState: OnboardingWpmUiState,
    isLoading: Boolean,
    onNextClick: () -> Unit,
    onSaveClick: () -> Unit,
    canProceed: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            when (uiState.currentStep) {
                WpmOnboardingStep.CONFIRMATION -> {
                    Button(
                        onClick = onSaveClick,
                        enabled = canProceed && !isLoading,
                        modifier = Modifier.height(48.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Complete Setup")
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                WpmOnboardingStep.COMPLETE -> {
                    // No button needed for complete step
                }
                else -> {
                    Button(
                        onClick = onNextClick,
                        enabled = canProceed,
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Next")
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingProgressIndicator(
    currentStep: WpmOnboardingStep,
    modifier: Modifier = Modifier
) {
    val totalSteps = WpmOnboardingStep.values().size - 1 // Exclude COMPLETE step
    val currentStepIndex = minOf(currentStep.stepNumber, totalSteps - 1)
    val progress = (currentStepIndex + 1) / totalSteps.toFloat()
    
    LinearProgressIndicator(
        progress = progress,
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp),
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        color = MaterialTheme.colorScheme.primary
    )
}