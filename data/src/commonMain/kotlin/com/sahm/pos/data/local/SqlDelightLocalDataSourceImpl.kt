package com.sahm.pos.data.local

import com.sahm.pos.data.local.database.SahmPosDatabase
import com.sahm.pos.domain.entity.User

internal class SqlDelightLocalDataSourceImpl(
    private val database: SahmPosDatabase,
) : SqlDelightLocalDataSource {
    override suspend fun hasUsers(): Boolean =
        database.userQueries.hasUsers().executeAsOne()

    override suspend fun upsertUsers(users: List<User>) {
        database.transaction {
            users.forEach { user ->
                database.userQueries.upsert(
                    id = user.id,
                    username = user.username,
                    phone = user.phone,
                    password = user.password,
                    created_at = user.createdAt,
                    is_active = if (user.isActive) 1L else 0L,
                    last_login_at = user.lastLoginAt,
                )
            }
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
            )
        }

    override suspend fun updateUserLastLoginAt(userId: String, timestamp: String) {
        database.userQueries.updateLastLoginAt(
            last_login_at = timestamp,
            id = userId,
        )
    }
}