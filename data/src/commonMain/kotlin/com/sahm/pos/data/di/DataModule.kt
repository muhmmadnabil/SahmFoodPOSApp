package com.sahm.pos.data.di

import com.sahm.pos.data.local.CurrentUserLocalDataSource
import com.sahm.pos.data.local.DataStorePref
import com.sahm.pos.data.local.LocalDataSource
import com.sahm.pos.data.local.PlatformContext
import com.sahm.pos.data.local.SqlDelightLocalDataSource
import com.sahm.pos.data.local.createCurrentUserDataStore
import com.sahm.pos.data.local.createDatabaseDriver
import com.sahm.pos.data.local.database.SahmPosDatabase
import com.sahm.pos.data.remote.RemoteDataSource
import com.sahm.pos.data.remote.createRemoteDataSource
import com.sahm.pos.data.repo.AuthRepoImpl
import com.sahm.pos.domain.repository.AuthRepo
import com.sahm.pos.domain.usecase.CurrentTimestampProvider
import com.sahm.pos.domain.usecase.HasCurrentUserUseCase
import com.sahm.pos.domain.usecase.HasUsersUseCase
import com.sahm.pos.domain.usecase.LoginUseCase
import com.sahm.pos.domain.usecase.SystemCurrentTimestampProvider
import com.sahm.pos.domain.usecase.SyncUsersUseCase
import org.koin.dsl.module

fun dataModule(platformContext: PlatformContext) = module {
    single { SahmPosDatabase(createDatabaseDriver(platformContext)) }
    single { createCurrentUserDataStore(platformContext) }
    single<LocalDataSource> { SqlDelightLocalDataSource(get()) }
    single<CurrentUserLocalDataSource> { DataStorePref(get()) }
    single<RemoteDataSource> { createRemoteDataSource() }
    single<AuthRepo> { AuthRepoImpl(get(), get(), get()) }
    single<CurrentTimestampProvider> { SystemCurrentTimestampProvider() }
    factory { HasCurrentUserUseCase(get()) }
    factory { HasUsersUseCase(get()) }
    factory { LoginUseCase(get(), get()) }
    factory { SyncUsersUseCase(get()) }
}
