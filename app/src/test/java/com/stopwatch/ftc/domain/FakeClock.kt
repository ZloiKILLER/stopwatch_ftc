package com.stopwatch.ftc.domain

/** Hand-driven [ElapsedClock] so timing behaviour can be asserted without real waiting. */
class FakeClock(
    private var uptime: Long = 300_000L,
    private var wallClock: Long = 1_800_000_000_000L,
) : ElapsedClock {

    override fun uptimeMillis(): Long = uptime

    override fun wallClockMillis(): Long = wallClock

    fun advance(millis: Long) {
        uptime += millis
        wallClock += millis
    }

    /** Models a restart: uptime returns to zero while wall-clock time keeps moving. */
    fun reboot(downtimeMillis: Long = 45_000L) {
        wallClock += downtimeMillis
        uptime = 0L
    }
}
