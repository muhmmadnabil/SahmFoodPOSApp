package com.sahm.pos.data.repo

import com.sahm.pos.data.local.CurrentUserLocalDataSource
import com.sahm.pos.data.local.LocalDataSource
import com.sahm.pos.data.remote.RemoteDataSource
import com.sahm.pos.domain.entity.CurrentUser
import com.sahm.pos.domain.entity.User
import com.sahm.pos.domain.repository.AuthRepo

class AuthRepoImpl(
    private val localDataSource: LocalDataSource,
    private val currentUserLocalDataSource: CurrentUserLocalDataSource,
    private val remoteDataSource: RemoteDataSource,
) : AuthRepo {
    override suspend fun hasUsers(): Boolean =
        localDataSource.hasUsers()

    override suspend fun syncUsers() {
        val users = remoteDataSource.getUsers()
        localDataSource.upsertUsers(users)
    }

    override suspend fun getUserByPhone(phone: String): User? =
        localDataSource.getUserByPhone(phone)

    override suspend fun saveCurrentUser(currentUser: CurrentUser) {
        currentUserLocalDataSource.saveCurrentUser(currentUser)
    }

    override suspend fun getCurrentUser(): CurrentUser? =
        currentUserLocalDataSource.getCurrentUser()

    override suspend fun updateUserLastLoginAt(userId: String, timestamp: String) {
        localDataSource.updateUserLastLoginAt(
            userId = userId,
            timestamp = timestamp,
        )
    }
}
