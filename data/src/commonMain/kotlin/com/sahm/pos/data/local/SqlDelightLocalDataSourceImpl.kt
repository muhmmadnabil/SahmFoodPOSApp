package com.sahm.pos.data.local

import com.sahm.pos.data.local.database.SahmPosDatabase
import com.sahm.pos.domain.entity.MenuItem
import com.sahm.pos.domain.entity.User

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
}