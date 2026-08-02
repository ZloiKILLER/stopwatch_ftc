package com.stopwatch.ftc.ui

/** A duration broken into fields so the UI can style the fractional part differently. */
internal data class TimeParts(
    val hours: String,
    val minutes: String,
    val seconds: String,
    val hundredths: String,
) {
    val hasHours: Boolean
        get() = hours != "00"
}

internal fun timePartsOf(millis: Long): TimeParts {
    val safe = millis.coerceAtLeast(0L)
    return TimeParts(
        hours = pad(safe / 3_600_000L),
        minutes = pad(safe / 60_000L % 60L),
        seconds = pad(safe / 1_000L % 60L),
        hundredths = pad(safe % 1_000L / 10L),
    )
}

internal fun formatDuration(millis: Long): String =
    with(timePartsOf(millis)) {
        if (hasHours) "$hours:$minutes:$seconds.$hundredths" else "$minutes:$seconds.$hundredths"
    }

/**
 * Padded without `String.format`, which would otherwise render digits in the default locale's
 * numbering system and turn the timer into non-Latin numerals in some languages.
 */
private fun pad(value: Long): String = value.toString().padStart(2, '0')
