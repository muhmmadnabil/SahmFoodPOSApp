package com.sahm.pos.data.local

import com.sahm.pos.domain.entity.User

interface LocalDataSource {
    suspend fun hasUsers(): Boolean
    suspend fun upsertUsers(users: List<User>)
    suspend fun getUserByPhone(phone: String): User?
    suspend fun updateUserLastLoginAt(userId: String, timestamp: String)
}
