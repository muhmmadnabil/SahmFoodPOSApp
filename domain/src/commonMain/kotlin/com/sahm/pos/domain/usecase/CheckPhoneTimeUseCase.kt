package com.sahm.pos.domain.usecase

import com.sahm.pos.domain.ClockProvider
import com.sahm.pos.domain.results.CheckPhoneTimeResult
import com.sahm.pos.domain.DataError
import com.sahm.pos.domain.entity.TimeSyncInfo
import com.sahm.pos.domain.repository.SyncDataRepo
import kotlin.math.abs

class CheckPhoneTimeUseCase(
    private val syncDataRepo: SyncDataRepo,
    private val clockProvider: ClockProvider,
) {
    suspend operator fun invoke(): CheckPhoneTimeResult {
        val phoneTimeBefore = clockProvider.nowMillis()
        val serverTime = syncDataRepo.getServerTimeStamp()

        if (serverTime == null || serverTime <= 0) {
            return CheckPhoneTimeResult.Failed(DataError.Remote.INVALID_REMOTE_DATA)
        }

        val phoneTimeAfter = clockProvider.nowMillis()
        val estimatedPhoneTime = (phoneTimeBefore + phoneTimeAfter) / 2
        val offsetMillis = serverTime - estimatedPhoneTime
        val info = TimeSyncInfo(
            offsetMillis = offsetMillis,
            serverTimeMillis = serverTime,
            phoneTimeMillis = estimatedPhoneTime,
            checkedAtMillis = phoneTimeAfter,
        )

        return try {
            syncDataRepo.saveTimeSyncInfo(info)
            if (abs(offsetMillis) <= MAX_ALLOWED_PHONE_TIME_DIFFERENCE_MILLIS) {
                CheckPhoneTimeResult.Valid(info)
            } else {
                CheckPhoneTimeResult.Invalid(info)
            }
        } catch (throwable: Throwable) {
            CheckPhoneTimeResult.Failed(DataError.Local.LOCAL_STORAGE_ERROR)
        }
    }

    private companion object {
        const val MAX_ALLOWED_PHONE_TIME_DIFFERENCE_MILLIS = 5 * 60 * 1000L
    }
}