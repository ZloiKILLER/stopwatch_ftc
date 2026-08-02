package com.ftc.stopwatch.domain

enum class StopwatchStatus {
    /** Never started, or reset back to zero. */
    IDLE,
    RUNNING,
    PAUSED,
}
