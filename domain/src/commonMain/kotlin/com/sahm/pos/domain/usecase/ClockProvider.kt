package com.sahm.pos.domain.usecase

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

fun interface ClockProvider {
    fun nowMillis(): Long
}

class SystemClockProvider : ClockProvider {
    @OptIn(ExperimentalTime::class)
    override fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()
}
