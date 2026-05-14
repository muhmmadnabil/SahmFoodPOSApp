package com.sahm.pos.data.repo

import com.sahm.pos.data.local.SqlDelightLocalDataSource
import com.sahm.pos.data.remote.RemoteDataSource
import com.sahm.pos.domain.repository.SyncDataRepo

class SyncDataRepoImpl(
    private val sqlDelightLocalDataSource: SqlDelightLocalDataSource,
    private val remoteDataSource: RemoteDataSource
) : SyncDataRepo {
    override suspend fun hasUsers(): Boolean =
        sqlDelightLocalDataSource.hasUsers()

    override suspend fun syncUsers() {
        val users = remoteDataSource.getUsers()
        sqlDelightLocalDataSource.upsertUsers(users)
    }
}