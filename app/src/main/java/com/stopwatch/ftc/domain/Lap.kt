package com.stopwatch.ftc.domain

import kotlinx.serialization.Serializable

/** A single lap recorded while the stopwatch was running. */
@Serializable
data class Lap(
    /** 1-based position in the order the laps were recorded. */
    val number: Int,
    /** Duration of this lap on its own. */
    val lapMillis: Long,
    /** Total elapsed time at the moment the lap was recorded. */
    val totalMillis: Long,
)
