package com.sahm.pos.domain.usecase

import com.sahm.pos.domain.entity.SyncOutboxCounts
import com.sahm.pos.domain.sync.SyncScheduler

class ScheduleSyncIfPendingUseCase(
    private val getSyncOutboxCounts: GetSyncOutboxCountsUseCase,
    private val syncScheduler: SyncScheduler,
) {
    suspend operator fun invoke(): SyncOutboxCounts {
        val counts = getSyncOutboxCounts()
        if (counts.hasPendingRows) syncScheduler.scheduleSync()
        return counts
    }
}