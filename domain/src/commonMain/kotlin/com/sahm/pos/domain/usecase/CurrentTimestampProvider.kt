package com.sahm.pos.domain.usecase

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

fun interface CurrentTimestampProvider {
    fun now(): String
}

class SystemCurrentTimestampProvider : CurrentTimestampProvider {
    @OptIn(ExperimentalTime::class)
    override fun now(): String = Clock.System.now().toString()
}
