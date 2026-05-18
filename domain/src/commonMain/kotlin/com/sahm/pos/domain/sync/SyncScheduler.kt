package com.sahm.pos.domain.sync

interface SyncScheduler {
    fun scheduleSync(reason: SyncReason)
}
