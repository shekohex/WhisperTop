package me.shadykhalifa.whispertop.presentation.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.shadykhalifa.whispertop.domain.models.TranscriptionHistoryItem
import me.shadykhalifa.whispertop.presentation.ui.utils.TextUtils
import me.shadykhalifa.whispertop.presentation.viewmodels.TranscriptionDetailUiEvent
import me.shadykhalifa.whispertop.presentation.viewmodels.TranscriptionDetailViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscriptionDetailScreen(
    transcriptionId: String,
    onNavigateBack: () -> Unit,
    viewModel: TranscriptionDetailViewModel = koinInject()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val uiEvents by viewModel.uiEvents.collectAsStateWithLifecycle()
    
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }
    
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    // Load transcription when screen opens
    LaunchedEffect(transcriptionId) {
        viewModel.loadTranscription(transcriptionId)
    }
    
    // Handle UI events
    LaunchedEffect(uiEvents) {
        when (val event = uiEvents) {
            is TranscriptionDetailUiEvent.ShowMessage -> {
                snackbarHostState.showSnackbar(event.message)
                viewModel.clearEvent()
            }
            is TranscriptionDetailUiEvent.ShowError -> {
                snackbarHostState.showSnackbar(event.message)
                viewModel.clearEvent()
            }
            is TranscriptionDetailUiEvent.NavigateBack -> {
                onNavigateBack()
                viewModel.clearEvent()
            }
            is TranscriptionDetailUiEvent.ShareText -> {
                shareText(context, event.text)
                viewModel.clearEvent()
            }
            is TranscriptionDetailUiEvent.CopyToClipboard -> {
                copyToClipboard(context, event.text)
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                snackbarHostState.showSnackbar("Copied to clipboard")
                viewModel.clearEvent()
            }
            null -> { /* No event */ }
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Transcription") },
            text = { Text("Are you sure you want to delete this transcription? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteTranscription()
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transcription Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Go back",
                            modifier = Modifier.semantics {
                                contentDescription = "Navigate back to transcription history"
                            }
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.copyToClipboard()
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        enabled = uiState.transcription != null
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy text",
                            modifier = Modifier.semantics {
                                contentDescription = "Copy transcription text to clipboard"
                            }
                        )
                    }
                    IconButton(
                        onClick = {
                            viewModel.shareTranscription()
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        enabled = uiState.transcription != null
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Share text",
                            modifier = Modifier.semantics {
                                contentDescription = "Share transcription text"
                            }
                        )
                    }
                    IconButton(
                        onClick = {
                            showDeleteDialog = true
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        enabled = uiState.transcription != null
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete transcription",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.semantics {
                                contentDescription = "Delete this transcription permanently"
                            }
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = "Loading transcription...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            uiState.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = uiState.error!!,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Button(onClick = onNavigateBack) {
                            Text("Go Back")
                        }
                    }
                }
            }
            
            uiState.transcription != null -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Main transcription text with selection support
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "Transcription",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                
                                SelectionContainer {
                                    Text(
                                        text = TextUtils.buildSyntaxHighlightedText(uiState.transcription!!.text),
                                        style = MaterialTheme.typography.bodyLarge,
                                        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.2,
                                        modifier = Modifier.semantics {
                                            contentDescription = "Transcription text content: ${uiState.transcription!!.text}"
                                        }
                                    )
                                }
                            }
                        }
                    }
                    
                    // Metadata cards
                    item {
                        TranscriptionMetadata(
                            transcription = uiState.transcription!!,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TranscriptionMetadata(
    transcription: TranscriptionHistoryItem,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Date and Time
            MetadataRow(
                label = "Date & Time",
                value = TextUtils.formatTimestamp(transcription.timestamp)
            )
            
            // Duration
            if (transcription.duration != null) {
                MetadataRow(
                    label = "Duration",
                    value = TextUtils.formatDuration(transcription.duration)
                )
            }
            
            // Word Count
            MetadataRow(
                label = "Word Count",
                value = TextUtils.getWordCount(transcription.text).toString()
            )
            
            // Character Count
            MetadataRow(
                label = "Character Count",
                value = TextUtils.getCharacterCount(transcription.text).toString()
            )
            
            // Language
            if (!transcription.language.isNullOrBlank()) {
                MetadataRow(
                    label = "Language",
                    value = transcription.language!!
                )
            }
            
            // Model
            if (!transcription.model.isNullOrBlank()) {
                MetadataRow(
                    label = "Model",
                    value = transcription.model!!
                )
            }
            
            // Confidence
            if (transcription.confidence != null) {
                MetadataRow(
                    label = "Confidence",
                    value = "${(transcription.confidence!! * 100).toInt()}%"
                )
            }
            
            // Temperature
            if (transcription.temperature != null) {
                MetadataRow(
                    label = "Temperature",
                    value = transcription.temperature!!.toString()
                )
            }
            
            // Custom Prompt
            if (!transcription.customPrompt.isNullOrBlank()) {
                MetadataRow(
                    label = "Custom Prompt",
                    value = transcription.customPrompt!!,
                    isExpandable = true
                )
            }
        }
    }
}

@Composable
private fun MetadataRow(
    label: String,
    value: String,
    isExpandable: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            
            if (isExpandable && value.length > 50) {
                Column(
                    modifier = Modifier.weight(2f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = if (expanded) value else "${value.take(50)}...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.End
                    )
                    TextButton(
                        onClick = { expanded = !expanded },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = if (expanded) "Show Less" else "Show More",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            } else {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(2f)
                )
            }
        }
        
        if (isExpandable && value.length <= 50) {
            // For short expandable text, show it normally
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun shareText(context: Context, text: String) {
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        putExtra(Intent.EXTRA_SUBJECT, "Transcription")
    }
    
    val chooserIntent = Intent.createChooser(shareIntent, "Share transcription")
    context.startActivity(chooserIntent)
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clipData = ClipData.newPlainText("Transcription", text)
    clipboardManager.setPrimaryClip(clipData)
}