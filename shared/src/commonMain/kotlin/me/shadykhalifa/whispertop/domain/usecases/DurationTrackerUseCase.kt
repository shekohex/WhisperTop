package me.shadykhalifa.whispertop.domain.usecases

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.delay
import me.shadykhalifa.whispertop.data.audio.getCurrentTimeMillis
import me.shadykhalifa.whispertop.domain.managers.RecordingManager
import me.shadykhalifa.whispertop.domain.models.RecordingState

class DurationTrackerUseCase(
    private val recordingManager: RecordingManager
) {
    operator fun invoke(): Flow<Long> {
        // Create a ticker flow that emits every 100ms
        val ticker = flow {
            while (true) {
                emit(Unit)
                delay(100)
            }
        }
        
        return combine(recordingManager.recordingState, ticker) { state, _ ->
            when (state) {
                is RecordingState.Recording -> getCurrentTimeMillis() - state.startTime
                else -> 0L
            }
        }
    }
}