package com.sahm.pos.data.local

import com.sahm.pos.data.local.database.SahmPosDatabase
import com.sahm.pos.data.local.database.SelectRefundDependencies
import com.sahm.pos.domain.entity.Discount
import com.sahm.pos.domain.entity.MenuItem
import com.sahm.pos.domain.entity.SyncAggregateType
import com.sahm.pos.domain.entity.SyncOutboxItem
import com.sahm.pos.domain.entity.SyncOutboxStatus
import com.sahm.pos.domain.entity.SyncOutboxType
import com.sahm.pos.domain.entity.User
import kotlin.text.toLong

internal class SqlDelightLocalDataSourceImpl(
    private val database: SahmPosDatabase,
) : SqlDelightLocalDataSource {
    override suspend fun hasUsers(): Boolean =
        database.userQueries.hasUsers().executeAsOne()

    override suspend fun upsertUsers(users: List<User>) {
        replaceUsersSnapshot(users)
    }

    override suspend fun replaceUsersSnapshot(users: List<User>) {
        if (users.isEmpty()) return

        database.transaction {
            users.forEach { user ->
                require(user.id.isNotBlank()) { "User id must not be blank." }
                database.userQueries.insertOrIgnore(
                    id = user.id,
                    username = user.username,
                    phone = user.phone,
                    password = user.password,
                    created_at = user.createdAt,
                    is_active = if (user.isActive) 1L else 0L,
                    last_login_at = user.lastLoginAt,
                    last_sync_at = user.lastSyncAt.toString()
                )
                database.userQueries.update(
                    username = user.username,
                    phone = user.phone,
                    password = user.password,
                    created_at = user.createdAt,
                    is_active = if (user.isActive) 1L else 0L,
                    last_login_at = user.lastLoginAt,
                    last_sync_at = user.lastSyncAt.toString(),
                    id = user.id,
                )
            }
            database.userQueries.markMissingInactive(users.map { it.id })
        }
    }

    override suspend fun getUserByPhone(phone: String): User? =
        database.userQueries.selectByPhone(phone).executeAsOneOrNull()?.let { user ->
            User(
                id = user.id,
                username = user.username,
                phone = user.phone,
                password = user.password,
                createdAt = user.created_at,
                isActive = user.is_active == 1L,
                lastLoginAt = user.last_login_at,
                lastSyncAt = user.last_sync_at.toLong()
            )
        }

    override suspend fun updateUserLastLoginAt(userId: String, timestamp: String) {
        database.userQueries.updateLastLoginAt(
            last_login_at = timestamp,
            id = userId,
        )
    }

    override suspend fun getUserCount(): Long =
        database.userQueries.countAll().executeAsOne()

    override suspend fun getLastUsersSyncAt(): Long? =
        database.userQueries.lastSyncAt().executeAsOneOrNull()?.MAX

    override suspend fun replaceMenuItemsSnapshot(items: List<MenuItem>) {
        if (items.isEmpty()) return

        database.transaction {
            items.forEach { item ->
                require(item.id.isNotBlank()) { "Menu item id must not be blank." }
                database.menuItemQueries.insertOrIgnore(
                    id = item.id,
                    category = item.category,
                    name = item.name,
                    description = item.description,
                    image_url = item.imageUrl,
                    local_image_url = item.localImageUrl,
                    price = item.price,
                    last_synced_at = item.lastSyncedAt,
                    is_active = if (item.isActive) 1L else 0L,
                )
                database.menuItemQueries.update(
                    category = item.category,
                    name = item.name,
                    description = item.description,
                    image_url = item.imageUrl,
                    local_image_url = item.localImageUrl,
                    price = item.price,
                    last_synced_at = item.lastSyncedAt,
                    is_active = if (item.isActive) 1L else 0L,
                    id = item.id,
                )
            }
            database.menuItemQueries.markMissingInactive(items.map { it.id })
        }
    }

    override suspend fun getActiveMenuItems(): List<MenuItem> =
        database.menuItemQueries.selectActive(::mapMenuItem).executeAsList()

    override suspend fun getMenuItemById(id: String): MenuItem? =
        database.menuItemQueries.selectById(id, ::mapMenuItem).executeAsOneOrNull()

    override suspend fun getMenuItemCountById(id: String): Long =
        database.menuItemQueries.countById(id).executeAsOne()

    override suspend fun getMenuItemCount(): Long =
        database.menuItemQueries.countAll().executeAsOne()

    override suspend fun getLastMenuItemsSyncAt(): Long? =
        database.menuItemQueries.lastSyncAt().executeAsOneOrNull()?.MAX

    override suspend fun replaceAllDiscounts(discounts: List<Discount>) {
        database.transaction {
            database.discountQueries.deleteAll()
            discounts.forEach { discount ->
                require(discount.id.isNotBlank()) { "Discount id must not be blank." }
                database.discountQueries.insert(
                    id = discount.id,
                    promoCode = discount.promoCode,
                    percent = discount.percent,
                    minValue = discount.minValue,
                    maxValue = discount.maxValue,
                    startAt = discount.startAt,
                    endAt = discount.endAt,
                    syncedAt = discount.syncAt,
                )
            }
        }
    }

    override suspend fun getAllDiscounts(): List<Discount> =
        database.discountQueries.selectAll(::mapDiscount).executeAsList()

    override suspend fun getDiscountByPromoCode(promoCode: String): Discount? =
        database.discountQueries.selectByPromoCode(promoCode, ::mapDiscount).executeAsOneOrNull()

    override suspend fun getDiscountCount(): Long =
        database.discountQueries.countAll().executeAsOne()

    override suspend fun getLastDiscountsSyncAt(): Long? =
        database.discountQueries.lastSyncAt().executeAsOneOrNull()?.MAX

    override suspend fun getDiscountsCount(): Int =
        database.discountQueries.countAll().executeAsOne().toInt()

    override suspend fun insertOutboxItem(item: SyncOutboxItem) {
        database.syncOutboxQueries.insertOutboxItem(
            id = item.id,
            type = item.type.name,
            aggregate_id = item.aggregateId,
            aggregate_type = item.aggregateType.name,
            payload_json = item.payloadJson,
            idempotency_key = item.idempotencyKey,
            status = item.status.name,
            retry_count = item.retryCount.toLong(),
            max_retries = item.maxRetries.toLong(),
            next_attempt_at = item.nextAttemptAt,
            created_at = item.createdAt,
            updated_at = item.updatedAt,
            locked_at = item.lockedAt,
            last_error_code = item.lastErrorCode,
            last_error_message = item.lastErrorMessage,
        )
    }

    override suspend fun getPendingItems(
        nowMillis: Long,
        limit: Long
    ): List<SyncOutboxItem> =
        database.syncOutboxQueries.selectPendingItems(nowMillis, limit, ::mapOutboxItem)
            .executeAsList()

    override suspend fun markSyncItemInProgress(id: String, nowMillis: Long) {
        database.syncOutboxQueries.markInProgress(nowMillis, nowMillis, id)
    }

    override suspend fun markSyncItemSucceeded(id: String, nowMillis: Long) {
        database.syncOutboxQueries.markSucceeded(nowMillis, id)
    }

    override suspend fun markRetryWaiting(
        id: String,
        retryCount: Int,
        nextAttemptAt: Long,
        errorCode: String,
        errorMessage: String,
        nowMillis: Long
    ) {
        database.syncOutboxQueries.markRetryWaiting(
            retry_count = retryCount.toLong(),
            next_attempt_at = nextAttemptAt,
            last_error_code = errorCode,
            last_error_message = errorMessage,
            updated_at = nowMillis,
            id = id,
        )
    }

    override suspend fun markSyncItemFailed(
        id: String,
        errorCode: String,
        errorMessage: String,
        nowMillis: Long
    ) {
        database.syncOutboxQueries.markFailed(errorCode, errorMessage, nowMillis, id)
    }

    override suspend fun markSyncItemConflict(
        id: String,
        errorCode: String,
        errorMessage: String,
        nowMillis: Long
    ) {
        database.syncOutboxQueries.markConflict(errorCode, errorMessage, nowMillis, id)
    }

    override suspend fun resetStaleInProgress(
        expiredBeforeMillis: Long,
        nowMillis: Long
    ) {
        database.syncOutboxQueries.resetStaleInProgress(nowMillis, expiredBeforeMillis)
    }

    override suspend fun countSyncItemsPending(): Long =
        database.syncOutboxQueries.countPending().executeAsOne()

    override suspend fun countConflicts(): Long =
        database.syncOutboxQueries.countConflicts().executeAsOne()

    override suspend fun countFailed(): Long =
        database.syncOutboxQueries.countFailed().executeAsOne()

    override suspend fun isAggregateSynced(
        aggregateType: SyncAggregateType,
        aggregateId: String
    ): Boolean {
        val syncedAt = when (aggregateType) {
            SyncAggregateType.ORDER -> database.syncOutboxQueries.selectOrderSyncedAt(aggregateId)
                .executeAsOneOrNull()

            SyncAggregateType.PAYMENT ->
                database.syncOutboxQueries.selectPaymentSyncedAt(aggregateId).executeAsOneOrNull()

            SyncAggregateType.REFUND ->
                database.syncOutboxQueries.selectRefundSyncedAt(aggregateId).executeAsOneOrNull()
        }
        return syncedAt != null
    }

    override fun markOrderSynced(time: Long, aggregateId: String) {
        database.syncOutboxQueries.markOrderSynced(synced_at = time, id = aggregateId)
    }

    override fun markPaymentSynced(time: Long, aggregateId: String) {
        database.syncOutboxQueries.markPaymentSynced(synced_at = time, id = aggregateId)
    }

    override fun markRefundSynced(time: Long, aggregateId: String) {
        database.syncOutboxQueries.markRefundSynced(synced_at = time, id = aggregateId)
    }

    override fun getRefundDependencies(aggregateId: String): SelectRefundDependencies? =
        database.syncOutboxQueries.selectRefundDependencies(aggregateId)
            .executeAsOneOrNull()

    override fun getPaymentSyncedAt(aggregateId: String): Long? =
        database.syncOutboxQueries.selectPaymentSyncedAt(aggregateId).executeAsOneOrNull()?.synced_at

    private fun mapMenuItem(
        id: String,
        category: String,
        name: String,
        description: String,
        image_url: String,
        local_image_url: String?,
        price: Long,
        last_synced_at: Long?,
        is_active: Long,
    ) = MenuItem(
        id = id,
        category = category,
        name = name,
        description = description,
        imageUrl = image_url,
        localImageUrl = local_image_url,
        price = price,
        lastSyncedAt = last_synced_at,
        isActive = is_active == 1L,
    )

    private fun mapDiscount(
        id: String,
        promoCode: String,
        percent: Double,
        minValue: Double,
        maxValue: Double,
        startAt: Long,
        endAt: Long,
        syncedAt: Long,
    ) = Discount(
        id = id,
        promoCode = promoCode,
        percent = percent,
        minValue = minValue,
        maxValue = maxValue,
        startAt = startAt,
        endAt = endAt,
        syncAt = syncedAt,
    )

    private fun mapOutboxItem(
        id: String,
        type: String,
        aggregate_id: String,
        aggregate_type: String,
        payload_json: String,
        idempotency_key: String,
        status: String,
        retry_count: Long,
        max_retries: Long,
        next_attempt_at: Long?,
        created_at: Long,
        updated_at: Long,
        locked_at: Long?,
        last_error_code: String?,
        last_error_message: String?,
    ) = SyncOutboxItem(
        id = id,
        type = SyncOutboxType.valueOf(type),
        aggregateId = aggregate_id,
        aggregateType = SyncAggregateType.valueOf(aggregate_type),
        payloadJson = payload_json,
        idempotencyKey = idempotency_key,
        status = SyncOutboxStatus.valueOf(status),
        retryCount = retry_count.toInt(),
        maxRetries = max_retries.toInt(),
        nextAttemptAt = next_attempt_at,
        createdAt = created_at,
        updatedAt = updated_at,
        lockedAt = locked_at,
        lastErrorCode = last_error_code,
        lastErrorMessage = last_error_message,
    )
}