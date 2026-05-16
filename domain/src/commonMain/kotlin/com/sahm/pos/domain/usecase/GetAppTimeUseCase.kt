package com.sahm.pos.domain.usecase

import com.sahm.pos.domain.ClockProvider
import com.sahm.pos.domain.repository.SyncDataRepo

class GetAppTimeUseCase(
    private val syncDataRepo: SyncDataRepo,
    private val clockProvider: ClockProvider,
) {
    suspend fun nowMillis(): Long {
        val offset = syncDataRepo.getTimeSyncInfo()?.offsetMillis ?: 0L
        return clockProvider.nowMillis() + offset
    }
}
