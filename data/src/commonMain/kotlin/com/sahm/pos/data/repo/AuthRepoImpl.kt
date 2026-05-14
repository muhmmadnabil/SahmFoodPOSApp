package com.sahm.pos.data.repo

import com.sahm.pos.data.remote.RemoteDataSource
import com.sahm.pos.domain.repository.AuthRepo

class AuthRepoImpl(
    private val remoteDataSource: RemoteDataSource,
) : AuthRepo {
}
