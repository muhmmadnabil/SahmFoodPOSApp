package com.sahm.pos.domain.entity

data class TimeSyncInfo(
    val offsetMillis: Long,
    val serverTimeMillis: Long,
    val phoneTimeMillis: Long,
    val checkedAtMillis: Long,
)
