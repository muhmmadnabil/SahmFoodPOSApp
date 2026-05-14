package com.sahm.pos.domain.repository

import com.sahm.pos.domain.entity.CurrentUser
import com.sahm.pos.domain.entity.User

interface AuthRepo {
    suspend fun hasUsers(): Boolean
    suspend fun syncUsers()
    suspend fun getUserByPhone(phone: String): User?
    suspend fun saveCurrentUser(currentUser: CurrentUser)
    suspend fun getCurrentUser(): CurrentUser?
    suspend fun updateUserLastLoginAt(userId: String, timestamp: String)
}
