package com.sahm.pos.domain.usecase

import com.sahm.pos.domain.results.SyncProcessorResult
import com.sahm.pos.domain.sync.SyncOutboxProcessor
import com.sahm.pos.domain.sync.SyncResult

class SyncPendingOutboxUseCase(
    private val processor: SyncOutboxProcessor,
    private val pendingOutboxCounter: PendingOutboxCounter? = null,
) {
    suspend operator fun invoke(): SyncResult =
        runCatching {
            if (pendingOutboxCounter?.countPendingRows() == 0L) {
                return@runCatching SyncResult.NothingToSync
            }

            when (val result = processor.processPending()) {
                SyncProcessorResult.Success -> SyncResult.Success
                SyncProcessorResult.NeedsRetry -> SyncResult.TransientFailure()
                is SyncProcessorResult.Failure -> SyncResult.TransientFailure(
                    IllegalStateException(result.message)
                )
            }
        }.getOrElse { throwable ->
            SyncResult.TransientFailure(throwable)
        }
}

fun interface PendingOutboxCounter {
    suspend fun countPendingRows(): Long
}
