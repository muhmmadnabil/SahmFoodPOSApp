package com.sahm.pos.domain.repository

import com.sahm.pos.domain.results.SyncResult
import com.sahm.pos.domain.entity.Discount
import com.sahm.pos.domain.entity.MenuItem
import com.sahm.pos.domain.entity.SyncAggregateStats
import com.sahm.pos.domain.entity.SyncOutboxItem
import com.sahm.pos.domain.entity.TimeSyncInfo
import com.sahm.pos.domain.results.SyncUploadResult

interface SyncDataRepo {
    suspend fun hasUsers(): Boolean
    suspend fun syncUsers(): SyncResult
    suspend fun syncMenuItems(): SyncResult
    suspend fun syncDiscounts(): SyncResult
    suspend fun getActiveMenuItems(): List<MenuItem>
    suspend fun getDiscountByPromoCode(promoCode: String): Discount?
    suspend fun getUserCount(): Long
    suspend fun getMenuItemCount(): Long
    suspend fun getLastUsersSyncAt(): Long?
    suspend fun getLastMenuItemsSyncAt(): Long?
    suspend fun saveTimeSyncInfo(info: TimeSyncInfo)
    suspend fun getTimeSyncInfo(): TimeSyncInfo?
    suspend fun getServerTimeStamp(): Long?
    suspend fun getLastDiscountsSyncAt(): Long?
    suspend fun getDiscountsCount(): Int
    suspend fun uploadData(item: SyncOutboxItem): SyncUploadResult
    suspend fun areDependenciesSatisfied(item: SyncOutboxItem): Boolean
    suspend fun getSyncPendingItems(limit: Long): List<SyncOutboxItem>
    suspend fun makeSyncItemInProgress(id: String)
    suspend fun resetStaleInProgress(cutoffTime: Long)
    suspend fun markSyncItemSucceeded(id: String)
    suspend fun markSyncItemFailed(
        id: String,
        errorCode: String,
        errorMessage: String
    )

    suspend fun markSyncItemConflict(id: String, errorCode: String, errorMessage: String)
    suspend fun markRetryWaiting(
        id: String, retryCount: Int, nextAttemptAt: Long, errorCode: String,
        errorMessage: String
    )

    suspend fun getCountSyncItemsPending(): Long
    suspend fun getCountSyncItemsFailed(): Long
    suspend fun getCountSyncItemsConflicts(): Long
    suspend fun getOrderSyncStats(): SyncAggregateStats = SyncAggregateStats.Empty
    suspend fun getPaymentSyncStats(): SyncAggregateStats = SyncAggregateStats.Empty
}
