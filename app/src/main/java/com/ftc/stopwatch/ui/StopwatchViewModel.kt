package com.ftc.stopwatch.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ftc.stopwatch.data.StopwatchStore
import com.ftc.stopwatch.domain.Lap
import com.ftc.stopwatch.domain.Stopwatch
import com.ftc.stopwatch.domain.StopwatchStatus
import com.ftc.stopwatch.domain.SystemElapsedClock
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Which laps get the fastest / slowest treatment in the list. */
data class LapHighlights(val fastestLapNumber: Int? = null, val slowestLapNumber: Int? = null)

class StopwatchViewModel(
    private val stopwatch: Stopwatch,
    private val store: StopwatchStore,
) : ViewModel() {

    val status: StateFlow<StopwatchStatus> = stopwatch.status
    val laps: StateFlow<List<Lap>> = stopwatch.laps

    val highlights: StateFlow<LapHighlights> =
        stopwatch.laps
            .map(::highlightsOf)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LapHighlights())

    init {
        viewModelScope.launch {
            restoreSavedState()
            checkpointWhileRunning()
        }
    }

    /**
     * The current reading. Deliberately a plain function rather than a flow: the UI samples it once
     * per rendered frame, so pushing 60 values a second through a flow would only add allocations.
     */
    fun elapsedMillis(): Long = stopwatch.elapsedMillis()

    fun start() {
        stopwatch.start()
        persist()
    }

    fun pause() {
        stopwatch.pause()
        persist()
    }

    fun lap() {
        stopwatch.lap()
        persist()
    }

    fun reset() {
        stopwatch.reset()
        persist()
    }

    /** Called when the app leaves the foreground, the most likely moment before a process kill. */
    fun checkpoint() = persist()

    private suspend fun restoreSavedState() {
        val saved = store.load() ?: return
        // Ignore the saved state if the user already started timing while the load was in flight.
        if (stopwatch.status.value == StopwatchStatus.IDLE && stopwatch.laps.value.isEmpty()) {
            stopwatch.restore(saved)
        }
    }

    private suspend fun checkpointWhileRunning(): Nothing =
        stopwatch.status.collectLatest { status ->
            if (status != StopwatchStatus.RUNNING) return@collectLatest
            while (true) {
                delay(CHECKPOINT_INTERVAL_MILLIS)
                persist()
            }
        }

    private fun persist() {
        val snapshot = stopwatch.snapshot()
        viewModelScope.launch { store.save(snapshot) }
    }

    companion object {
        /** Highlighting the extremes of one or two laps says nothing useful. */
        private const val MIN_LAPS_FOR_HIGHLIGHTS = 3

        /**
         * Bounds how much timing a reboot can cost. Every user action is persisted immediately;
         * this only covers a device that dies mid-measurement with no interaction.
         */
        private const val CHECKPOINT_INTERVAL_MILLIS = 10_000L

        internal fun highlightsOf(laps: List<Lap>): LapHighlights {
            if (laps.size < MIN_LAPS_FOR_HIGHLIGHTS) return LapHighlights()
            val fastest = laps.minBy(Lap::lapMillis)
            val slowest = laps.maxBy(Lap::lapMillis)
            // All laps identical: nothing meaningful to single out.
            if (fastest.lapMillis == slowest.lapMillis) return LapHighlights()
            return LapHighlights(fastest.number, slowest.number)
        }

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                StopwatchViewModel(
                    stopwatch = Stopwatch(SystemElapsedClock),
                    store = StopwatchStore(application),
                )
            }
        }
    }
}
