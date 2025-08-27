package me.shadykhalifa.whispertop.presentation.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import me.shadykhalifa.whispertop.presentation.viewmodels.DashboardViewModel
import me.shadykhalifa.whispertop.presentation.ui.components.TrendChartComponent
import me.shadykhalifa.whispertop.presentation.ui.components.ShimmerDashboardStats
import me.shadykhalifa.whispertop.presentation.ui.components.ShimmerChart
import me.shadykhalifa.whispertop.presentation.ui.components.ShimmerTranscriptionCard
import me.shadykhalifa.whispertop.presentation.ui.components.ShimmerBox
import me.shadykhalifa.whispertop.presentation.ui.components.AnimatedErrorState
import org.koin.compose.koinInject
import kotlin.math.roundToInt

@Composable
private fun EmptyStateContent(
    modifier: Modifier = Modifier,
    horizontalPadding: Dp
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = horizontalPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(64.dp))
        }

        item {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        item {
            Text(
                text = "Welcome to WhisperTop!",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }

        item {
            Text(
                text = "Start transcribing speech to see your productivity statistics and insights.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }

        item {
            Text(
                text = "Sample Dashboard Preview",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        }

        item {
            Text(
                text = "Below is a preview of what your dashboard will look like with real data:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}

data class MetricCardData(
    val title: String,
    val value: String,
    val icon: ImageVector,
    val iconColor: Color,
    val animatedValue: Int
)

data class StatisticCardData(
    val title: String,
    val value: String,
    val icon: ImageVector,
    val animatedValue: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    dashboardViewModel: DashboardViewModel = koinInject()
) {
    val uiState by dashboardViewModel.uiState.collectAsStateWithLifecycle()
    val pullToRefreshState = rememberPullToRefreshState()
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    
    // Enhanced pull-to-refresh with custom spring animation
    val refreshProgress by animateFloatAsState(
        targetValue = if (uiState.isRefreshing) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "refreshProgress"
    )
    
    // Calculate responsive layout parameters
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val screenWidthDp = configuration.screenWidthDp
    val isTablet = screenWidthDp >= 600
    
    // Determine grid columns based on screen size and orientation
    val metricsColumns = remember(isLandscape, isTablet, screenWidthDp) {
        when {
            isTablet && isLandscape -> 4
            isTablet -> 3
            isLandscape -> 3
            else -> 2
        }
    }
    
    val statisticsColumns = remember(isLandscape, isTablet, screenWidthDp) {
        when {
            isTablet -> 4
            isLandscape -> 3
            else -> 2
        }
    }
    
    // Responsive padding
    val horizontalPadding = remember(isTablet) {
        if (isTablet) 24.dp else 16.dp
    }
    
    // Derived state for expensive calculations
    val speakingTimeMinutes = remember(uiState.statistics?.totalSpeakingTimeMs) {
        derivedStateOf { (uiState.statistics?.totalSpeakingTimeMs ?: 0L) / 60000.0 }
    }.value
    
    val typingTimeMinutes = remember(uiState.statistics?.totalWords) {
        derivedStateOf { calculateTypingTime(uiState.statistics?.totalWords ?: 0L) }
    }.value
    
    val recentTranscriptionsForDisplay = remember(uiState.recentTranscriptions) {
        derivedStateOf { uiState.recentTranscriptions.take(5) }
    }.value
    
    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = { dashboardViewModel.refreshData() },
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_content"),
        state = pullToRefreshState
    ) {
        if (uiState.isLoading && (uiState.statistics == null || uiState.isEmptyState)) {
            // Show loading shimmer when loading and no data OR when loading empty state
            DashboardShimmerContent(
                columns = metricsColumns,
                horizontalPadding = horizontalPadding
            )
        } else if (uiState.isEmptyState && !uiState.isLoading) {
            // Show welcome screen for empty state
            EmptyStateContent(
                modifier = modifier,
                horizontalPadding = horizontalPadding
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
            item(key = "header") {
                DashboardHeader()
            }
            
            item(key = "productivity_metrics") {
                ProductivityMetricsSection(
                    speakingTimeMinutes = speakingTimeMinutes,
                    typingTimeMinutes = typingTimeMinutes,
                    timeSavedMinutes = uiState.timeSavedTotal,
                    efficiencyMultiplier = uiState.efficiencyMultiplier,
                    columns = metricsColumns
                )
            }
            
            item(key = "statistics_grid") {
                StatisticsGrid(
                    totalWords = uiState.totalWordsTranscribed,
                    totalSessions = uiState.statistics?.totalSessions ?: 0,
                    avgWordsPerMinute = uiState.statistics?.averageWordsPerMinute ?: 0.0,
                    avgWordsPerSession = uiState.statistics?.averageWordsPerSession ?: 0.0,
                    columns = statisticsColumns
                )
            }
            
            item(key = "trend_chart") {
                var showSessions by remember { mutableStateOf(true) }
                TrendChartComponent(
                    trendData = uiState.trendData,
                    showSessions = showSessions,
                    onMetricToggle = { showSessionsMetric ->
                        showSessions = showSessionsMetric
                    }
                )
            }
            
            if (uiState.recentTranscriptions.isNotEmpty()) {
                item(key = "recent_activity_header") {
                    Text(
                        text = "Recent Activity",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                items(
                    items = recentTranscriptionsForDisplay,
                    key = { it.id }
                ) { session ->
                    RecentActivityItem(session = session)
            }
            }
        }
    }
}
}

@Composable
private fun DashboardHeader() {
    val currentTime = remember {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    }
    val greeting = when (currentTime.hour) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }
    
    Column(
        modifier = Modifier.semantics { 
            contentDescription = "Dashboard header with greeting and current date" 
        }
    ) {
        Text(
            text = greeting,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = "Here's your productivity overview",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProductivityMetricsSection(
    speakingTimeMinutes: Double,
    typingTimeMinutes: Double,
    timeSavedMinutes: Double,
    efficiencyMultiplier: Float,
    columns: Int = 2
) {
    Column {
        Text(
            text = "Productivity Metrics",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        val metricCards = listOf(
            MetricCardData("Speaking Time", formatTime(speakingTimeMinutes), Icons.Default.Mic, Color(0xFF4CAF50), speakingTimeMinutes.roundToInt()),
            MetricCardData("Typing Time", formatTime(typingTimeMinutes), Icons.Default.Keyboard, Color(0xFFFF9800), typingTimeMinutes.roundToInt()),
            MetricCardData("Time Saved", formatTime(timeSavedMinutes), Icons.Default.Schedule, Color(0xFF2196F3), timeSavedMinutes.roundToInt()),
            MetricCardData("Efficiency", "${(efficiencyMultiplier * 100).roundToInt()}%", Icons.Default.Timeline, MaterialTheme.colorScheme.primary, (efficiencyMultiplier * 100).roundToInt())
        )
        
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            metricCards.chunked(columns).forEach { rowCards ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowCards.forEach { cardData ->
                        MetricCard(
                            title = cardData.title,
                            value = cardData.value,
                            icon = cardData.icon,
                            iconColor = cardData.iconColor,
                            animatedValue = cardData.animatedValue,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Fill remaining space if this row has fewer items
                    repeat(columns - rowCards.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatisticsGrid(
    totalWords: Long,
    totalSessions: Int,
    avgWordsPerMinute: Double,
    avgWordsPerSession: Double,
    columns: Int = 2
) {
    Column {
        Text(
            text = "Statistics Overview",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        val statisticCards = listOf(
            StatisticCardData("Words Captured", formatNumber(totalWords), Icons.Default.TextFields, totalWords.toInt()),
            StatisticCardData("Sessions", totalSessions.toString(), Icons.Default.PlayArrow, totalSessions),
            StatisticCardData("Avg Words/Min", "${avgWordsPerMinute.roundToInt()}", Icons.Default.Speed, avgWordsPerMinute.roundToInt()),
            StatisticCardData("Words/Session", "${avgWordsPerSession.roundToInt()}", Icons.Default.Analytics, avgWordsPerSession.roundToInt())
        )
        
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            statisticCards.chunked(columns).forEach { rowCards ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowCards.forEach { cardData ->
                        StatisticCard(
                            title = cardData.title,
                            value = cardData.value,
                            icon = cardData.icon,
                            animatedValue = cardData.animatedValue,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Fill remaining space if this row has fewer items
                    repeat(columns - rowCards.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    animatedValue: Int,
    modifier: Modifier = Modifier
) {
    var animatedCount by remember { mutableIntStateOf(0) }
    val animatedCountValue by animateIntAsState(
        targetValue = animatedValue,
        animationSpec = tween(
            durationMillis = 800,
            easing = FastOutSlowInEasing
        ),
        label = "MetricCardCounter"
    )
    
    LaunchedEffect(animatedValue) {
        animatedCount = animatedCountValue
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp)
            .semantics { 
                contentDescription = "$title: $value" 
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Column {
                AnimatedContent(
                    targetState = animatedCountValue,
                    transitionSpec = {
                        slideInVertically(
                            animationSpec = tween(300),
                            initialOffsetY = { it }
                        ).plus(fadeIn(animationSpec = tween(300))) togetherWith
                        slideOutVertically(
                            animationSpec = tween(300),
                            targetOffsetY = { -it }
                        ).plus(fadeOut(animationSpec = tween(300)))
                    },
                    label = "CounterAnimation"
                ) { count ->
                    Text(
                        text = if (title == "Efficiency") "${count}%" else when {
                            title.contains("Time") -> formatTime(count.toDouble())
                            else -> formatNumber(count.toLong())
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatisticCard(
    title: String,
    value: String,
    icon: ImageVector,
    animatedValue: Int,
    modifier: Modifier = Modifier
) {
    val animatedCountValue by animateIntAsState(
        targetValue = animatedValue,
        animationSpec = tween(
            durationMillis = 800,
            easing = FastOutSlowInEasing
        ),
        label = "StatisticCardCounter"
    )
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(110.dp)
            .semantics { 
                contentDescription = "$title: $value" 
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Column {
                AnimatedContent(
                    targetState = animatedCountValue,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith
                        fadeOut(animationSpec = tween(300))
                    },
                    label = "StatisticCounterAnimation"
                ) { count ->
                    Text(
                        text = formatNumber(count.toLong()),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RecentActivityItem(
    session: me.shadykhalifa.whispertop.domain.models.TranscriptionSession,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics { 
                contentDescription = "Recent transcription session with ${session.wordCount} words" 
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.RecordVoiceOver,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "${session.wordCount} words transcribed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = formatDuration(session.audioLengthMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Text(
                text = formatTimeAgo(session.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Helper functions
private fun calculateTypingTime(totalWords: Long): Double {
    val averageWpm = 40.0
    return totalWords / averageWpm
}

private fun formatTime(minutes: Double): String {
    val hours = (minutes / 60).toInt()
    val mins = (minutes % 60).toInt()
    return if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
}

private fun formatNumber(number: Long): String {
    return when {
        number >= 1_000_000 -> "${(number / 1_000_000.0).roundToInt()}M"
        number >= 1_000 -> "${(number / 1_000.0).roundToInt()}K"
        else -> number.toString()
    }
}

private fun formatDuration(durationMs: Long): String {
    val seconds = durationMs / 1000
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "${minutes}:${remainingSeconds.toString().padStart(2, '0')}"
}

private fun formatTimeAgo(instant: kotlinx.datetime.Instant): String {
    val now = Clock.System.now()
    val diff = now - instant
    
    return when {
        diff.inWholeMinutes < 1 -> "Just now"
        diff.inWholeMinutes < 60 -> "${diff.inWholeMinutes}m ago"
        diff.inWholeHours < 24 -> "${diff.inWholeHours}h ago"
        else -> "${diff.inWholeDays}d ago"
    }
}

@Composable
private fun DashboardShimmerContent(
    columns: Int,
    horizontalPadding: Dp
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = horizontalPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            ShimmerHeaderContent()
        }
        
        item {
            ShimmerDashboardStats(columns = columns)
        }
        
        item {
            ShimmerChart(height = 200.dp)
        }
        
        items(3) {
            ShimmerTranscriptionCard()
        }
    }
}

@Composable
private fun ShimmerHeaderContent() {
    Column {
        ShimmerBox(
            modifier = Modifier
                .width(200.dp)
                .height(32.dp)
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        ShimmerBox(
            modifier = Modifier
                .width(280.dp)
                .height(20.dp)
        )
    }
}



@Composable
private fun ErrorStateContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedErrorState(
        message = message,
        onRetry = onRetry,
        modifier = modifier
    )
}