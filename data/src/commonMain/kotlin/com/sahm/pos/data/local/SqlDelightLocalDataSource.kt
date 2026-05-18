package com.sahm.pos.data.local

import com.sahm.pos.data.local.database.SelectRefundDependencies
import com.sahm.pos.domain.entity.Discount
import com.sahm.pos.domain.entity.MenuItem
import com.sahm.pos.domain.entity.SyncAggregateStats
import com.sahm.pos.domain.entity.SyncAggregateType
import com.sahm.pos.domain.entity.SyncOutboxItem
import com.sahm.pos.domain.entity.User

interface SqlDelightLocalDataSource {
    suspend fun hasUsers(): Boolean
    suspend fun upsertUsers(users: List<User>)
    suspend fun replaceUsersSnapshot(users: List<User>)
    suspend fun getUserByPhone(phone: String): User?
    suspend fun updateUserLastLoginAt(userId: String, timestamp: String)
    suspend fun getUserCount(): Long
    suspend fun getLastUsersSyncAt(): Long?
    suspend fun replaceMenuItemsSnapshot(items: List<MenuItem>)
    suspend fun getActiveMenuItems(): List<MenuItem>
    suspend fun getMenuItemById(id: String): MenuItem?
    suspend fun getMenuItemCountById(id: String): Long
    suspend fun getMenuItemCount(): Long
    suspend fun getLastMenuItemsSyncAt(): Long?
    suspend fun replaceAllDiscounts(discounts: List<Discount>)
    suspend fun getAllDiscounts(): List<Discount>
    suspend fun getDiscountByPromoCode(promoCode: String): Discount?
    suspend fun getDiscountCount(): Long
    suspend fun getLastDiscountsSyncAt(): Long? = null
    suspend fun getDiscountsCount(): Int = 0

    //Upload Sync Outbox
    suspend fun insertOutboxItem(item: SyncOutboxItem) = Unit
    suspend fun getPendingItems(nowMillis: Long, limit: Long): List<SyncOutboxItem> = emptyList()
    suspend fun markSyncItemInProgress(id: String, nowMillis: Long) = Unit
    suspend fun markSyncItemSucceeded(id: String, nowMillis: Long) = Unit
    suspend fun markRetryWaiting(
        id: String,
        retryCount: Int,
        nextAttemptAt: Long,
        errorCode: String,
        errorMessage: String,
        nowMillis: Long,
    ) = Unit

    suspend fun markSyncItemFailed(
        id: String,
        errorCode: String,
        errorMessage: String,
        nowMillis: Long
    ) = Unit

    suspend fun markSyncItemConflict(
        id: String,
        errorCode: String,
        errorMessage: String,
        nowMillis: Long
    ) = Unit

    suspend fun resetStaleInProgress(expiredBeforeMillis: Long, nowMillis: Long) = Unit
    suspend fun countSyncItemsPending(): Long = 0
    suspend fun countConflicts(): Long = 0
    suspend fun countFailed(): Long = 0
    suspend fun isAggregateSynced(aggregateType: SyncAggregateType, aggregateId: String): Boolean = true
    fun markOrderSynced(time: Long, aggregateId: String) = Unit
    fun markPaymentSynced(time: Long, aggregateId: String) = Unit
    fun markRefundSynced(time: Long, aggregateId: String) = Unit
    fun getRefundDependencies(aggregateId: String): SelectRefundDependencies? = null
    fun getPaymentSyncedAt(aggregateId: String): Long? = null
    fun getPaymentOrderSyncedAt(paymentId: String): Long? = null
    suspend fun getOrderSyncStats(): SyncAggregateStats = SyncAggregateStats.Empty
    suspend fun getPaymentSyncStats(): SyncAggregateStats = SyncAggregateStats.Empty
}
