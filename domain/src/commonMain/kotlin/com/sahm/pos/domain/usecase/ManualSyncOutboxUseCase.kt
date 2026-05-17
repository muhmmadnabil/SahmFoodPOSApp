package com.sahm.pos.domain.usecase

import com.sahm.pos.domain.sync.SyncScheduler

class ManualSyncOutboxUseCase(
    private val syncScheduler: SyncScheduler,
) {
    operator fun invoke() {
        syncScheduler.scheduleSync()
    }
}