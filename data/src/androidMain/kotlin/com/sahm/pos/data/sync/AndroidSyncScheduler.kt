package com.sahm.pos.data.sync

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.sahm.pos.data.local.PlatformContext
import com.sahm.pos.domain.sync.SyncScheduler
import java.util.concurrent.TimeUnit

private const val SyncOutboxWorkName = "sync_outbox_worker"

class AndroidSyncScheduler(
    private val workManager: WorkManager,
) : SyncScheduler {
    override fun scheduleSync() {
        val request = OneTimeWorkRequestBuilder<SyncOutboxWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
            .build()

        workManager.enqueueUniqueWork(
            SyncOutboxWorkName,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }
}

actual fun createSyncScheduler(platformContext: PlatformContext): SyncScheduler =
    AndroidSyncScheduler(WorkManager.getInstance(platformContext.context))
