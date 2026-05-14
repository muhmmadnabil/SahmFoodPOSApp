package com.sahm.pos.data.repo

import com.sahm.pos.data.local.DataStoreLocalDataSource
import com.sahm.pos.data.local.SqlDelightLocalDataSource
import com.sahm.pos.domain.entity.CurrentUser
import com.sahm.pos.domain.entity.User
import com.sahm.pos.domain.repository.AuthRepo

class AuthRepoImpl(
    private val sqlDelightLocalDataSource: SqlDelightLocalDataSource,
    private val dataStoreLocalDataSource: DataStoreLocalDataSource
) : AuthRepo {
    override suspend fun getUserByPhone(phone: String): User? =
        sqlDelightLocalDataSource.getUserByPhone(phone)

    override suspend fun saveCurrentUser(currentUser: CurrentUser) {
        dataStoreLocalDataSource.saveCurrentUser(currentUser)
    }

    override suspend fun getCurrentUser(): CurrentUser? =
        dataStoreLocalDataSource.getCurrentUser()

    override suspend fun updateUserLastLoginAt(userId: String, timestamp: String) {
        sqlDelightLocalDataSource.updateUserLastLoginAt(
            userId = userId,
            timestamp = timestamp,
        )
    }
}