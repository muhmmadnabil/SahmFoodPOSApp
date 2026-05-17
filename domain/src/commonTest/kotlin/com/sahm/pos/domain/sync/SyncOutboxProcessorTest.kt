package com.sahm.pos.domain.sync

import com.sahm.pos.domain.ClockProvider
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SyncOutboxProcessorTest {
    @Test
    fun returnsSuccessWhenNoPendingRowsExist() = runTest {
        val local = FakeOutboxLocalDataSource()
        val processor = processor(local)

        assertEquals(SyncProcessorResult.Success, processor.processPending())
        assertTrue(local.resetStaleCalled)
    }

    @Test
    fun uploadsPendingRowsInCreatedOrderAndMarksSucceeded() = runTest {
        val first = item("first", createdAt = 1_000)
        val second = item("second", createdAt = 2_000)
        val local = FakeOutboxLocalDataSource(first, second)
        val repo = FakeSyncRepo()

        val result = processor(local, repo).processPending()

        assertEquals(SyncProcessorResult.Success, result)
        assertEquals(listOf("first", "second"), repo.uploaded.map { it.aggregateId })
        assertEquals(SyncOutboxStatus.SUCCEEDED, local.items.getValue(first.id).status)
        assertEquals(SyncOutboxStatus.SUCCEEDED, local.items.getValue(second.id).status)
    }

    @Test
    fun retryableErrorMovesRowToRetryWaitingAndRequestsRetry() = runTest {
        val pending = item("order-1")
        val local = FakeOutboxLocalDataSource(pending)
        val repo = FakeSyncRepo(SyncUploadResult.RetryableError("NO_INTERNET", "offline"))

        val result = processor(local, repo).processPending()

        val saved = local.items.getValue(pending.id)
        assertEquals(SyncProcessorResult.NeedsRetry, result)
        assertEquals(SyncOutboxStatus.RETRY_WAITING, saved.status)
        assertEquals(1, saved.retryCount)
        assertEquals(61_000L, saved.nextAttemptAt)
        assertEquals("NO_INTERNET", saved.lastErrorCode)
    }

    @Test
    fun conflictAndValidationErrorsDoNotRetryForever() = runTest {
        val conflict = item("refund-1", type = SyncOutboxType.CREATE_REFUND, aggregateType = SyncAggregateType.REFUND)
        val failed = item("payment-1", type = SyncOutboxType.CREATE_PAYMENT, aggregateType = SyncAggregateType.PAYMENT)
        val local = FakeOutboxLocalDataSource(conflict, failed)
        val repo = FakeSyncRepo(
            results = mutableListOf(
                SyncUploadResult.Conflict("REFUND_QUANTITY_EXCEEDED", "already refunded"),
                SyncUploadResult.NonRetryableError("VALIDATION_FAILED", "bad payload"),
            )
        )

        val result = processor(local, repo).processPending()

        assertEquals(SyncProcessorResult.Success, result)
        assertEquals(SyncOutboxStatus.CONFLICT, local.items.getValue(conflict.id).status)
        assertEquals(SyncOutboxStatus.FAILED, local.items.getValue(failed.id).status)
    }

    private fun processor(
        local: FakeOutboxLocalDataSource,
        repo: FakeSyncRepo = FakeSyncRepo(),
    ) = SyncOutboxProcessor(local, repo, ClockProvider { 1_000 })

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

private class FakeSyncRepo(
    private val result: SyncUploadResult = SyncUploadResult.Success,
    private val dependenciesSatisfied: Boolean = true,
    private val results: MutableList<SyncUploadResult> = mutableListOf(),
) : SyncRepo {
    val uploaded = mutableListOf<SyncOutboxItem>()

    override suspend fun upload(item: SyncOutboxItem): SyncUploadResult {
        uploaded += item
        return results.removeFirstOrNull() ?: result
    }

    override suspend fun areDependenciesSatisfied(item: SyncOutboxItem): Boolean = dependenciesSatisfied
}

private class FakeOutboxLocalDataSource(
    vararg initialItems: SyncOutboxItem,
) : SyncOutboxLocalDataSource {
    val items = initialItems.associateBy { it.id }.toMutableMap()
    var resetStaleCalled = false

    override suspend fun insertOutboxItem(item: SyncOutboxItem) {
        check(items.putIfAbsent(item.id, item) == null)
    }

    override suspend fun getPendingItems(nowMillis: Long, limit: Long): List<SyncOutboxItem> =
        items.values
            .filter { it.status == SyncOutboxStatus.PENDING || (it.status == SyncOutboxStatus.RETRY_WAITING && (it.nextAttemptAt ?: 0) <= nowMillis) }
            .sortedWith(compareBy<SyncOutboxItem> { it.createdAt }.thenBy { it.id })
            .take(limit.toInt())

    override suspend fun markInProgress(id: String, nowMillis: Long) {
        items[id] = items.getValue(id).copy(status = SyncOutboxStatus.IN_PROGRESS, lockedAt = nowMillis, updatedAt = nowMillis)
    }

    override suspend fun markSucceeded(id: String, nowMillis: Long) {
        items[id] = items.getValue(id).copy(status = SyncOutboxStatus.SUCCEEDED, lockedAt = null, updatedAt = nowMillis)
    }

    override suspend fun markRetryWaiting(id: String, retryCount: Int, nextAttemptAt: Long, errorCode: String, errorMessage: String, nowMillis: Long) {
        items[id] = items.getValue(id).copy(
            status = SyncOutboxStatus.RETRY_WAITING,
            retryCount = retryCount,
            nextAttemptAt = nextAttemptAt,
            lastErrorCode = errorCode,
            lastErrorMessage = errorMessage,
            lockedAt = null,
            updatedAt = nowMillis,
        )
    }

    override suspend fun markFailed(id: String, errorCode: String, errorMessage: String, nowMillis: Long) {
        items[id] = items.getValue(id).copy(status = SyncOutboxStatus.FAILED, lastErrorCode = errorCode, lastErrorMessage = errorMessage)
    }

    override suspend fun markConflict(id: String, errorCode: String, errorMessage: String, nowMillis: Long) {
        items[id] = items.getValue(id).copy(status = SyncOutboxStatus.CONFLICT, lastErrorCode = errorCode, lastErrorMessage = errorMessage)
    }

    override suspend fun resetStaleInProgress(expiredBeforeMillis: Long, nowMillis: Long) {
        resetStaleCalled = true
    }

    override suspend fun countPending(): Long =
        items.values.count { it.status in setOf(SyncOutboxStatus.PENDING, SyncOutboxStatus.RETRY_WAITING, SyncOutboxStatus.IN_PROGRESS) }.toLong()

    override suspend fun countConflicts(): Long = items.values.count { it.status == SyncOutboxStatus.CONFLICT }.toLong()
    override suspend fun countFailed(): Long = items.values.count { it.status == SyncOutboxStatus.FAILED }.toLong()
    override suspend fun isAggregateSynced(aggregateType: SyncAggregateType, aggregateId: String): Boolean = true
}
