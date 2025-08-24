package me.shadykhalifa.whispertop.presentation.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun TypingTestComponent(
    text: String,
    isInProgress: Boolean,
    progress: Float,
    timeRemaining: Int,
    onStartTest: () -> Unit,
    onUpdateProgress: (wordsTyped: Int, secondsElapsed: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var userInput by remember { mutableStateOf("") }
    var startTime by remember { mutableLongStateOf(0L) }
    val focusRequester = remember { FocusRequester() }
    
    // Calculate statistics
    val wordsTyped = userInput.trim().split("\\s+".toRegex()).size
    val charactersTyped = userInput.length
    val secondsElapsed = if (startTime > 0) {
        ((System.currentTimeMillis() - startTime) / 1000).toInt()
    } else 0
    
    val currentWpm = if (secondsElapsed > 0) {
        (wordsTyped * 60) / secondsElapsed
    } else 0
    
    // Auto-update progress during test
    LaunchedEffect(isInProgress, userInput) {
        if (isInProgress && startTime > 0) {
            val elapsed = ((System.currentTimeMillis() - startTime) / 1000).toInt()
            onUpdateProgress(wordsTyped, elapsed)
        }
    }
    
    // Timer effect
    LaunchedEffect(isInProgress) {
        if (isInProgress) {
            startTime = System.currentTimeMillis()
            focusRequester.requestFocus()
            
            // Update every second
            while (isInProgress && secondsElapsed < 60) {
                delay(1000L)
                val elapsed = ((System.currentTimeMillis() - startTime) / 1000).toInt()
                onUpdateProgress(wordsTyped, elapsed)
            }
        }
    }
    
    Column(
        modifier = modifier
    ) {
        // Test controls and stats
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
                    // Timer
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = formatTime(timeRemaining),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (timeRemaining <= 10) MaterialTheme.colorScheme.error 
                                   else MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Time Left",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    
                    // Current WPM
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$currentWpm",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Current WPM",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    
                    // Progress
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            progress = progress,
                            modifier = Modifier.size(40.dp),
                            strokeWidth = 4.dp,
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
                
                if (isInProgress) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Text to type
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Type this text:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Reference text with highlighting
                TypingTestText(
                    referenceText = text,
                    userInput = userInput,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .verticalScroll(rememberScrollState())
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Input field
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isInProgress) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Your typing:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                BasicTextField(
                    value = userInput,
                    onValueChange = { newValue ->
                        if (isInProgress) {
                            userInput = newValue
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .focusRequester(focusRequester)
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = if (isInProgress) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            },
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp),
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    enabled = isInProgress,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrect = false
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (userInput.isEmpty() && !isInProgress) {
                                Text(
                                    text = "Click 'Start Test' to begin typing here...",
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    style = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 14.sp
                                    ),
                                    modifier = Modifier.align(Alignment.TopStart)
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Start/Stop button
        if (!isInProgress) {
            Button(
                onClick = {
                    userInput = ""
                    onStartTest()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Test")
            }
        } else {
            // Show stats during test
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatCard(
                    value = charactersTyped.toString(),
                    label = "Characters",
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                StatCard(
                    value = wordsTyped.toString(),
                    label = "Words",
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                StatCard(
                    value = calculateAccuracy(text, userInput).toString() + "%",
                    label = "Accuracy",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TypingTestText(
    referenceText: String,
    userInput: String,
    modifier: Modifier = Modifier
) {
    val annotatedText = buildAnnotatedString {
        referenceText.forEachIndexed { index, char ->
            when {
                index < userInput.length -> {
                    val userChar = userInput[index]
                    if (userChar == char) {
                        // Correct character - green background
                        withStyle(
                            style = SpanStyle(
                                background = Color(0xFF4CAF50).copy(alpha = 0.3f),
                                color = Color(0xFF2E7D32)
                            )
                        ) {
                            append(char)
                        }
                    } else {
                        // Incorrect character - red background
                        withStyle(
                            style = SpanStyle(
                                background = Color(0xFFF44336).copy(alpha = 0.3f),
                                color = Color(0xFFC62828),
                                textDecoration = TextDecoration.Underline
                            )
                        ) {
                            append(char)
                        }
                    }
                }
                index == userInput.length -> {
                    // Current character - cursor position with yellow background
                    withStyle(
                        style = SpanStyle(
                            background = Color(0xFFFFEB3B).copy(alpha = 0.5f),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        append(char)
                    }
                }
                else -> {
                    // Untyped character - normal
                    withStyle(
                        style = SpanStyle(
                            color = Color.Gray
                        )
                    ) {
                        append(char)
                    }
                }
            }
        }
    }
    
    Text(
        text = annotatedText,
        modifier = modifier,
        style = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 16.sp,
            lineHeight = 24.sp
        )
    )
}

@Composable
private fun StatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

private fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%d:%02d", minutes, remainingSeconds)
}

private fun calculateAccuracy(referenceText: String, userInput: String): Int {
    if (userInput.isEmpty()) return 100
    
    val minLength = minOf(referenceText.length, userInput.length)
    var correctChars = 0
    
    for (i in 0 until minLength) {
        if (referenceText[i] == userInput[i]) {
            correctChars++
        }
    }
    
    return ((correctChars.toFloat() / userInput.length) * 100).toInt()
}