package com.sahm.pos.data.local

import com.sahm.pos.domain.entity.Discount
import com.sahm.pos.domain.entity.MenuItem
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
}