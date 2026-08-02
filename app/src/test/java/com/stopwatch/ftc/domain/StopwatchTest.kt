package com.stopwatch.ftc.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StopwatchTest {

    private val clock = FakeClock()
    private val stopwatch = Stopwatch(clock)

    @Test
    fun `reads zero until started`() {
        assertEquals(StopwatchStatus.IDLE, stopwatch.status.value)
        assertEquals(0L, stopwatch.elapsedMillis())

        clock.advance(5_000L)

        assertEquals(0L, stopwatch.elapsedMillis())
    }

    @Test
    fun `tracks the clock while running`() {
        stopwatch.start()
        clock.advance(1_234L)

        assertEquals(StopwatchStatus.RUNNING, stopwatch.status.value)
        assertEquals(1_234L, stopwatch.elapsedMillis())
    }

    @Test
    fun `pause freezes the reading and resume carries it on`() {
        stopwatch.start()
        clock.advance(2_000L)
        stopwatch.pause()

        clock.advance(10_000L)
        assertEquals(2_000L, stopwatch.elapsedMillis())

        stopwatch.start()
        clock.advance(500L)
        assertEquals(2_500L, stopwatch.elapsedMillis())
    }

    @Test
    fun `reset clears time and laps`() {
        stopwatch.start()
        clock.advance(3_000L)
        stopwatch.lap()
        stopwatch.pause()

        stopwatch.reset()

        assertEquals(StopwatchStatus.IDLE, stopwatch.status.value)
        assertEquals(0L, stopwatch.elapsedMillis())
        assertEquals(emptyList<Lap>(), stopwatch.laps.value)
    }

    @Test
    fun `lap records the split and the running total, newest first`() {
        stopwatch.start()
        clock.advance(1_000L)
        stopwatch.lap()
        clock.advance(1_500L)
        stopwatch.lap()

        val laps = stopwatch.laps.value
        assertEquals(2, laps.size)
        assertEquals(Lap(number = 2, lapMillis = 1_500L, totalMillis = 2_500L), laps[0])
        assertEquals(Lap(number = 1, lapMillis = 1_000L, totalMillis = 1_000L), laps[1])
    }

    /** Regression: laps used to appear to stop once the list outgrew the visible area. */
    @Test
    fun `keeps recording laps far past a screenful`() {
        stopwatch.start()
        repeat(50) {
            clock.advance(1_000L)
            stopwatch.lap()
        }

        val laps = stopwatch.laps.value
        assertEquals(50, laps.size)
        assertEquals(50, laps.first().number)
        assertEquals(50_000L, laps.first().totalMillis)
        assertEquals(1_000L, laps.first().lapMillis)
        assertEquals(List(50) { 50 - it }, laps.map(Lap::number))
    }

    @Test
    fun `starting from idle drops the previous laps`() {
        stopwatch.start()
        clock.advance(1_000L)
        stopwatch.lap()
        stopwatch.pause()
        stopwatch.reset()

        stopwatch.start()

        assertEquals(emptyList<Lap>(), stopwatch.laps.value)
        assertEquals(0L, stopwatch.elapsedMillis())
    }

    @Test
    fun `lap and pause do nothing unless running`() {
        stopwatch.lap()
        stopwatch.pause()

        assertEquals(StopwatchStatus.IDLE, stopwatch.status.value)
        assertEquals(emptyList<Lap>(), stopwatch.laps.value)
    }

    @Test
    fun `restores a running measurement after the process is killed`() {
        stopwatch.start()
        clock.advance(4_000L)
        val snapshot = stopwatch.snapshot()

        clock.advance(6_000L)
        val revived = Stopwatch(clock)
        revived.restore(snapshot)

        assertEquals(StopwatchStatus.RUNNING, revived.status.value)
        assertEquals(10_000L, revived.elapsedMillis())
    }

    @Test
    fun `restores laps alongside the reading`() {
        stopwatch.start()
        clock.advance(1_000L)
        stopwatch.lap()
        clock.advance(1_000L)
        stopwatch.lap()
        stopwatch.pause()

        val revived = Stopwatch(clock)
        revived.restore(stopwatch.snapshot())

        assertEquals(StopwatchStatus.PAUSED, revived.status.value)
        assertEquals(2_000L, revived.elapsedMillis())
        assertEquals(stopwatch.laps.value, revived.laps.value)
    }

    /**
     * Regression: the saved uptime reference is meaningless after a restart, which used to produce
     * a negative elapsed time and render as garbage.
     */
    @Test
    fun `freezes instead of going negative when the device reboots mid-measurement`() {
        stopwatch.start()
        clock.advance(7_000L)
        val snapshot = stopwatch.snapshot()

        clock.reboot()
        val revived = Stopwatch(clock)
        revived.restore(snapshot)

        assertEquals(StopwatchStatus.PAUSED, revived.status.value)
        assertEquals(7_000L, revived.elapsedMillis())
        assertTrue(revived.elapsedMillis() >= 0L)
    }

    @Test
    fun `a reboot keeps the laps that were already recorded`() {
        stopwatch.start()
        clock.advance(2_000L)
        stopwatch.lap()
        val snapshot = stopwatch.snapshot()

        clock.reboot()
        val revived = Stopwatch(clock)
        revived.restore(snapshot)

        assertEquals(1, revived.laps.value.size)
        assertEquals(2_000L, revived.laps.value.first().totalMillis)
    }

    @Test
    fun `restoring an idle snapshot stays idle`() {
        val revived = Stopwatch(clock)
        revived.restore(StopwatchSnapshot())

        assertEquals(StopwatchStatus.IDLE, revived.status.value)
        assertEquals(0L, revived.elapsedMillis())
    }
}
