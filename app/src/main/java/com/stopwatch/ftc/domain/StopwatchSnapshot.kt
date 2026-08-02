package com.stopwatch.ftc.domain

import kotlinx.serialization.Serializable

/**
 * Serialisable form of the stopwatch, persisted so a measurement survives the process being killed.
 *
 * [capturedAtUptime] is only meaningful within a single boot session, so [bootMarker] is stored
 * alongside it: it is the offset between wall-clock time and uptime, which shifts by the reboot
 * downtime whenever the device restarts. Comparing it on restore is what stops the stopwatch from
 * reporting a negative duration after a reboot.
 */
@Serializable
data class StopwatchSnapshot(
    val status: StopwatchStatus = StopwatchStatus.IDLE,
    /** Total elapsed time at the moment this snapshot was taken. */
    val elapsedMillis: Long = 0L,
    /** [ElapsedClock.uptimeMillis] when this snapshot was taken. */
    val capturedAtUptime: Long = 0L,
    /** `wallClockMillis - uptimeMillis` when this snapshot was taken. */
    val bootMarker: Long = 0L,
    val laps: List<Lap> = emptyList(),
)
