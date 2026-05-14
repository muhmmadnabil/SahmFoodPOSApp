package com.sahm.pos.data.remote

import com.sahm.pos.domain.entity.User

interface RemoteDataSource {
    suspend fun createUser(user: User)
}

expect fun createRemoteDataSource(): RemoteDataSource
