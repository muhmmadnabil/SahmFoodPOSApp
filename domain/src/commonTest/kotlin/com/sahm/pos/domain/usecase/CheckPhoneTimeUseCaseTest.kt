package com.sahm.pos.domain.usecase

import com.sahm.pos.domain.DataError
import com.sahm.pos.domain.entity.TimeSyncInfo
import com.sahm.pos.domain.repository.TimeRemoteDataSource
import com.sahm.pos.domain.repository.TimeRemoteException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class CheckPhoneTimeUseCaseTest {
    @Test
    fun validPhoneTimeWithZeroOffsetSavesInfo() = runTest {
        val local = FakeTimeLocalDataSource()
        val result = useCase(
            remote = FakeTimeRemoteDataSource(Result.success(1100)),
            local = local,
            clock = FakeClockProvider(1000, 1200),
        )()

        assertIs<CheckPhoneTimeResult.Valid>(result)
        assertEquals(0, result.info.offsetMillis)
        assertEquals(result.info, local.savedInfo)
    }

    @Test
    fun validPhoneTimeWithinAllowedThresholdSavesOffset() = runTest {
        val local = FakeTimeLocalDataSource()
        val result = useCase(
            remote = FakeTimeRemoteDataSource(Result.success(1_000)),
            local = local,
            clock = FakeClockProvider(1_000 + 60_000, 1_000 + 60_000),
        )()

        assertIs<CheckPhoneTimeResult.Valid>(result)
        assertEquals(-60_000, local.savedInfo?.offsetMillis)
    }

    @Test
    fun invalidPhoneTimeAheadOfServerSavesNegativeOffset() = runTest {
        val local = FakeTimeLocalDataSource()
        val result = useCase(
            remote = FakeTimeRemoteDataSource(Result.success(1_000)),
            local = local,
            clock = FakeClockProvider(400_001, 400_001),
        )()

        assertIs<CheckPhoneTimeResult.Invalid>(result)
        assertEquals(-399_001, result.info.offsetMillis)
        assertEquals(result.info, local.savedInfo)
    }

    @Test
    fun invalidPhoneTimeBehindServerSavesPositiveOffset() = runTest {
        val local = FakeTimeLocalDataSource()
        val result = useCase(
            remote = FakeTimeRemoteDataSource(Result.success(400_001)),
            local = local,
            clock = FakeClockProvider(1_000, 1_000),
        )()

        assertIs<CheckPhoneTimeResult.Invalid>(result)
        assertEquals(399_001, result.info.offsetMillis)
        assertEquals(result.info, local.savedInfo)
    }

    @Test
    fun apiNoInternetDoesNotOverwritePreviousInfo() = runTest {
        val previous = TimeSyncInfo(1, 2, 3, 4)
        val local = FakeTimeLocalDataSource(savedInfo = previous)
        val result = useCase(
            remote = FakeTimeRemoteDataSource(Result.failure(TimeRemoteException(DataError.Remote.NO_INTERNET_CONNECTION))),
            local = local,
        )()

        assertEquals(CheckPhoneTimeResult.Failed(DataError.Remote.NO_INTERNET_CONNECTION), result)
        assertEquals(previous, local.savedInfo)
    }

    @Test
    fun apiTimeoutDoesNotOverwritePreviousInfo() = runTest {
        val previous = TimeSyncInfo(1, 2, 3, 4)
        val local = FakeTimeLocalDataSource(savedInfo = previous)
        val result = useCase(
            remote = FakeTimeRemoteDataSource(Result.failure(TimeRemoteException(DataError.Remote.REQUEST_TIMEOUT))),
            local = local,
        )()

        assertEquals(CheckPhoneTimeResult.Failed(DataError.Remote.REQUEST_TIMEOUT), result)
        assertEquals(previous, local.savedInfo)
    }

    @Test
    fun apiSerializationErrorReturnsFailed() = runTest {
        val result = useCase(
            remote = FakeTimeRemoteDataSource(Result.failure(TimeRemoteException(DataError.Remote.SERIALIZATION_ERROR))),
        )()

        assertEquals(CheckPhoneTimeResult.Failed(DataError.Remote.SERIALIZATION_ERROR), result)
    }

    @Test
    fun invalidTimestampDoesNotSaveInfo() = runTest {
        val local = FakeTimeLocalDataSource()
        val result = useCase(remote = FakeTimeRemoteDataSource(Result.success(0)), local = local)()

        assertEquals(CheckPhoneTimeResult.Failed(DataError.Remote.INVALID_REMOTE_DATA), result)
        assertNull(local.savedInfo)
    }

    @Test
    fun localSaveFailureReturnsLocalStorageError() = runTest {
        val result = useCase(local = FakeTimeLocalDataSource(throwOnSave = true))()

        assertEquals(CheckPhoneTimeResult.Failed(DataError.Local.LOCAL_STORAGE_ERROR), result)
    }

    @Test
    fun usesMidpointToCalculateEstimatedPhoneTime() = runTest {
        val local = FakeTimeLocalDataSource()
        val result = useCase(
            remote = FakeTimeRemoteDataSource(Result.success(2000)),
            local = local,
            clock = FakeClockProvider(1000, 3000),
        )()

        assertIs<CheckPhoneTimeResult.Valid>(result)
        assertEquals(2000, result.info.phoneTimeMillis)
        assertEquals(0, result.info.offsetMillis)
    }

    private fun useCase(
        remote: TimeRemoteDataSource = FakeTimeRemoteDataSource(Result.success(1_000)),
        local: FakeTimeLocalDataSource = FakeTimeLocalDataSource(),
        clock: ClockProvider = FakeClockProvider(1_000, 1_000),
    ) = CheckPhoneTimeUseCase(remote, local, clock)

    private class FakeTimeRemoteDataSource(
        private val result: Result<Long>,
    ) : TimeRemoteDataSource {
        override suspend fun getUnixTimeMillis(): Result<Long> = result
    }

    private class FakeTimeLocalDataSource(
        var savedInfo: TimeSyncInfo? = null,
        private val throwOnSave: Boolean = false,
    ) : TimeLocalDataSource {
        override suspend fun saveTimeSyncInfo(info: TimeSyncInfo) {
            if (throwOnSave) error("local failed")
            savedInfo = info
        }

        override suspend fun getTimeSyncInfo(): TimeSyncInfo? = savedInfo
    }

    private class FakeClockProvider(
        private vararg val values: Long,
    ) : ClockProvider {
        private var index = 0
        override fun nowMillis(): Long = values.getOrElse(index++) { values.last() }
    }
}
