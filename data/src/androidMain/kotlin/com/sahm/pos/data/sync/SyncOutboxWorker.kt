package com.sahm.pos.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sahm.pos.data.local.DataStorePref
import com.sahm.pos.data.local.PlatformContext
import com.sahm.pos.data.local.SqlDelightLocalDataSourceImpl
import com.sahm.pos.data.local.createCurrentUserDataStore
import com.sahm.pos.data.local.createDatabaseDriver
import com.sahm.pos.data.local.database.SahmPosDatabase
import com.sahm.pos.data.remote.TimeRemoteDataSourceImpl
import com.sahm.pos.data.remote.createRemoteDataSource
import com.sahm.pos.data.remote.image.createMenuItemImageCache
import com.sahm.pos.data.repo.SyncDataRepoImpl
import com.sahm.pos.domain.SystemClockProvider
import com.sahm.pos.domain.SystemCurrentEpochMillisProvider
import com.sahm.pos.domain.results.SyncProcessorResult

class SyncOutboxWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val platformContext = PlatformContext(applicationContext)
        val database = SahmPosDatabase(createDatabaseDriver(platformContext))
        val localDataSource = SqlDelightLocalDataSourceImpl(database)
        val dataStore = DataStorePref(createCurrentUserDataStore(platformContext))
        val epochMillisProvider = SystemCurrentEpochMillisProvider()
        val repo = SyncDataRepoImpl(
            sqlDelightLocalDataSource = localDataSource,
            remoteDataSource = createRemoteDataSource(),
            dataStoreLocalDataSource = dataStore,
            timeRemoteDataSource = TimeRemoteDataSourceImpl(),
            currentEpochMillisProvider = epochMillisProvider,
            menuItemImageCache = createMenuItemImageCache(platformContext),
        )
        val processor = SyncOutboxProcessorImpl(
            repo = repo,
            clockProvider = epochMillisProvider,
        )

        return when (processor.processPending()) {
            SyncProcessorResult.Success -> Result.success()
            SyncProcessorResult.NeedsRetry -> Result.retry()
            is SyncProcessorResult.Failure -> Result.failure()
        }
    }
}
