package me.shadykhalifa.whispertop.presentation.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun shimmerBrush(targetValue: Float = 1000f): Brush {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
        MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnimation = transition.animateFloat(
        initialValue = 0f,
        targetValue = targetValue,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Restart
        ), label = "shimmer"
    )
    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnimation.value, y = translateAnimation.value)
    )
}

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp)
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(shimmerBrush())
            .semantics {
                contentDescription = "Loading content"
            }
    )
}

@Composable
fun ShimmerStatCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .semantics {
                contentDescription = "Loading statistics card"
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ShimmerBox(
                modifier = Modifier
                    .height(24.dp)
                    .fillMaxWidth(0.6f)
            )
            ShimmerBox(
                modifier = Modifier
                    .height(32.dp)
                    .fillMaxWidth(0.4f)
            )
            ShimmerBox(
                modifier = Modifier
                    .height(16.dp)
                    .fillMaxWidth(0.8f)
            )
        }
    }
}

@Composable
fun ShimmerTranscriptionCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Loading transcription item"
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShimmerBox(
                    modifier = Modifier
                        .height(16.dp)
                        .fillMaxWidth(0.3f)
                )
                ShimmerBox(
                    modifier = Modifier
                        .size(24.dp),
                    shape = CircleShape
                )
            }
            
            repeat(3) { index ->
                ShimmerBox(
                    modifier = Modifier
                        .height(16.dp)
                        .fillMaxWidth(
                            when (index) {
                                0 -> 1f
                                1 -> 0.9f
                                else -> 0.6f
                            }
                        )
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(2) {
                    ShimmerBox(
                        modifier = Modifier
                            .height(12.dp)
                            .weight(1f),
                        shape = RoundedCornerShape(6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ShimmerDashboardStats(
    modifier: Modifier = Modifier,
    columns: Int = 2
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(6) {
            ShimmerStatCard()
        }
    }
}

@Composable
fun ShimmerHistoryList(
    modifier: Modifier = Modifier,
    itemCount: Int = 5
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(itemCount) {
            ShimmerTranscriptionCard()
        }
    }
}

@Composable
fun ShimmerChart(
    modifier: Modifier = Modifier,
    height: Dp = 200.dp
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .semantics {
                contentDescription = "Loading chart"
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ShimmerBox(
                modifier = Modifier
                    .height(20.dp)
                    .fillMaxWidth(0.4f)
            )
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                repeat(5) { index ->
                    val heightFraction = (0.3f + (index * 0.15f)).coerceAtMost(1f)
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxHeight(heightFraction)
                            .width(40.dp)
                            .offset(x = (index * 60).dp)
                            .align(Alignment.BottomStart),
                        shape = RoundedCornerShape(4.dp)
                    )
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                repeat(5) {
                    ShimmerBox(
                        modifier = Modifier
                            .height(12.dp)
                            .width(30.dp),
                        shape = RoundedCornerShape(6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ShimmerProfileSection(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Loading profile section"
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShimmerBox(
                modifier = Modifier.size(48.dp),
                shape = CircleShape
            )
            
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                ShimmerBox(
                    modifier = Modifier
                        .height(20.dp)
                        .fillMaxWidth(0.6f)
                )
                ShimmerBox(
                    modifier = Modifier
                        .height(16.dp)
                        .fillMaxWidth(0.4f)
                )
            }
            
            ShimmerBox(
                modifier = Modifier
                    .size(24.dp),
                shape = CircleShape
            )
        }
    }
}

@Composable
fun ShimmerListItem(
    modifier: Modifier = Modifier,
    showIcon: Boolean = true,
    showSecondaryText: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .semantics {
                contentDescription = "Loading list item"
            },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showIcon) {
            ShimmerBox(
                modifier = Modifier.size(24.dp),
                shape = CircleShape
            )
        }
        
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ShimmerBox(
                modifier = Modifier
                    .height(16.dp)
                    .fillMaxWidth(0.7f)
            )
            
            if (showSecondaryText) {
                ShimmerBox(
                    modifier = Modifier
                        .height(14.dp)
                        .fillMaxWidth(0.5f)
                )
            }
        }
        
        ShimmerBox(
            modifier = Modifier
                .size(20.dp),
            shape = CircleShape
        )
    }
}