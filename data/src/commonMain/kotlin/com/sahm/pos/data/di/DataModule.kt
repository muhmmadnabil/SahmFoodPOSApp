package com.sahm.pos.data.di

import com.sahm.pos.data.remote.RemoteDataSource
import com.sahm.pos.data.remote.createRemoteDataSource
import com.sahm.pos.data.repo.AuthRepoImpl
import com.sahm.pos.domain.repository.AuthRepo
import org.koin.dsl.module

val dataModule = module {
    single<RemoteDataSource> { createRemoteDataSource() }
    single<AuthRepo> { AuthRepoImpl(get()) }
}
