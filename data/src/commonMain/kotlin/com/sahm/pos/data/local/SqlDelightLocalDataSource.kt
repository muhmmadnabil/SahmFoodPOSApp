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
    suspend fun getLastDiscountsSyncAt(): Long?
    suspend fun getDiscountsCount(): Int

    //Upload Sync Outbox
    suspend fun insertOutboxItem(item: SyncOutboxItem)
    suspend fun getPendingItems(nowMillis: Long, limit: Long): List<SyncOutboxItem>
    suspend fun markSyncItemInProgress(id: String, nowMillis: Long)
    suspend fun markSyncItemSucceeded(id: String, nowMillis: Long)
    suspend fun markRetryWaiting(
        id: String,
        retryCount: Int,
        nextAttemptAt: Long,
        errorCode: String,
        errorMessage: String,
        nowMillis: Long,
    )

    suspend fun markSyncItemFailed(
        id: String,
        errorCode: String,
        errorMessage: String,
        nowMillis: Long
    )

    suspend fun markSyncItemConflict(
        id: String,
        errorCode: String,
        errorMessage: String,
        nowMillis: Long
    )

    suspend fun resetStaleInProgress(expiredBeforeMillis: Long, nowMillis: Long)
    suspend fun countSyncItemsPending(): Long
    suspend fun countConflicts(): Long
    suspend fun countFailed(): Long
    suspend fun isAggregateSynced(aggregateType: SyncAggregateType, aggregateId: String): Boolean
    fun markOrderSynced(time: Long, aggregateId: String)
    fun markPaymentSynced(time: Long, aggregateId: String)
    fun markRefundSynced(time: Long, aggregateId: String)
    fun getRefundDependencies(aggregateId: String): SelectRefundDependencies?
    fun getPaymentSyncedAt(aggregateId: String): Long?
    fun getPaymentOrderSyncedAt(paymentId: String): Long? = null
    suspend fun getOrderSyncStats(): SyncAggregateStats
    suspend fun getPaymentSyncStats(): SyncAggregateStats
}
