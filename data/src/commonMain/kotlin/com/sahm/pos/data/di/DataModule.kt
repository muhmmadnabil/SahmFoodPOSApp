package com.sahm.pos.data.di

import com.sahm.pos.data.local.DataStoreLocalDataSource
import com.sahm.pos.data.local.DataStorePref
import com.sahm.pos.data.local.PlatformContext
import com.sahm.pos.data.local.SqlDelightLocalDataSource
import com.sahm.pos.data.local.SqlDelightLocalDataSourceImpl
import com.sahm.pos.data.local.createCurrentUserDataStore
import com.sahm.pos.data.local.createDatabaseDriver
import com.sahm.pos.data.local.database.SahmPosDatabase
import com.sahm.pos.data.remote.RemoteDataSource
import com.sahm.pos.data.remote.createRemoteDataSource
import com.sahm.pos.data.remote.image.createMenuItemImageCache
import com.sahm.pos.data.repo.AuthRepoImpl
import com.sahm.pos.data.repo.SyncDataRepoImpl
import com.sahm.pos.domain.repository.AuthRepo
import com.sahm.pos.domain.repository.SyncDataRepo
import com.sahm.pos.domain.usecase.CurrentEpochMillisProvider
import com.sahm.pos.domain.usecase.CurrentTimestampProvider
import com.sahm.pos.domain.usecase.GetMenuItemsUseCase
import com.sahm.pos.domain.usecase.HasCurrentUserUseCase
import com.sahm.pos.domain.usecase.HasUsersUseCase
import com.sahm.pos.domain.usecase.LoginUseCase
import com.sahm.pos.domain.usecase.SyncMenuItemsUseCase
import com.sahm.pos.domain.usecase.SyncUsersUseCase
import com.sahm.pos.domain.usecase.SystemCurrentEpochMillisProvider
import com.sahm.pos.domain.usecase.SystemCurrentTimestampProvider
import org.koin.dsl.module

fun dataModule(platformContext: PlatformContext) = module {
    single { SahmPosDatabase(createDatabaseDriver(platformContext)) }
    single { createCurrentUserDataStore(platformContext) }
    single<SqlDelightLocalDataSource> { SqlDelightLocalDataSourceImpl(get()) }
    single<DataStoreLocalDataSource> { DataStorePref(get()) }
    single<RemoteDataSource> { createRemoteDataSource() }
    single { createMenuItemImageCache(platformContext) }
    single<AuthRepo> { AuthRepoImpl(get(), get()) }
    single { SyncDataRepoImpl(get(), get(), get(), get()) }
    single<SyncDataRepo> { get<SyncDataRepoImpl>() }
    single<CurrentTimestampProvider> { SystemCurrentTimestampProvider() }
    single<CurrentEpochMillisProvider> { SystemCurrentEpochMillisProvider() }
    factory { HasCurrentUserUseCase(get()) }
    factory { HasUsersUseCase(get()) }
    factory { GetMenuItemsUseCase(get()) }
    factory { LoginUseCase(get(), get()) }
    factory { SyncMenuItemsUseCase(get()) }
    factory { SyncUsersUseCase(get()) }
}
