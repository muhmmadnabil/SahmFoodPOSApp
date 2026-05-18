package com.sahm.pos.data.sync

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.sahm.pos.data.local.SqlDelightLocalDataSourceImpl
import com.sahm.pos.data.local.database.SahmPosDatabase
import com.sahm.pos.domain.sync.SyncIdempotencyKey
import com.sahm.pos.domain.sync.SyncAggregateType
import com.sahm.pos.domain.sync.SyncOutboxItem
import com.sahm.pos.domain.sync.SyncOutboxStatus
import com.sahm.pos.domain.sync.SyncOutboxType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SyncOutboxLocalDataSourceImplTest {
    @Test
    fun insertOutboxItemInsertsPendingRowAndRejectsDuplicateIdempotencyKey() = runTest {
        val local = localDataSource()
        val item = item("order-1")

        local.insertOutboxItem(item)

        assertEquals(listOf(item), local.getPendingItems(nowMillis = 2_000, limit = 10))
        assertFailsWith<Throwable> {
            local.insertOutboxItem(item.copy(id = "different-id"))
        }
    }

    @Test
    fun getPendingItemsFiltersRetryWaitingFutureRowsAndOrdersByCreatedAt() = runTest {
        val local = localDataSource()
        val first = item("first", createdAt = 1_000)
        val second = item("second", createdAt = 2_000)
        val future = item("future", status = SyncOutboxStatus.RETRY_WAITING, createdAt = 500, nextAttemptAt = 9_000)
        local.insertOutboxItem(second)
        local.insertOutboxItem(future)
        local.insertOutboxItem(first)

        assertEquals(listOf(first, second), local.getPendingItems(nowMillis = 3_000, limit = 10))
    }

    @Test
    fun statusMarkersPersistRetryFailureAndConflictDetails() = runTest {
        val local = localDataSource()
        val item = item("order-1")
        local.insertOutboxItem(item)

        local.markSyncItemInProgress(item.id, 2_000)
        assertEquals(0, local.getPendingItems(2_000, 10).size)

        local.markRetryWaiting(item.id, 1, 62_000, "NO_INTERNET", "offline", 2_000)
        val retry = local.getPendingItems(62_000, 10).single()
        assertEquals(SyncOutboxStatus.RETRY_WAITING, retry.status)
        assertEquals(1, retry.retryCount)
        assertEquals(62_000, retry.nextAttemptAt)

        local.markSyncItemFailed(item.id, "VALIDATION_FAILED", "bad payload", 3_000)
        assertEquals(1, local.countFailed())

        val conflict = item("refund-1", type = SyncOutboxType.CREATE_REFUND, aggregateType = SyncAggregateType.REFUND)
        local.insertOutboxItem(conflict)
        local.markSyncItemConflict(conflict.id, "REFUND_QUANTITY_EXCEEDED", "already refunded", 3_000)
        assertEquals(1, local.countConflicts())
    }

    @Test
    fun resetStaleInProgressOnlyResetsExpiredLocks() = runTest {
        val local = localDataSource()
        val stale = item("stale")
        val fresh = item("fresh")
        local.insertOutboxItem(stale)
        local.insertOutboxItem(fresh)
        local.markSyncItemInProgress(stale.id, 1_000)
        local.markSyncItemInProgress(fresh.id, 10_000)

        local.resetStaleInProgress(expiredBeforeMillis = 5_000, nowMillis = 20_000)

        val pending = local.getPendingItems(nowMillis = 20_000, limit = 10)
        assertTrue(pending.any { it.id == stale.id })
        assertTrue(pending.none { it.id == fresh.id })
        assertEquals(2, local.countSyncItemsPending())
    }

    private fun localDataSource(): SqlDelightLocalDataSourceImpl {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        SahmPosDatabase.Schema.create(driver)
        return SqlDelightLocalDataSourceImpl(SahmPosDatabase(driver))
    }

    private fun item(
        aggregateId: String,
        type: SyncOutboxType = SyncOutboxType.CREATE_ORDER,
        aggregateType: SyncAggregateType = SyncAggregateType.ORDER,
        status: SyncOutboxStatus = SyncOutboxStatus.PENDING,
        createdAt: Long = 1_000,
        nextAttemptAt: Long? = null,
    ): SyncOutboxItem {
        val key = SyncIdempotencyKey.create(type, aggregateId)
        return SyncOutboxItem(
            id = key,
            type = type,
            aggregateId = aggregateId,
            aggregateType = aggregateType,
            payloadJson = "{}",
            idempotencyKey = key,
            status = status,
            nextAttemptAt = nextAttemptAt,
            createdAt = createdAt,
            updatedAt = createdAt,
        )
    }
}
