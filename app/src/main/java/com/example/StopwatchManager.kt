package com.example

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Represent a single lap recorded by the user.
 */
data class Lap(
    val id: Int,
    val lapTime: Long,    // Duration of this specific lap
    val totalTime: Long   // Total accumulated time at the moment of lap recording
)

enum class StopwatchState {
    IDLE, RUNNING, PAUSED
}

/**
 * Singleton stopwatch engine that manages time, state, and laps.
 * Uses SystemClock.elapsedRealtime() to prevent drift.
 * Persists state to SharedPreferences to survive Activity or process destruction.
 */
object StopwatchManager {
    private const val PREFS_NAME = "stopwatch_prefs"
    private const val KEY_STATE = "state"
    private const val KEY_START_TIME = "start_time"
    private const val KEY_ACCUMULATED_TIME = "accumulated_time"
    private const val KEY_LAPS = "laps"

    private lateinit var prefs: SharedPreferences
    private var isInitialized = false

    private val _state = MutableStateFlow(StopwatchState.IDLE)
    val state: StateFlow<StopwatchState> = _state.asStateFlow()

    private val _elapsedTime = MutableStateFlow(0L)
    val elapsedTime: StateFlow<Long> = _elapsedTime.asStateFlow()

    private val _laps = MutableStateFlow<List<Lap>>(emptyList())
    val laps: StateFlow<List<Lap>> = _laps.asStateFlow()

    private var startTime = 0L
    private var accumulatedTime = 0L

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var tickJob: Job? = null

    /**
     * Initializes the manager with a context to load saved state.
     */
    fun init(context: Context) {
        if (isInitialized) return
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        isInitialized = true
        loadState()
    }

    /**
     * Starts or resumes the stopwatch.
     */
    fun start() {
        if (_state.value == StopwatchState.RUNNING) return

        if (_state.value == StopwatchState.IDLE) {
            startTime = SystemClock.elapsedRealtime()
            accumulatedTime = 0L
            _laps.value = emptyList()
        } else if (_state.value == StopwatchState.PAUSED) {
            startTime = SystemClock.elapsedRealtime()
        }

        _state.value = StopwatchState.RUNNING
        saveState()
        startTickJob()
    }

    /**
     * Pauses the stopwatch.
     */
    fun pause() {
        if (_state.value != StopwatchState.RUNNING) return

        tickJob?.cancel()
        accumulatedTime += SystemClock.elapsedRealtime() - startTime
        _state.value = StopwatchState.PAUSED
        _elapsedTime.value = accumulatedTime
        saveState()
    }

    /**
     * Records a lap.
     */
    fun lap() {
        if (_state.value != StopwatchState.RUNNING) return

        val total = getCalculatedElapsedTime()
        val currentLaps = _laps.value
        val lapNumber = currentLaps.size + 1

        val previousTotal = if (currentLaps.isEmpty()) 0L else currentLaps.first().totalTime
        val lapDuration = total - previousTotal

        val newLap = Lap(
            id = lapNumber,
            lapTime = lapDuration,
            totalTime = total
        )

        // Insert new lap at the beginning so the latest lap is shown at the top of the list
        _laps.value = listOf(newLap) + currentLaps
        saveState()
    }

    /**
     * Resets the stopwatch to initial state.
     */
    fun reset() {
        tickJob?.cancel()
        startTime = 0L
        accumulatedTime = 0L
        _state.value = StopwatchState.IDLE
        _elapsedTime.value = 0L
        _laps.value = emptyList()
        saveState()
    }

    /**
     * Calculates the exact elapsed time based on monotonic system clock.
     */
    private fun getCalculatedElapsedTime(): Long {
        return when (_state.value) {
            StopwatchState.IDLE -> 0L
            StopwatchState.PAUSED -> accumulatedTime
            StopwatchState.RUNNING -> accumulatedTime + (SystemClock.elapsedRealtime() - startTime)
        }
    }

    private fun startTickJob() {
        tickJob?.cancel()
        tickJob = scope.launch {
            while (isActive) {
                _elapsedTime.value = getCalculatedElapsedTime()
                delay(15) // Balance responsiveness and energy-efficiency (approx 60fps updates)
            }
        }
    }

    /**
     * Saves the current state to SharedPreferences to prevent loss across process deaths.
     */
    private fun saveState() {
        if (!isInitialized) return
        val lapsString = _laps.value.joinToString(",") { "${it.id}:${it.lapTime}:${it.totalTime}" }
        prefs.edit().apply {
            putString(KEY_STATE, _state.value.name)
            putLong(KEY_START_TIME, startTime)
            putLong(KEY_ACCUMULATED_TIME, accumulatedTime)
            putString(KEY_LAPS, lapsString)
            apply()
        }
    }

    /**
     * Loads the saved state from SharedPreferences.
     */
    private fun loadState() {
        val stateName = prefs.getString(KEY_STATE, StopwatchState.IDLE.name) ?: StopwatchState.IDLE.name
        val loadedState = try {
            StopwatchState.valueOf(stateName)
        } catch (e: Exception) {
            StopwatchState.IDLE
        }

        startTime = prefs.getLong(KEY_START_TIME, 0L)
        accumulatedTime = prefs.getLong(KEY_ACCUMULATED_TIME, 0L)

        val lapsString = prefs.getString(KEY_LAPS, "") ?: ""
        val loadedLaps = if (lapsString.isNotEmpty()) {
            lapsString.split(",").mapNotNull { lapStr ->
                val parts = lapStr.split(":")
                if (parts.size == 3) {
                    Lap(
                        id = parts[0].toIntOrNull() ?: 0,
                        lapTime = parts[1].toLongOrNull() ?: 0L,
                        totalTime = parts[2].toLongOrNull() ?: 0L
                    )
                } else null
            }
        } else emptyList()

        _laps.value = loadedLaps
        _state.value = loadedState
        _elapsedTime.value = getCalculatedElapsedTime()

        if (loadedState == StopwatchState.RUNNING) {
            startTickJob()
        }
    }
}
