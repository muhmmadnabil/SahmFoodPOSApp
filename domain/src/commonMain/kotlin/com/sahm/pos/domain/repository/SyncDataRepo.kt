package com.sahm.pos.domain.repository

import com.sahm.pos.domain.results.SyncResult
import com.sahm.pos.domain.entity.Discount
import com.sahm.pos.domain.entity.MenuItem
import com.sahm.pos.domain.entity.SyncAggregateStats
import com.sahm.pos.domain.entity.SyncOutboxItem
import com.sahm.pos.domain.entity.TimeSyncInfo
import com.sahm.pos.domain.results.SyncUploadResult

interface SyncDataRepo {
    suspend fun hasUsers(): Boolean = false
    suspend fun syncUsers(): SyncResult = SyncResult.UnknownError
    suspend fun syncMenuItems(): SyncResult = SyncResult.UnknownError
    suspend fun syncDiscounts(): SyncResult = SyncResult.UnknownError
    suspend fun getActiveMenuItems(): List<MenuItem> = emptyList()
    suspend fun getDiscountByPromoCode(promoCode: String): Discount? = null
    suspend fun getUserCount(): Long = 0
    suspend fun getMenuItemCount(): Long = 0
    suspend fun getLastUsersSyncAt(): Long? = null
    suspend fun getLastMenuItemsSyncAt(): Long? = null
    suspend fun saveTimeSyncInfo(info: TimeSyncInfo) = Unit
    suspend fun getTimeSyncInfo(): TimeSyncInfo? = null
    suspend fun getServerTimeStamp(): Long? = null
    suspend fun getLastDiscountsSyncAt(): Long? = null
    suspend fun getDiscountsCount(): Int = 0
    suspend fun uploadData(item: SyncOutboxItem): SyncUploadResult =
        SyncUploadResult.RetryableError("NOT_IMPLEMENTED", "Outbox upload is not implemented.")
    suspend fun areDependenciesSatisfied(item: SyncOutboxItem): Boolean = true
    suspend fun getSyncPendingItems(limit: Long): List<SyncOutboxItem> = emptyList()
    suspend fun makeSyncItemInProgress(id: String) = Unit
    suspend fun resetStaleInProgress(cutoffTime: Long) = Unit
    suspend fun markSyncItemSucceeded(id: String) = Unit
    suspend fun markSyncItemFailed(
        id: String,
        errorCode: String,
        errorMessage: String
    ) = Unit

    suspend fun markSyncItemConflict(id: String, errorCode: String, errorMessage: String) = Unit
    suspend fun markRetryWaiting(
        id: String, retryCount: Int, nextAttemptAt: Long, errorCode: String,
        errorMessage: String
    ) = Unit

    suspend fun getCountSyncItemsPending(): Long = 0
    suspend fun getCountSyncItemsFailed(): Long = 0
    suspend fun getCountSyncItemsConflicts(): Long = 0
    suspend fun getOrderSyncStats(): SyncAggregateStats = SyncAggregateStats.Empty
    suspend fun getPaymentSyncStats(): SyncAggregateStats = SyncAggregateStats.Empty
}
