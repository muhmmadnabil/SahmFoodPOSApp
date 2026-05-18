package com.sahm.pos.domain

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

fun interface CurrentEpochMillisProvider {
    fun now(): Long
}

class SystemCurrentEpochMillisProvider : CurrentEpochMillisProvider {
    @OptIn(ExperimentalTime::class)
    override fun now(): Long = Clock.System.now().toEpochMilliseconds()
}
