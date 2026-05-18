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
            repo.resetStaleInProgress(now - staleLockMillis)
            val items = repo.getSyncPendingItems(batchLimit)
            if (items.isEmpty()) {
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

                if (!repo.areDependenciesSatisfied(item)) {
                    hasRetryableRows = true
                    delayDependency(item)
                    continue
                }

                repo.makeSyncItemInProgress(item.id)

                when (val result = repo.uploadData(item)) {
                    SyncUploadResult.Success,
                    SyncUploadResult.DuplicateIdempotencyKey,
                        -> repo.markSyncItemSucceeded(item.id)

                    is SyncUploadResult.RetryableError -> {
                        hasRetryableRows = true
                        handleRetryable(item, result.code, result.message)
                    }

                    is SyncUploadResult.NonRetryableError ->
                        repo.markSyncItemFailed(
                            id = item.id,
                            errorCode = result.code,
                            errorMessage = result.message
                        )

                    is SyncUploadResult.Conflict ->
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
            repo.markSyncItemFailed(item.id, errorCode, errorMessage)
            return
        }

        repo.markRetryWaiting(
            id = item.id,
            retryCount = nextRetryCount,
            nextAttemptAt = now + SyncRetryPolicy.delayMillisForRetryCount(item.retryCount),
            errorCode = errorCode,
            errorMessage = errorMessage,
        )
    }
}
