package me.shadykhalifa.whispertop.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.datetime.Clock
import me.shadykhalifa.whispertop.domain.models.TranscriptionHistory
import me.shadykhalifa.whispertop.presentation.components.TranscriptionHistoryItem
import me.shadykhalifa.whispertop.ui.theme.WhisperTopTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class UiBenchmark {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @get:Rule
    val composeTestRule = createComposeRule()

    private val random = Random(123) // Fixed seed for reproducible benchmarks

    @Test
    fun benchmarkSimpleComposition() {
        benchmarkRule.measureRepeated {
            composeTestRule.setContent {
                WhisperTopTheme {
                    Surface {
                        Text("Benchmark Test")
                    }
                }
            }
        }
    }

    @Test
    fun benchmarkMaterialTheming() {
        benchmarkRule.measureRepeated {
            composeTestRule.setContent {
                WhisperTopTheme {
                    MaterialTheme {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            Text(
                                text = "Themed Content",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun benchmarkSmallList_rendering() {
        val items = generateTranscriptionHistory(10)
        
        benchmarkRule.measureRepeated {
            composeTestRule.setContent {
                WhisperTopTheme {
                    TranscriptionHistoryList(items = items)
                }
            }
        }
    }

    @Test
    fun benchmarkMediumList_rendering() {
        val items = generateTranscriptionHistory(50)
        
        benchmarkRule.measureRepeated {
            composeTestRule.setContent {
                WhisperTopTheme {
                    TranscriptionHistoryList(items = items)
                }
            }
        }
    }

    @Test
    fun benchmarkLargeList_rendering() {
        val items = generateTranscriptionHistory(200)
        
        benchmarkRule.measureRepeated {
            composeTestRule.setContent {
                WhisperTopTheme {
                    TranscriptionHistoryList(items = items)
                }
            }
        }
    }

    @Test
    fun benchmarkList_scrolling() {
        val items = generateTranscriptionHistory(100)
        
        composeTestRule.setContent {
            WhisperTopTheme {
                LazyColumn(
                    modifier = Modifier.testTag("transcription_list")
                ) {
                    items(items) { item ->
                        TranscriptionHistoryItem(
                            transcription = item,
                            onItemClick = { },
                            onDeleteClick = { }
                        )
                    }
                }
            }
        }

        benchmarkRule.measureRepeated {
            composeTestRule.onNodeWithTag("transcription_list")
                .performScrollToIndex(50)
        }
    }

    @Test
    fun benchmarkComplexItem_rendering() {
        val complexItems = generateComplexTranscriptionHistory(20)
        
        benchmarkRule.measureRepeated {
            composeTestRule.setContent {
                WhisperTopTheme {
                    LazyColumn {
                        items(complexItems) { item ->
                            TranscriptionHistoryItem(
                                transcription = item,
                                onItemClick = { },
                                onDeleteClick = { }
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun benchmarkStateUpdates() {
        benchmarkRule.measureRepeated {
            composeTestRule.setContent {
                WhisperTopTheme {
                    StatefulCounter()
                }
            }
            
            // Trigger multiple state updates
            repeat(10) {
                composeTestRule.onNodeWithText("Increment").performClick()
            }
        }
    }

    @Test
    fun benchmarkRecomposition_frequency() {
        composeTestRule.setContent {
            WhisperTopTheme {
                FrequentRecompositionComponent()
            }
        }

        benchmarkRule.measureRepeated {
            // Trigger recomposition by clicking
            composeTestRule.onNodeWithText("Update").performClick()
        }
    }

    @Test
    fun benchmarkLazyColumn_itemRemoval() {
        val initialItems = generateTranscriptionHistory(50)
        
        composeTestRule.setContent {
            WhisperTopTheme {
                RemovableItemsList(initialItems = initialItems)
            }
        }

        benchmarkRule.measureRepeated {
            // Remove first item (triggers recomposition)
            composeTestRule.onNodeWithText("Remove First").performClick()
        }
    }

    @Test
    fun benchmarkAnimatedContent() {
        benchmarkRule.measureRepeated {
            composeTestRule.setContent {
                WhisperTopTheme {
                    AnimatedVisibilityComponent()
                }
            }
            
            // Trigger animation
            composeTestRule.onNodeWithText("Toggle").performClick()
        }
    }

    @Test
    fun benchmarkNestedComposition() {
        benchmarkRule.measureRepeated {
            composeTestRule.setContent {
                WhisperTopTheme {
                    DeepNestedComponent(depth = 10)
                }
            }
        }
    }

    @Test
    fun benchmarkComplexLayout_measurement() {
        val items = generateTranscriptionHistory(30)
        
        benchmarkRule.measureRepeated {
            composeTestRule.setContent {
                WhisperTopTheme {
                    ComplexLayoutComponent(items = items)
                }
            }
        }
    }

    // Helper Composables

    @Composable
    private fun TranscriptionHistoryList(items: List<TranscriptionHistory>) {
        LazyColumn {
            items(items) { item ->
                TranscriptionHistoryItem(
                    transcription = item,
                    onItemClick = { },
                    onDeleteClick = { }
                )
            }
        }
    }

    @Composable
    private fun StatefulCounter() {
        var count by remember { mutableStateOf(0) }
        
        androidx.compose.foundation.layout.Column {
            Text("Count: $count")
            androidx.compose.material3.Button(
                onClick = { count++ }
            ) {
                Text("Increment")
            }
        }
    }

    @Composable
    private fun FrequentRecompositionComponent() {
        var value by remember { mutableStateOf(0) }
        
        androidx.compose.foundation.layout.Column {
            repeat(20) { index ->
                Text("Value $index: ${value + index}")
            }
            androidx.compose.material3.Button(
                onClick = { value = random.nextInt() }
            ) {
                Text("Update")
            }
        }
    }

    @Composable
    private fun RemovableItemsList(initialItems: List<TranscriptionHistory>) {
        var items by remember { mutableStateOf(initialItems) }
        
        androidx.compose.foundation.layout.Column {
            androidx.compose.material3.Button(
                onClick = { 
                    if (items.isNotEmpty()) {
                        items = items.drop(1)
                    }
                }
            ) {
                Text("Remove First")
            }
            
            LazyColumn {
                items(items) { item ->
                    TranscriptionHistoryItem(
                        transcription = item,
                        onItemClick = { },
                        onDeleteClick = { }
                    )
                }
            }
        }
    }

    @Composable
    private fun AnimatedVisibilityComponent() {
        var visible by remember { mutableStateOf(true) }
        
        androidx.compose.foundation.layout.Column {
            androidx.compose.material3.Button(
                onClick = { visible = !visible }
            ) {
                Text("Toggle")
            }
            
            androidx.compose.animation.AnimatedVisibility(visible = visible) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text("Animated Content")
                }
            }
        }
    }

    @Composable
    private fun DeepNestedComponent(depth: Int) {
        if (depth > 0) {
            androidx.compose.foundation.layout.Box {
                DeepNestedComponent(depth - 1)
            }
        } else {
            Text("Depth 0")
        }
    }

    @Composable
    private fun ComplexLayoutComponent(items: List<TranscriptionHistory>) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxSize()
        ) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.weight(1f)
            ) {
                LazyColumn {
                    items(items.take(items.size / 2)) { item ->
                        TranscriptionHistoryItem(
                            transcription = item,
                            onItemClick = { },
                            onDeleteClick = { }
                        )
                    }
                }
            }
            
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.weight(1f)
            ) {
                LazyColumn {
                    items(items.drop(items.size / 2)) { item ->
                        TranscriptionHistoryItem(
                            transcription = item,
                            onItemClick = { },
                            onDeleteClick = { }
                        )
                    }
                }
            }
        }
    }

    // Helper functions for generating test data

    private fun generateTranscriptionHistory(count: Int): List<TranscriptionHistory> {
        val now = Clock.System.now().toEpochMilliseconds()
        return (0 until count).map { index ->
            TranscriptionHistory(
                id = "bench_$index",
                text = "Benchmark transcription $index with some content",
                timestamp = now - (index * 60000L), // 1 minute apart
                duration = random.nextFloat() * 30f,
                confidence = 0.8f + random.nextFloat() * 0.2f,
                language = "en",
                model = "whisper-1",
                wordCount = random.nextInt(10, 50)
            )
        }
    }

    private fun generateComplexTranscriptionHistory(count: Int): List<TranscriptionHistory> {
        val now = Clock.System.now().toEpochMilliseconds()
        return (0 until count).map { index ->
            TranscriptionHistory(
                id = "complex_bench_$index",
                text = "This is a much longer transcription text for benchmark $index. " +
                      "It contains multiple sentences and various punctuation marks! " +
                      "This helps test rendering performance with longer content. " +
                      "The text includes numbers like ${random.nextInt(1000)} and symbols @#$.",
                timestamp = now - (index * 30000L), // 30 seconds apart
                duration = random.nextFloat() * 180f, // 0-3 minutes
                confidence = random.nextFloat(),
                language = listOf("en", "es", "fr", "de").random(),
                model = listOf("whisper-1", "whisper-2", "whisper-large").random(),
                wordCount = random.nextInt(50, 200),
                customPrompt = if (random.nextBoolean()) "Custom prompt for $index" else null,
                audioFilePath = "/path/to/audio_$index.wav"
            )
        }
    }
}