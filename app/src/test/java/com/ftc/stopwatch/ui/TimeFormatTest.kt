package com.ftc.stopwatch.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatTest {

    @Test
    fun `formats minutes, seconds and hundredths`() {
        assertEquals("01:05.30", formatDuration(65_300L))
    }

    @Test
    fun `pads every field`() {
        assertEquals("00:00.00", formatDuration(0L))
        assertEquals("00:00.09", formatDuration(90L))
    }

    @Test
    fun `adds an hours field only once past an hour`() {
        assertEquals("59:59.99", formatDuration(3_599_990L))
        assertEquals("01:00:00.00", formatDuration(3_600_000L))
    }

    @Test
    fun `keeps counting hours past a day`() {
        assertEquals("25:00:00.00", formatDuration(90_000_000L))
    }

    @Test
    fun `clamps a negative reading to zero rather than rendering minus signs`() {
        assertEquals("00:00.00", formatDuration(-5_000L))
    }

    @Test
    fun `exposes the hours flag so the UI can resize the readout`() {
        assertEquals(false, timePartsOf(60_000L).hasHours)
        assertEquals(true, timePartsOf(3_600_000L).hasHours)
    }
}
