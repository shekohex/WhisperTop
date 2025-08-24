package me.shadykhalifa.whispertop.presentation.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import me.shadykhalifa.whispertop.domain.models.DailyUsage
import java.util.Locale

@Composable
fun TrendChartComponent(
    trendData: List<DailyUsage>,
    modifier: Modifier = Modifier,
    title: String = "30-Day Activity Trend",
    showSessions: Boolean = true,
    onMetricToggle: (Boolean) -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    // Create chart model producer
    val modelProducer = remember { CartesianChartModelProducer() }
    
    // Process trend data for chart
    LaunchedEffect(trendData, showSessions) {
        withContext(Dispatchers.Default) {
            if (trendData.isNotEmpty()) {
                val chartData = trendData.map { dailyUsage ->
                    if (showSessions) {
                        dailyUsage.sessionsCount.toFloat()
                    } else {
                        (dailyUsage.wordsTranscribed / 100f) // Scale words for better visualization
                    }
                }
                
                modelProducer.runTransaction {
                    lineSeries {
                        series(chartData)
                    }
                }
            }
        }
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Chart title and metric selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = typography.titleMedium,
                    color = colorScheme.onSurface
                )
                
                // Metric toggle buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        onClick = { onMetricToggle(true) },
                        label = { Text("Sessions") },
                        selected = showSessions,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colorScheme.primary.copy(alpha = 0.12f),
                            selectedLabelColor = colorScheme.primary
                        )
                    )
                    FilterChip(
                        onClick = { onMetricToggle(false) },
                        label = { Text("Words") },
                        selected = !showSessions,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colorScheme.primary.copy(alpha = 0.12f),
                            selectedLabelColor = colorScheme.primary
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Chart container with animation
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(500)) + scaleIn(spring(stiffness = Spring.StiffnessLow)),
                exit = fadeOut(tween(300)) + scaleOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colorScheme.surface)
                        .padding(8.dp)
                ) {
                    if (trendData.isEmpty()) {
                        EmptyStateComponent(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        CartesianChartHost(
                            chart = rememberCartesianChart(
                                rememberLineCartesianLayer(),
                                startAxis = VerticalAxis.rememberStart(),
                                bottomAxis = HorizontalAxis.rememberBottom()
                            ),
                            modelProducer = modelProducer,
                            modifier = Modifier.fillMaxSize()
                        )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Chart summary stats
            if (trendData.isNotEmpty()) {
                ChartSummaryStats(
                    trendData = trendData,
                    showSessions = showSessions,
                    colorScheme = colorScheme,
                    typography = typography
                    )
                    }
                }
            }
    }
}

@Composable
private fun EmptyStateComponent(
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.TrendingUp,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = colorScheme.outline.copy(alpha = 0.6f)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "No usage data yet",
            style = typography.bodyMedium,
            color = colorScheme.outline,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = "Start transcribing to see your activity trend",
            style = typography.bodySmall,
            color = colorScheme.outline.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ChartSummaryStats(
    trendData: List<DailyUsage>,
    showSessions: Boolean,
    colorScheme: ColorScheme,
    typography: Typography
) {
    val totalSessions = trendData.sumOf { it.sessionsCount }
    val totalWords = trendData.sumOf { it.wordsTranscribed }
    val averageSessions = if (trendData.isNotEmpty()) totalSessions.toFloat() / trendData.size else 0f
    val averageWords = if (trendData.isNotEmpty()) totalWords.toFloat() / trendData.size else 0f
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem(
            label = "Total",
            value = if (showSessions) totalSessions.toString() else totalWords.toString(),
            unit = if (showSessions) "sessions" else "words",
            colorScheme = colorScheme,
            typography = typography
        )
        
        StatItem(
            label = "Daily Avg",
            value = String.format(Locale.getDefault(), "%.1f", if (showSessions) averageSessions else averageWords),
            unit = if (showSessions) "sessions" else "words",
            colorScheme = colorScheme,
            typography = typography
        )
        
        val peakValue = if (showSessions) {
            trendData.maxOfOrNull { it.sessionsCount } ?: 0
        } else {
            trendData.maxOfOrNull { it.wordsTranscribed } ?: 0L
        }
        
        StatItem(
            label = "Peak Day",
            value = peakValue.toString(),
            unit = if (showSessions) "sessions" else "words",
            colorScheme = colorScheme,
            typography = typography
        )
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    unit: String,
    colorScheme: ColorScheme,
    typography: Typography
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = typography.titleMedium,
            color = colorScheme.primary
        )
        Text(
            text = unit,
            style = typography.bodySmall,
            color = colorScheme.onSurfaceVariant
        )
        Text(
            text = label,
            style = typography.labelSmall,
            color = colorScheme.outline
        )
    }
}

