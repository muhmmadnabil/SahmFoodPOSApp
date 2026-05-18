package com.sahm.pos.data.sync

import com.sahm.pos.data.local.PlatformContext
import com.sahm.pos.domain.sync.SyncReason
import com.sahm.pos.domain.sync.SyncScheduler

actual fun createSyncScheduler(platformContext: PlatformContext): SyncScheduler =
    object : SyncScheduler {
        override fun scheduleSync(reason: SyncReason) = Unit
    }
