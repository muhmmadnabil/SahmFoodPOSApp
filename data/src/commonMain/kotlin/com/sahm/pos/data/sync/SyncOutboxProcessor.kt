package com.sahm.pos.data.sync

import com.sahm.pos.domain.CurrentEpochMillisProvider
import com.sahm.pos.domain.entity.SyncOutboxItem
import com.sahm.pos.domain.repository.SyncDataRepo
import com.sahm.pos.domain.results.SyncProcessorResult
import com.sahm.pos.domain.results.SyncUploadResult
import com.sahm.pos.domain.sync.SyncOutboxProcessor
import com.sahm.pos.domain.sync.SyncRetryPolicy

class SyncOutboxProcessorImpl(
    private val repo: SyncDataRepo,
    private val clockProvider: CurrentEpochMillisProvider,
    private val batchLimit: Long = 50,
    private val staleLockMillis: Long = 10 * 60 * 1_000L,
) : SyncOutboxProcessor {
    override suspend fun processPending(): SyncProcessorResult {
        val now = clockProvider.now()
        return runCatching {
            // If the app was killed during an upload, the row may be stuck as IN_PROGRESS.
            // After the lock is old enough we make it pending again so sync can recover.
            repo.resetStaleInProgress(now - staleLockMillis)
            val items = repo.getSyncPendingItems(batchLimit)
            if (items.isEmpty()) {
                // Pending rows can still exist when all of them are waiting for nextAttemptAt.
                // Returning NeedsRetry lets WorkManager/manual sync try again later.
                return@runCatching if (repo.getCountSyncItemsPending() > 0) {
                    SyncProcessorResult.NeedsRetry
                } else {
                    SyncProcessorResult.Success
                }
            }

            var hasRetryableRows = false
            val processedIds = mutableSetOf<String>()

            for (item in items) {
                if (!processedIds.add(item.id)) continue

                // Payments must wait for their order, and refunds must wait for order/payment.
                // This keeps the remote side consistent even when all actions were created offline.
                if (!repo.areDependenciesSatisfied(item)) {
                    hasRetryableRows = true
                    delayDependency(item)
                    continue
                }

                repo.makeSyncItemInProgress(item.id)

                when (val result = repo.uploadData(item)) {
                    SyncUploadResult.Success,
                    // If the server already has this idempotency key, a previous retry probably
                    // succeeded after the client timed out. Locally we can mark it as synced.
                    SyncUploadResult.DuplicateIdempotencyKey,
                        -> repo.markSyncItemSucceeded(item.id)

                    is SyncUploadResult.RetryableError -> {
                        hasRetryableRows = true
                        handleRetryable(item, result.code, result.message)
                    }

                    is SyncUploadResult.NonRetryableError ->
                        // Invalid payloads or permission errors will not be fixed by waiting, so
                        // keep them visible as FAILED for manual investigation.
                        repo.markSyncItemFailed(
                            id = item.id,
                            errorCode = result.code,
                            errorMessage = result.message
                        )

                    is SyncUploadResult.Conflict ->
                        // Conflicts need explicit handling because retrying the same payload could
                        // hide a real business data problem, especially around refunds.
                        repo.markSyncItemConflict(
                            id = item.id,
                            errorCode = result.code,
                            errorMessage = result.message
                        )
                }
            }

            if (hasRetryableRows || repo.getCountSyncItemsPending() > 0) {
                SyncProcessorResult.NeedsRetry
            } else {
                SyncProcessorResult.Success
            }
        }.getOrElse { throwable ->
            SyncProcessorResult.Failure(throwable.message ?: "Sync outbox processing failed.")
        }
    }

    private suspend fun delayDependency(item: SyncOutboxItem) {
        val now = clockProvider.now()
        // Dependency waits do not increase retryCount. The row itself is fine; it only needs its
        // parent aggregate to sync first.
        repo.markRetryWaiting(
            id = item.id,
            retryCount = item.retryCount,
            nextAttemptAt = now + SyncRetryPolicy.delayMillisForRetryCount(0),
            errorCode = "DEPENDENCY_NOT_SYNCED",
            errorMessage = "Required related aggregate has not synced yet.",
        )
    }

    private suspend fun handleRetryable(
        item: SyncOutboxItem,
        errorCode: String,
        errorMessage: String
    ) {
        val nextRetryCount = item.retryCount + 1
        val now = clockProvider.now()
        if (nextRetryCount >= item.maxRetries) {
            // After max retries we stop automatic uploads to avoid endless background work and
            // leave the latest error on the row for the sync screen.
            repo.markSyncItemFailed(item.id, errorCode, errorMessage)
            return
        }

        // Retryable failures keep the row in the outbox with a future nextAttemptAt. WorkManager
        // or manual sync can upload it again when the delay has passed.
        repo.markRetryWaiting(
            id = item.id,
            retryCount = nextRetryCount,
            nextAttemptAt = now + SyncRetryPolicy.delayMillisForRetryCount(item.retryCount),
            errorCode = errorCode,
            errorMessage = errorMessage,
        )
    }
}
