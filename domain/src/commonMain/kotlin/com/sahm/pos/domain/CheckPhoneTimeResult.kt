package com.sahm.pos.domain

import com.sahm.pos.domain.entity.TimeSyncInfo

sealed interface CheckPhoneTimeResult {
    val info: TimeSyncInfo?

    data class Valid(override val info: TimeSyncInfo) : CheckPhoneTimeResult
    data class Invalid(override val info: TimeSyncInfo) : CheckPhoneTimeResult
    data class Failed(val error: DataError) : CheckPhoneTimeResult {
        override val info: TimeSyncInfo? = null
    }
}