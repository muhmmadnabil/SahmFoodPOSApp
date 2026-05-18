package com.sahm.pos.domain.usecase

import com.sahm.pos.domain.results.SyncProcessorResult
import com.sahm.pos.domain.sync.SyncOutboxProcessor
import com.sahm.pos.domain.sync.SyncResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SyncPendingOutboxUseCaseTest {
    @Test
    fun returnsNothingToSyncWhenCounterReportsNoPendingRows() = runTest {
        val useCase = SyncPendingOutboxUseCase(
            processor = FakeSyncOutboxProcessor(SyncProcessorResult.Success),
            pendingOutboxCounter = PendingOutboxCounter { 0 },
        )

        assertEquals(SyncResult.NothingToSync, useCase())
    }

    @Test
    fun returnsSuccessWhenProcessorSucceeds() = runTest {
        val useCase = SyncPendingOutboxUseCase(FakeSyncOutboxProcessor(SyncProcessorResult.Success))

        assertEquals(SyncResult.Success, useCase())
    }

    @Test
    fun returnsTransientFailureWhenProcessorNeedsRetry() = runTest {
        val useCase = SyncPendingOutboxUseCase(FakeSyncOutboxProcessor(SyncProcessorResult.NeedsRetry))

        assertIs<SyncResult.TransientFailure>(useCase())
    }

    @Test
    fun returnsTransientFailureWhenProcessorFailsSafely() = runTest {
        val useCase = SyncPendingOutboxUseCase(FakeSyncOutboxProcessor(SyncProcessorResult.Failure("offline")))

        val result = useCase()

        assertIs<SyncResult.TransientFailure>(result)
        assertEquals("offline", result.throwable?.message)
    }

    @Test
    fun neverThrowsWhenProcessorThrowsUnexpectedException() = runTest {
        val useCase = SyncPendingOutboxUseCase(
            object : SyncOutboxProcessor {
                override suspend fun processPending(): SyncProcessorResult {
                    error("backend unreachable")
                }
            }
        )

        val result = useCase()

        assertIs<SyncResult.TransientFailure>(result)
        assertEquals("backend unreachable", result.throwable?.message)
    }
}

private class FakeSyncOutboxProcessor(
    private val result: SyncProcessorResult,
) : SyncOutboxProcessor {
    override suspend fun processPending(): SyncProcessorResult = result
}
