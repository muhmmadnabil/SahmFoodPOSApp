package com.sahm.pos.data.sync

import com.sahm.pos.domain.CurrentEpochMillisProvider
import com.sahm.pos.domain.entity.SyncAggregateType
import com.sahm.pos.domain.entity.SyncOutboxItem
import com.sahm.pos.domain.entity.SyncOutboxStatus
import com.sahm.pos.domain.entity.SyncOutboxType
import com.sahm.pos.domain.repository.SyncDataRepo
import com.sahm.pos.domain.results.SyncProcessorResult
import com.sahm.pos.domain.results.SyncUploadResult
import com.sahm.pos.domain.sync.SyncIdempotencyKey
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SyncOutboxProcessorImplTest {
    @Test
    fun returnsSuccessWhenNoPendingRowsExist() = runTest {
        val repo = FakeSyncDataRepo()
        val processor = processor(repo)

        assertEquals(SyncProcessorResult.Success, processor.processPending())
        assertTrue(repo.resetStaleCalled)
    }

    @Test
    fun uploadsPendingRowsInCreatedOrderAndMarksSucceeded() = runTest {
        val first = item("first", createdAt = 1_000)
        val second = item("second", createdAt = 2_000)
        val repo = FakeSyncDataRepo(listOf(first, second))

        val result = processor(repo).processPending()

        assertEquals(SyncProcessorResult.Success, result)
        assertEquals(listOf("first", "second"), repo.uploaded.map { it.aggregateId })
        assertEquals(SyncOutboxStatus.SUCCEEDED, repo.items.getValue(first.id).status)
        assertEquals(SyncOutboxStatus.SUCCEEDED, repo.items.getValue(second.id).status)
    }

    @Test
    fun retryableErrorMovesRowToRetryWaitingAndRequestsRetry() = runTest {
        val pending = item("order-1")
        val repo = FakeSyncDataRepo(
            initialItems = listOf(pending),
            result = SyncUploadResult.RetryableError("NO_INTERNET", "offline"),
        )

        val result = processor(repo).processPending()

        val saved = repo.items.getValue(pending.id)
        assertEquals(SyncProcessorResult.NeedsRetry, result)
        assertEquals(SyncOutboxStatus.RETRY_WAITING, saved.status)
        assertEquals(1, saved.retryCount)
        assertEquals(61_000L, saved.nextAttemptAt)
        assertEquals("NO_INTERNET", saved.lastErrorCode)
    }

    @Test
    fun conflictAndValidationErrorsDoNotRetryForever() = runTest {
        val conflict = item("refund-1", type = SyncOutboxType.CREATE_REFUND, aggregateType = SyncAggregateType.REFUND, createdAt = 1_000)
        val failed = item("payment-1", type = SyncOutboxType.CREATE_PAYMENT, aggregateType = SyncAggregateType.PAYMENT, createdAt = 2_000)
        val repo = FakeSyncDataRepo(
            initialItems = listOf(conflict, failed),
            results = mutableListOf(
                SyncUploadResult.Conflict("REFUND_QUANTITY_EXCEEDED", "already refunded"),
                SyncUploadResult.NonRetryableError("VALIDATION_FAILED", "bad payload"),
            )
        )

        val result = processor(repo).processPending()

        assertEquals(SyncProcessorResult.Success, result)
        assertEquals(SyncOutboxStatus.CONFLICT, repo.items.getValue(conflict.id).status)
        assertEquals(SyncOutboxStatus.FAILED, repo.items.getValue(failed.id).status)
    }

    private fun processor(repo: FakeSyncDataRepo) =
        SyncOutboxProcessorImpl(repo, CurrentEpochMillisProvider { 1_000 })

    private fun item(
        aggregateId: String,
        type: SyncOutboxType = SyncOutboxType.CREATE_ORDER,
        aggregateType: SyncAggregateType = SyncAggregateType.ORDER,
        createdAt: Long = 1_000,
    ): SyncOutboxItem {
        val key = SyncIdempotencyKey.create(type, aggregateId)
        return SyncOutboxItem(
            id = key,
            type = type,
            aggregateId = aggregateId,
            aggregateType = aggregateType,
            payloadJson = "{}",
            idempotencyKey = key,
            status = SyncOutboxStatus.PENDING,
            createdAt = createdAt,
            updatedAt = createdAt,
        )
    }
}

private class FakeSyncDataRepo(
    initialItems: List<SyncOutboxItem> = emptyList(),
    private val result: SyncUploadResult = SyncUploadResult.Success,
    private val dependenciesSatisfied: Boolean = true,
    private val results: MutableList<SyncUploadResult> = mutableListOf(),
) : SyncDataRepo {
    val items = initialItems.associateBy { it.id }.toMutableMap()
    val uploaded = mutableListOf<SyncOutboxItem>()
    var resetStaleCalled = false

    override suspend fun uploadData(item: SyncOutboxItem): SyncUploadResult {
        uploaded += item
        return results.removeFirstOrNull() ?: result
    }

    override suspend fun areDependenciesSatisfied(item: SyncOutboxItem): Boolean = dependenciesSatisfied

    override suspend fun getSyncPendingItems(limit: Long): List<SyncOutboxItem> =
        items.values
            .filter { it.status == SyncOutboxStatus.PENDING || (it.status == SyncOutboxStatus.RETRY_WAITING && (it.nextAttemptAt ?: 0) <= 1_000) }
            .sortedWith(compareBy<SyncOutboxItem> { it.createdAt }.thenBy { it.id })
            .take(limit.toInt())

    override suspend fun makeSyncItemInProgress(id: String) {
        items[id] = items.getValue(id).copy(status = SyncOutboxStatus.IN_PROGRESS, lockedAt = 1_000, updatedAt = 1_000)
    }

    override suspend fun markSyncItemSucceeded(id: String) {
        items[id] = items.getValue(id).copy(status = SyncOutboxStatus.SUCCEEDED, lockedAt = null, updatedAt = 1_000)
    }

    override suspend fun markRetryWaiting(
        id: String,
        retryCount: Int,
        nextAttemptAt: Long,
        errorCode: String,
        errorMessage: String,
    ) {
        items[id] = items.getValue(id).copy(
            status = SyncOutboxStatus.RETRY_WAITING,
            retryCount = retryCount,
            nextAttemptAt = nextAttemptAt,
            lastErrorCode = errorCode,
            lastErrorMessage = errorMessage,
            lockedAt = null,
            updatedAt = 1_000,
        )
    }

    override suspend fun markSyncItemFailed(id: String, errorCode: String, errorMessage: String) {
        items[id] = items.getValue(id).copy(
            status = SyncOutboxStatus.FAILED,
            lastErrorCode = errorCode,
            lastErrorMessage = errorMessage,
        )
    }

    override suspend fun markSyncItemConflict(id: String, errorCode: String, errorMessage: String) {
        items[id] = items.getValue(id).copy(
            status = SyncOutboxStatus.CONFLICT,
            lastErrorCode = errorCode,
            lastErrorMessage = errorMessage,
        )
    }

    override suspend fun resetStaleInProgress(cutoffTime: Long) {
        resetStaleCalled = true
    }

    override suspend fun getCountSyncItemsPending(): Long =
        items.values.count { it.status in setOf(SyncOutboxStatus.PENDING, SyncOutboxStatus.RETRY_WAITING, SyncOutboxStatus.IN_PROGRESS) }.toLong()

    override suspend fun getCountSyncItemsConflicts(): Long =
        items.values.count { it.status == SyncOutboxStatus.CONFLICT }.toLong()

    override suspend fun getCountSyncItemsFailed(): Long =
        items.values.count { it.status == SyncOutboxStatus.FAILED }.toLong()
}
