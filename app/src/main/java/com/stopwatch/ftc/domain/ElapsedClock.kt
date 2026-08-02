package com.stopwatch.ftc.domain

import android.os.SystemClock

/**
 * Time source behind the stopwatch, split out from [SystemClock] so that [Stopwatch] can be driven
 * by a fake clock in unit tests.
 */
interface ElapsedClock {

    /** Milliseconds since boot. Monotonic, so changing the system time cannot skew a measurement. */
    fun uptimeMillis(): Long

    /** Milliseconds since the epoch. Only used to notice that the device rebooted. */
    fun wallClockMillis(): Long
}

/** The real device clock. */
object SystemElapsedClock : ElapsedClock {
    override fun uptimeMillis(): Long = SystemClock.elapsedRealtime()

    override fun wallClockMillis(): Long = System.currentTimeMillis()
}
