package com.sahm.pos.data.sync

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.sahm.pos.data.local.PlatformContext
import com.sahm.pos.domain.sync.SyncReason
import com.sahm.pos.domain.sync.SyncScheduler
import java.util.concurrent.TimeUnit

internal const val SyncOutboxWorkName = "sync_outbox"
internal const val SyncReasonInputKey = "sync_reason"

class WorkManagerSyncScheduler(
    private val workManager: WorkManager,
) : SyncScheduler {
    override fun scheduleSync(reason: SyncReason) {
        workManager.enqueueUniqueWork(
            SyncOutboxWorkName,
            ExistingWorkPolicy.KEEP,
            buildSyncOutboxRequest(reason),
        )
    }
}

internal fun buildSyncOutboxRequest(reason: SyncReason): OneTimeWorkRequest =
    OneTimeWorkRequestBuilder<SyncOutboxWorker>()
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        )
        .setInputData(workDataOf(SyncReasonInputKey to reason.name))
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
        .build()

actual fun createSyncScheduler(platformContext: PlatformContext): SyncScheduler =
    WorkManagerSyncScheduler(WorkManager.getInstance(platformContext.context))
