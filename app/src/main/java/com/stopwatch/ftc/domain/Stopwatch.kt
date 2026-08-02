package com.stopwatch.ftc.domain

import kotlin.math.abs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The stopwatch itself: status, laps, and the arithmetic that turns clock readings into an elapsed
 * duration. Holds no Android dependencies beyond [ElapsedClock], so it is exercised directly by
 * unit tests.
 *
 * Elapsed time is never stored as a running counter. It is always derived from [ElapsedClock]
 * on demand, which is what keeps the reading accurate no matter how irregularly the UI samples it.
 */
class Stopwatch(private val clock: ElapsedClock) {

    private val _status = MutableStateFlow(StopwatchStatus.IDLE)
    val status: StateFlow<StopwatchStatus> = _status.asStateFlow()

    private val _laps = MutableStateFlow<List<Lap>>(emptyList())
    val laps: StateFlow<List<Lap>> = _laps.asStateFlow()

    /** Time banked by previously completed run legs. */
    private var committedMillis = 0L

    /** [ElapsedClock.uptimeMillis] at which the current run leg started. */
    private var legStartedAt = 0L

    fun elapsedMillis(): Long =
        when (_status.value) {
            StopwatchStatus.IDLE -> 0L
            StopwatchStatus.PAUSED -> committedMillis
            StopwatchStatus.RUNNING ->
                // coerceAtLeast guards against a clock that somehow reads backwards: a stopwatch
                // that stalls is recoverable, one that shows a negative time is not.
                (committedMillis + (clock.uptimeMillis() - legStartedAt)).coerceAtLeast(committedMillis)
        }

    fun start() {
        if (_status.value == StopwatchStatus.RUNNING) return
        if (_status.value == StopwatchStatus.IDLE) {
            committedMillis = 0L
            _laps.value = emptyList()
        }
        legStartedAt = clock.uptimeMillis()
        _status.value = StopwatchStatus.RUNNING
    }

    fun pause() {
        if (_status.value != StopwatchStatus.RUNNING) return
        committedMillis = elapsedMillis()
        legStartedAt = 0L
        _status.value = StopwatchStatus.PAUSED
    }

    fun lap() {
        if (_status.value != StopwatchStatus.RUNNING) return
        val total = elapsedMillis()
        val recorded = _laps.value
        val previousTotal = recorded.firstOrNull()?.totalMillis ?: 0L
        // Newest first, so the list reads like a race log with the latest split on top.
        _laps.value =
            buildList(recorded.size + 1) {
                add(
                    Lap(
                        number = recorded.size + 1,
                        lapMillis = total - previousTotal,
                        totalMillis = total,
                    )
                )
                addAll(recorded)
            }
    }

    fun reset() {
        committedMillis = 0L
        legStartedAt = 0L
        _laps.value = emptyList()
        _status.value = StopwatchStatus.IDLE
    }

    fun snapshot(): StopwatchSnapshot {
        val now = clock.uptimeMillis()
        return StopwatchSnapshot(
            status = _status.value,
            elapsedMillis = elapsedMillis(),
            capturedAtUptime = now,
            bootMarker = clock.wallClockMillis() - now,
            laps = _laps.value,
        )
    }

    fun restore(snapshot: StopwatchSnapshot) {
        _laps.value = snapshot.laps
        committedMillis = snapshot.elapsedMillis
        legStartedAt = 0L

        when (snapshot.status) {
            StopwatchStatus.IDLE -> {
                committedMillis = 0L
                _laps.value = emptyList()
                _status.value = StopwatchStatus.IDLE
            }
            StopwatchStatus.PAUSED -> _status.value = StopwatchStatus.PAUSED
            StopwatchStatus.RUNNING ->
                if (isSameBootSession(snapshot)) {
                    legStartedAt = snapshot.capturedAtUptime
                    _status.value = StopwatchStatus.RUNNING
                } else {
                    // The device rebooted, or the system clock was moved, while the stopwatch was
                    // running. The saved uptime reference means nothing now, so freeze at the last
                    // checkpoint instead of reporting a nonsense duration.
                    _status.value = StopwatchStatus.PAUSED
                }
        }
    }

    private fun isSameBootSession(snapshot: StopwatchSnapshot): Boolean {
        val now = clock.uptimeMillis()
        if (now < snapshot.capturedAtUptime) return false
        val bootMarker = clock.wallClockMillis() - now
        return abs(bootMarker - snapshot.bootMarker) <= BOOT_MARKER_TOLERANCE_MILLIS
    }

    private companion object {
        /**
         * How far the boot marker may drift before we assume a reboot. Comfortably above the sub
         * second corrections an NTP sync applies, and below any realistic reboot time.
         */
        const val BOOT_MARKER_TOLERANCE_MILLIS = 60_000L
    }
}
