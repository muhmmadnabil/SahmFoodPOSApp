package com.sahm.pos.domain.repository

import com.sahm.pos.domain.SyncResult
import com.sahm.pos.domain.entity.Discount
import com.sahm.pos.domain.entity.MenuItem
import com.sahm.pos.domain.entity.TimeSyncInfo

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
}