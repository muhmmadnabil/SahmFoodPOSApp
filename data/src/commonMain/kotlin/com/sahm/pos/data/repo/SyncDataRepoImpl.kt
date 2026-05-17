package com.sahm.pos.data.repo

import com.sahm.pos.data.local.DataStoreLocalDataSource
import com.sahm.pos.data.local.SqlDelightLocalDataSource
import com.sahm.pos.data.mapper.toDiscountOrNull
import com.sahm.pos.data.mapper.toMenuItemOrNull
import com.sahm.pos.data.mapper.toUserOrNull
import com.sahm.pos.data.remote.RemoteDataException
import com.sahm.pos.data.remote.RemoteDataSource
import com.sahm.pos.data.remote.TimeRemoteDataSource
import com.sahm.pos.data.remote.image.MenuItemImageCache
import com.sahm.pos.domain.CurrentEpochMillisProvider
import com.sahm.pos.domain.entity.Discount
import com.sahm.pos.domain.entity.MenuItem
import com.sahm.pos.domain.entity.SyncAggregateType
import com.sahm.pos.domain.entity.SyncOutboxItem
import com.sahm.pos.domain.entity.SyncOutboxType
import com.sahm.pos.domain.entity.TimeSyncInfo
import com.sahm.pos.domain.repository.SyncDataRepo
import com.sahm.pos.domain.results.SyncResult
import com.sahm.pos.domain.results.SyncUploadResult

class SyncDataRepoImpl(
    private val sqlDelightLocalDataSource: SqlDelightLocalDataSource,
    private val remoteDataSource: RemoteDataSource,
    private val dataStoreLocalDataSource: DataStoreLocalDataSource,
    private val timeRemoteDataSource: TimeRemoteDataSource,
    private val currentEpochMillisProvider: CurrentEpochMillisProvider,
    private val menuItemImageCache: MenuItemImageCache,
) : SyncDataRepo {
    override suspend fun hasUsers(): Boolean =
        sqlDelightLocalDataSource.hasUsers()

    override suspend fun syncUsers(): SyncResult =
        try {
            val documents = remoteDataSource.getUserDocuments()
            if (documents.isEmpty()) return SyncResult.EmptyRemoteData
            val syncTimestamp = currentEpochMillisProvider.now()
            var skippedInvalidCount = 0
            val users = documents.mapNotNull { document ->
                document.toUserOrNull(syncTimestamp).also { mapped ->
                    if (mapped == null) skippedInvalidCount += 1
                }
            }

            if (users.isEmpty()) return SyncResult.EmptyRemoteData

            sqlDelightLocalDataSource.replaceUsersSnapshot(users)
            SyncResult.Success(
                syncedCount = users.size,
                skippedInvalidCount = skippedInvalidCount,
            )
        } catch (exception: RemoteDataException.NoInternet) {
            SyncResult.NoInternet
        } catch (exception: RemoteDataException.PermissionDenied) {
            SyncResult.PermissionDenied
        } catch (throwable: Throwable) {
            SyncResult.UnknownError
        }

    override suspend fun syncMenuItems(): SyncResult =
        try {
            val documents = remoteDataSource.getMenuItemDocuments()
            if (documents.isEmpty()) return SyncResult.EmptyRemoteData

            val syncTimestamp = currentEpochMillisProvider.now()
            var skippedInvalidCount = 0
            val menuItems = documents.mapNotNull { document ->
                val menuItem = document.toMenuItemOrNull(
                    lastSyncedAt = syncTimestamp,
                    localImageUrl = null,
                )
                if (menuItem == null) {
                    skippedInvalidCount += 1
                    null
                } else {
                    val cachedImageUrl = runCatching {
                        menuItemImageCache.cacheImage(menuItem.id, menuItem.imageUrl)
                    }.getOrNull()
                    menuItem.copy(localImageUrl = cachedImageUrl)
                }
            }

            if (menuItems.isEmpty()) return SyncResult.EmptyRemoteData

            sqlDelightLocalDataSource.replaceMenuItemsSnapshot(menuItems)
            SyncResult.Success(
                syncedCount = menuItems.size,
                skippedInvalidCount = skippedInvalidCount,
            )
        } catch (exception: RemoteDataException.NoInternet) {
            SyncResult.NoInternet
        } catch (exception: RemoteDataException.RequestTimeout) {
            SyncResult.RequestTimeout
        } catch (exception: RemoteDataException.PermissionDenied) {
            SyncResult.PermissionDenied
        } catch (throwable: Throwable) {
            SyncResult.UnknownError
        }

    override suspend fun syncDiscounts(): SyncResult =
        try {
            val documents = remoteDataSource.getDiscountDocuments()
            val syncTimestamp = currentEpochMillisProvider.now()
            var skippedInvalidCount = 0
            val discounts = documents.mapNotNull { document ->
                document.toDiscountOrNull(syncTimestamp).also { mapped ->
                    if (mapped == null) skippedInvalidCount += 1
                }
            }

            if (documents.isNotEmpty() && discounts.isEmpty()) {
                return SyncResult.InvalidRemoteData
            }

            if (discounts.hasDuplicatePromoCodes()) {
                return SyncResult.DuplicatePromoCode
            }

            try {
                sqlDelightLocalDataSource.replaceAllDiscounts(discounts)
            } catch (throwable: Throwable) {
                return SyncResult.LocalStorageError
            }

            SyncResult.Success(
                syncedCount = discounts.size,
                skippedInvalidCount = skippedInvalidCount,
            )
        } catch (exception: RemoteDataException.NoInternet) {
            SyncResult.NoInternet
        } catch (exception: RemoteDataException.RequestTimeout) {
            SyncResult.RequestTimeout
        } catch (exception: RemoteDataException.PermissionDenied) {
            SyncResult.PermissionDenied
        } catch (throwable: Throwable) {
            SyncResult.UnknownError
        }

    override suspend fun getActiveMenuItems(): List<MenuItem> =
        sqlDelightLocalDataSource.getActiveMenuItems()

    override suspend fun getDiscountByPromoCode(promoCode: String): Discount? =
        sqlDelightLocalDataSource.getDiscountByPromoCode(promoCode)

    override suspend fun getUserCount(): Long =
        sqlDelightLocalDataSource.getUserCount()

    override suspend fun getMenuItemCount(): Long =
        sqlDelightLocalDataSource.getMenuItemCount()

    override suspend fun getLastUsersSyncAt(): Long? =
        sqlDelightLocalDataSource.getLastUsersSyncAt()

    override suspend fun getLastMenuItemsSyncAt(): Long? =
        sqlDelightLocalDataSource.getLastMenuItemsSyncAt()

    override suspend fun saveTimeSyncInfo(info: TimeSyncInfo) {
        dataStoreLocalDataSource.saveTimeSyncInfo(info)
    }

    override suspend fun getTimeSyncInfo(): TimeSyncInfo? =
        dataStoreLocalDataSource.getTimeSyncInfo()


    override suspend fun getServerTimeStamp(): Long? {
        val result = timeRemoteDataSource.getUnixTimeMillis()

        return if (result.isSuccess) {
            result.getOrNull()
        } else {
            null
        }
    }


    override suspend fun getLastDiscountsSyncAt(): Long? =
        sqlDelightLocalDataSource.getLastDiscountsSyncAt()

    override suspend fun getDiscountsCount(): Int {
        return sqlDelightLocalDataSource.getDiscountsCount()
    }

    override suspend fun uploadData(item: SyncOutboxItem): SyncUploadResult {
        val result = remoteDataSource.uploadOutboxItem(item)
        if (result == SyncUploadResult.Success || result == SyncUploadResult.DuplicateIdempotencyKey) {
            markAggregateSynced(item)
        }
        return result
    }

    override suspend fun areDependenciesSatisfied(item: SyncOutboxItem): Boolean =
        when (item.type) {
            SyncOutboxType.CREATE_ORDER -> true
            SyncOutboxType.CREATE_PAYMENT -> sqlDelightLocalDataSource.getPaymentSyncedAt(item.aggregateId) != null

            SyncOutboxType.CREATE_REFUND -> {
                val dependency = sqlDelightLocalDataSource.getRefundDependencies(item.aggregateId)
                    ?: return false
                dependency.order_synced_at != null && dependency.payment_synced_at != null
            }
        }

    override suspend fun getSyncPendingItems(limit: Long): List<SyncOutboxItem> =
        sqlDelightLocalDataSource.getPendingItems(
            nowMillis = currentEpochMillisProvider.now(),
            limit = limit
        )


    override suspend fun makeSyncItemInProgress(id: String) {
        sqlDelightLocalDataSource.markSyncItemInProgress(id, currentEpochMillisProvider.now())
    }

    override suspend fun resetStaleInProgress(cutoffTime: Long) {
        sqlDelightLocalDataSource.resetStaleInProgress(
            nowMillis = currentEpochMillisProvider.now(),
            expiredBeforeMillis = cutoffTime
        )
    }

    override suspend fun markSyncItemSucceeded(id: String) {
        sqlDelightLocalDataSource.markSyncItemSucceeded(id, currentEpochMillisProvider.now())
    }

    override suspend fun markSyncItemFailed(
        id: String,
        errorCode: String,
        errorMessage: String,
    ) {
        sqlDelightLocalDataSource.markSyncItemFailed(
            id = id,
            errorCode = errorCode,
            errorMessage = errorMessage,
            nowMillis = currentEpochMillisProvider.now()
        )
    }

    override suspend fun markSyncItemConflict(
        id: String,
        errorCode: String,
        errorMessage: String,
    ) {
        sqlDelightLocalDataSource.markSyncItemConflict(
            id = id,
            errorCode = errorCode,
            errorMessage = errorMessage,
            nowMillis = currentEpochMillisProvider.now()
        )
    }

    override suspend fun markRetryWaiting(
        id: String,
        retryCount: Int,
        nextAttemptAt: Long,
        errorCode: String,
        errorMessage: String
    ) {
        sqlDelightLocalDataSource.markRetryWaiting(
            id = id,
            retryCount = retryCount,
            nextAttemptAt = nextAttemptAt,
            errorCode = errorCode,
            errorMessage = errorMessage,
            nowMillis = currentEpochMillisProvider.now()
        )
    }

    override suspend fun getCountSyncItemsPending(): Long =
        sqlDelightLocalDataSource.countSyncItemsPending()

    override suspend fun getCountSyncItemsFailed(): Long = sqlDelightLocalDataSource.countFailed()


    override suspend fun getCountSyncItemsConflicts(): Long =
        sqlDelightLocalDataSource.countConflicts()


    private fun markAggregateSynced(item: SyncOutboxItem) {
        val now = currentEpochMillisProvider.now()
        when (item.aggregateType) {
            SyncAggregateType.ORDER -> sqlDelightLocalDataSource.markOrderSynced(
                now,
                item.aggregateId
            )

            SyncAggregateType.PAYMENT -> sqlDelightLocalDataSource.markPaymentSynced(
                now,
                item.aggregateId
            )

            SyncAggregateType.REFUND -> sqlDelightLocalDataSource.markRefundSynced(
                now,
                item.aggregateId
            )
        }
    }

    private fun List<Discount>.hasDuplicatePromoCodes(): Boolean =
        map { it.promoCode }.let { promoCodes -> promoCodes.size != promoCodes.toSet().size }
}