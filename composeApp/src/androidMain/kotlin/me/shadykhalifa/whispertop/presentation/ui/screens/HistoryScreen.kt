package me.shadykhalifa.whispertop.presentation.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable

import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems

import kotlinx.coroutines.launch
import me.shadykhalifa.whispertop.domain.models.TranscriptionHistory
import me.shadykhalifa.whispertop.presentation.viewmodels.HistoryUiEvent
import me.shadykhalifa.whispertop.presentation.viewmodels.HistoryUiState
import me.shadykhalifa.whispertop.presentation.viewmodels.HistoryViewModel
import org.koin.compose.koinInject
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable 
fun HistoryScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToDetail: (String) -> Unit = {},
    viewModel: HistoryViewModel = koinInject()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedItems by viewModel.selectedItems.collectAsStateWithLifecycle()
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    
    val transcriptions = viewModel.transcriptions.collectAsLazyPagingItems()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    
    var showDropdownMenu by remember { mutableStateOf(false) }
    var showBulkDeleteDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<TranscriptionHistory?>(null) }
    
    // Handle UI events
    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is HistoryUiEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is HistoryUiEvent.ShowError -> {
                    snackbarHostState.showSnackbar("Error: ${event.message}")
                }
                is HistoryUiEvent.ExportCompleted -> {
                    snackbarHostState.showSnackbar(
                        "Exported ${event.itemCount} items to ${event.format.name}"
                    )
                }
                HistoryUiEvent.RefreshCompleted -> {
                    // Handle refresh completion if needed
                }
            }
        }
    }
    
    // Confirmation Dialogs
    if (showBulkDeleteDialog) {
        BulkDeleteConfirmationDialog(
            selectedCount = selectedItems.size,
            onConfirm = {
                viewModel.deleteSelectedItems()
                showBulkDeleteDialog = false
            },
            onDismiss = { showBulkDeleteDialog = false }
        )
    }
    
    itemToDelete?.let { transcription ->
        SingleDeleteConfirmationDialog(
            transcription = transcription,
            onConfirm = {
                viewModel.deleteTranscription(transcription.id)
                itemToDelete = null
            },
            onDismiss = { itemToDelete = null }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            HistoryTopAppBar(
                isSelectionMode = isSelectionMode,
                selectedCount = selectedItems.size,
                searchQuery = searchQuery,
                onSearchQueryChange = viewModel::updateSearchQuery,
                onClearSearch = viewModel::clearSearch,
                onNavigateBack = onNavigateBack,
                onClearSelection = viewModel::clearSelection,
                onSelectAll = {
                    val visibleIds = (0 until transcriptions.itemCount).mapNotNull { index ->
                        transcriptions[index]?.id
                    }
                    viewModel.selectAll(visibleIds)
                },
                onDeleteSelected = { showBulkDeleteDialog = true },
                onExportSelected = { /* TODO: Connect to export functionality */ },
                onShareSelected = { /* TODO: Implement share selected */ },
                onExportAllJson = viewModel::exportAsJson,
                onExportAllCsv = viewModel::exportAsCsv,
                showDropdownMenu = showDropdownMenu,
                onDropdownMenuToggle = { showDropdownMenu = it }
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = !isSelectionMode,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                FloatingActionButton(
                    onClick = { 
                        coroutineScope.launch {
                            transcriptions.refresh()
                        }
                    }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        HistoryContent(
            transcriptions = transcriptions,
            uiState = uiState,
            selectedItems = selectedItems,
            isSelectionMode = isSelectionMode,
            onItemClick = { transcription ->
                if (isSelectionMode) {
                    viewModel.toggleItemSelection(transcription.id)
                } else {
                    onNavigateToDetail(transcription.id)
                }
            },
            onItemLongPress = { transcription ->
                if (!isSelectionMode) {
                    viewModel.toggleItemSelection(transcription.id)
                }
            },
            onDeleteItem = { transcription -> itemToDelete = transcription },
            onRefresh = { 
                coroutineScope.launch {
                    transcriptions.refresh()
                }
            },
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryTopAppBar(
    isSelectionMode: Boolean,
    selectedCount: Int,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onNavigateBack: () -> Unit,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onDeleteSelected: () -> Unit,
    onExportSelected: () -> Unit,
    onShareSelected: () -> Unit,
    onExportAllJson: () -> Unit,
    onExportAllCsv: () -> Unit,
    showDropdownMenu: Boolean,
    onDropdownMenuToggle: (Boolean) -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelectionMode) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "topbar_background"
    )
    
    TopAppBar(
        title = {
            if (isSelectionMode) {
                Text("$selectedCount selected")
            } else {
                Column {
                    Text(
                        text = "Transcription History",
                        style = MaterialTheme.typography.titleLarge
                    )
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text("Search transcriptions...") },
                        leadingIcon = { 
                            Icon(Icons.Default.Search, contentDescription = null) 
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = onClearSearch) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(
                onClick = {
                    if (isSelectionMode) {
                        onClearSelection()
                    } else {
                        onNavigateBack()
                    }
                }
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Navigate back")
            }
        },
        actions = {
            if (isSelectionMode) {
                IconButton(onClick = onSelectAll) {
                    Icon(Icons.Default.SelectAll, contentDescription = "Select all")
                }
                IconButton(onClick = onShareSelected) {
                    Icon(Icons.Default.Share, contentDescription = "Share selected")
                }
                IconButton(onClick = onExportSelected) {
                    Icon(Icons.Default.FileDownload, contentDescription = "Export selected")
                }
                IconButton(onClick = onDeleteSelected) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete selected")
                }
            } else {
                IconButton(onClick = { onDropdownMenuToggle(true) }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                }
                DropdownMenu(
                    expanded = showDropdownMenu,
                    onDismissRequest = { onDropdownMenuToggle(false) }
                ) {
                    DropdownMenuItem(
                        text = { Text("Export all as JSON") },
                        onClick = { 
                            onExportAllJson()
                            onDropdownMenuToggle(false)
                        },
                        leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Export all as CSV") },
                        onClick = { 
                            onExportAllCsv()
                            onDropdownMenuToggle(false)
                        },
                        leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null) }
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = backgroundColor
        )
    )
}

@Composable
private fun HistoryContent(
    transcriptions: LazyPagingItems<TranscriptionHistory>,
    uiState: HistoryUiState,
    selectedItems: Set<String>,
    isSelectionMode: Boolean,
    onItemClick: (TranscriptionHistory) -> Unit,
    onItemLongPress: (TranscriptionHistory) -> Unit,
    onDeleteItem: (TranscriptionHistory) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        transcriptions.loadState.refresh is LoadState.Loading && transcriptions.itemCount == 0 -> {
            LoadingStateWithPlaceholders(modifier = modifier)
        }
        transcriptions.loadState.refresh is LoadState.Error && transcriptions.itemCount == 0 -> {
            ErrorState(
                error = (transcriptions.loadState.refresh as LoadState.Error).error,
                onRetry = { 
                    transcriptions.retry()
                    onRefresh()
                },
                onRetryWithRefresh = {
                    transcriptions.refresh()
                },
                modifier = modifier
            )
        }
        transcriptions.itemCount == 0 -> {
            EmptyState(modifier = modifier)
        }
        else -> {
            Column(modifier = modifier) {
                // Simple refresh button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onRefresh
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Refresh")
                    }
                }
                
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 88.dp // Extra space for FAB
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        count = transcriptions.itemCount,
                        key = { index -> transcriptions[index]?.id ?: index }
                    ) { index ->
                        val transcription = transcriptions[index]
                        if (transcription != null) {
                            TranscriptionListItem(
                                transcription = transcription,
                                isSelected = selectedItems.contains(transcription.id),
                                isSelectionMode = isSelectionMode,
                                onClick = { onItemClick(transcription) },
                                onLongPress = { onItemLongPress(transcription) },
                                onDelete = { onDeleteItem(transcription) }
                            )
                        }
                    }
                    
                    // Loading indicator for pagination
                    when (val appendState = transcriptions.loadState.append) {
                        is LoadState.Loading -> {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                        is LoadState.Error -> {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "Failed to load more items",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        TextButton(onClick = { transcriptions.retry() }) {
                                            Text("Retry")
                                        }
                                    }
                                }
                            }
                        }
                        else -> { /* No action needed for NotLoading */ }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TranscriptionListItem(
    transcription: TranscriptionHistory,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = rememberDateFormatter()
    val cardColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "card_color"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 0.95f else 1f,
        label = "card_scale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            ),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        )
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = transcription.text.take(100).let {
                        if (transcription.text.length > 100) "$it..." else it
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            },
            supportingContent = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dateFormatter(transcription.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val duration = transcription.duration
                        if (duration != null && duration > 0f) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = formatDuration((duration * 1000).toLong()),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                        Badge(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text = "${transcription.wordCount} words",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            },
            leadingContent = {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            },
            trailingContent = {
                if (isSelectionMode) {
                    if (isSelected) {
                        Surface(
                            modifier = Modifier.size(24.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Selected",
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(4.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    } else {
                        Surface(
                            modifier = Modifier.size(24.dp),
                            shape = CircleShape,
                            color = Color.Transparent
                        ) {
                            // Empty circle for unselected items
                        }
                    }
                }
        }
    )
}




}



@Composable
private fun LoadingStateWithPlaceholders(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = "Loading transcriptions...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorState(
    error: Throwable,
    onRetry: () -> Unit,
    onRetryWithRefresh: () -> Unit = onRetry,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.History,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = "Failed to load transcriptions",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = error.localizedMessage ?: "An unexpected error occurred",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onRetry) {
                    Text("Retry")
                }
                TextButton(onClick = onRetryWithRefresh) {
                    Text("Refresh")
                }
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.History,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Text(
                text = "No transcriptions yet",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Start recording to see your transcription history here",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun rememberDateFormatter(): (Long) -> String {
    return remember {
        { timestamp ->
            try {
                val instant = Instant.fromEpochMilliseconds(timestamp)
                val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
                
                val month = localDateTime.month.name.take(3).lowercase()
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                val day = localDateTime.dayOfMonth.toString().padStart(2, '0')
                val year = localDateTime.year
                val hour = localDateTime.hour.toString().padStart(2, '0')
                val minute = localDateTime.minute.toString().padStart(2, '0')
                
                "$month $day, $year at $hour:$minute"
            } catch (e: Exception) {
                "Invalid date"
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}

@Composable
private fun BulkDeleteConfirmationDialog(
    selectedCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Delete Transcriptions")
        },
        text = {
            Text("Are you sure you want to delete $selectedCount selected transcription${if (selectedCount > 1) "s" else ""}? This action cannot be undone.")
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text(
                    "Delete",
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun SingleDeleteConfirmationDialog(
    transcription: TranscriptionHistory,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Delete Transcription")
        },
        text = {
            Column {
                Text("Are you sure you want to delete this transcription?")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = transcription.text.take(100).let {
                        if (transcription.text.length > 100) "$it..." else it
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text(
                    "Delete",
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}