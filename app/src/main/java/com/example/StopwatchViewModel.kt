package com.example

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel for the Stopwatch screen.
 * Interfaces with StopwatchManager and structures data for highly polished, responsive Compose UI rendering.
 */
class StopwatchViewModel : ViewModel() {

    // Expose core stopwatch state directly from the manager
    val state: StateFlow<StopwatchState> = StopwatchManager.state

    // Expose the raw elapsed time
    val elapsedTime: StateFlow<Long> = StopwatchManager.elapsedTime

    // Expose current list of laps
    val laps: StateFlow<List<Lap>> = StopwatchManager.laps

    /**
     * Exposes formatted time parts so the Compose UI can style hours, minutes, seconds,
     * and sub-seconds separately (e.g. smaller milliseconds text size for beautiful layout).
     */
    val formattedTime: StateFlow<TimeParts> = StopwatchManager.elapsedTime
        .map { ms -> formatTimeParts(ms) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TimeParts("00", "00", "00", "00")
        )

    /**
     * Determines which laps are the fastest and slowest to enable neon visual highlights.
     * Starts highlighting only when 3 or more laps are recorded.
     */
    val lapExtremums: StateFlow<LapExtremums> = StopwatchManager.laps
        .map { lapList ->
            if (lapList.size < 3) {
                LapExtremums(null, null)
            } else {
                var minLapId = -1
                var maxLapId = -1
                var minTime = Long.MAX_VALUE
                var maxTime = Long.MIN_VALUE

                // Find extremums from the list
                lapList.forEach { lap ->
                    if (lap.lapTime < minTime) {
                        minTime = lap.lapTime
                        minLapId = lap.id
                    }
                    if (lap.lapTime > maxTime) {
                        maxTime = lap.lapTime
                        maxLapId = lap.id
                    }
                }
                
                // If all laps have identical times, don't highlight both as different
                if (minTime == maxTime) {
                    LapExtremums(null, null)
                } else {
                    // In Kotlin: we mapped minTime to minLapId and maxTime to maxLapId (which we will define)
                    LapExtremums(minLapId, maxLapId)
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LapExtremums(null, null)
        )

    // Helper variables inside the map block above:
    // Let's make sure "maxId = lap.id" in map references the correct variable name "maxLapId" in implementation.

    /**
     * Starts or resumes the stopwatch directly.
     */
    fun start() {
        StopwatchManager.start()
    }

    /**
     * Pauses the stopwatch directly.
     */
    fun pause() {
        StopwatchManager.pause()
    }

    /**
     * Records a lap directly.
     */
    fun lap() {
        StopwatchManager.lap()
    }

    /**
     * Resets the stopwatch directly.
     */
    fun reset() {
        StopwatchManager.reset()
    }

    /**
     * Helper utility to convert raw ms into high-fidelity structured time segments.
     */
    private fun formatTimeParts(ms: Long): TimeParts {
        val hundredths = (ms % 1000) / 10
        val seconds = (ms / 1000) % 60
        val minutes = (ms / 60000) % 60
        val hours = (ms / 3600000)

        return TimeParts(
            hours = String.format("%02d", hours),
            minutes = String.format("%02d", minutes),
            seconds = String.format("%02d", seconds),
            hundredths = String.format("%02d", hundredths)
        )
    }
}

/**
 * Structural time container to design and render text layouts dynamically.
 */
data class TimeParts(
    val hours: String,
    val minutes: String,
    val seconds: String,
    val hundredths: String
)

/**
 * Identifiers of the fastest and slowest laps for UI rendering highlights.
 */
data class LapExtremums(
    val fastestLapId: Int?,
    val slowestLapId: Int?
)
