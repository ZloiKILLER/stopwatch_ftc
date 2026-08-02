package com.ftc.stopwatch.ui

import com.ftc.stopwatch.domain.Lap
import org.junit.Assert.assertEquals
import org.junit.Test

class LapHighlightsTest {

    @Test
    fun `no highlights below three laps`() {
        assertEquals(LapHighlights(), highlightsOf(lapsOf(1_000L, 2_000L)))
    }

    @Test
    fun `picks the fastest and the slowest lap`() {
        val highlights = highlightsOf(lapsOf(1_500L, 900L, 2_400L))

        assertEquals(2, highlights.fastestLapNumber)
        assertEquals(3, highlights.slowestLapNumber)
    }

    @Test
    fun `identical laps leave nothing to single out`() {
        assertEquals(LapHighlights(), highlightsOf(lapsOf(1_000L, 1_000L, 1_000L)))
    }

    @Test
    fun `ties resolve to a single fastest and a single slowest`() {
        val highlights = highlightsOf(lapsOf(1_000L, 1_000L, 2_000L, 2_000L))

        assertEquals(1, highlights.fastestLapNumber)
        assertEquals(3, highlights.slowestLapNumber)
    }

    private fun highlightsOf(laps: List<Lap>) = StopwatchViewModel.highlightsOf(laps)

    private fun lapsOf(vararg lapMillis: Long): List<Lap> {
        var total = 0L
        return lapMillis.mapIndexed { index, millis ->
            total += millis
            Lap(number = index + 1, lapMillis = millis, totalMillis = total)
        }
    }
}
