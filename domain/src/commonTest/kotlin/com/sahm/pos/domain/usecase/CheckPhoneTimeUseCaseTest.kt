package com.sahm.pos.domain.usecase

import com.sahm.pos.domain.ClockProvider
import com.sahm.pos.domain.DataError
import com.sahm.pos.domain.entity.TimeSyncInfo
import com.sahm.pos.domain.repository.SyncDataRepo
import com.sahm.pos.domain.results.CheckPhoneTimeResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class CheckPhoneTimeUseCaseTest {
    @Test
    fun validPhoneTimeWithZeroOffsetSavesInfo() = runTest {
        val repo = FakeSyncDataRepo(serverTime = 1100)
        val result = useCase(repo = repo, clock = FakeClockProvider(1000, 1200))()

        assertIs<CheckPhoneTimeResult.Valid>(result)
        assertEquals(0, result.info.offsetMillis)
        assertEquals(result.info, repo.savedInfo)
    }

    @Test
    fun validPhoneTimeWithinAllowedThresholdSavesOffset() = runTest {
        val repo = FakeSyncDataRepo(serverTime = 1_000)
        val result = useCase(
            repo = repo,
            clock = FakeClockProvider(1_000 + 60_000, 1_000 + 60_000),
        )()

        assertIs<CheckPhoneTimeResult.Valid>(result)
        assertEquals(-60_000, repo.savedInfo?.offsetMillis)
    }

    @Test
    fun invalidPhoneTimeAheadOfServerSavesNegativeOffset() = runTest {
        val repo = FakeSyncDataRepo(serverTime = 1_000)
        val result = useCase(repo = repo, clock = FakeClockProvider(400_001, 400_001))()

        assertIs<CheckPhoneTimeResult.Invalid>(result)
        assertEquals(-399_001, result.info.offsetMillis)
        assertEquals(result.info, repo.savedInfo)
    }

    @Test
    fun invalidPhoneTimeBehindServerSavesPositiveOffset() = runTest {
        val repo = FakeSyncDataRepo(serverTime = 400_001)
        val result = useCase(repo = repo, clock = FakeClockProvider(1_000, 1_000))()

        assertIs<CheckPhoneTimeResult.Invalid>(result)
        assertEquals(399_001, result.info.offsetMillis)
        assertEquals(result.info, repo.savedInfo)
    }

    @Test
    fun missingServerTimestampDoesNotOverwritePreviousInfo() = runTest {
        val previous = TimeSyncInfo(1, 2, 3, 4)
        val repo = FakeSyncDataRepo(serverTime = null, savedInfo = previous)
        val result = useCase(repo = repo)()

        assertEquals(CheckPhoneTimeResult.Failed(DataError.Remote.INVALID_REMOTE_DATA), result)
        assertEquals(previous, repo.savedInfo)
    }

    @Test
    fun invalidTimestampDoesNotSaveInfo() = runTest {
        val repo = FakeSyncDataRepo(serverTime = 0)
        val result = useCase(repo = repo)()

        assertEquals(CheckPhoneTimeResult.Failed(DataError.Remote.INVALID_REMOTE_DATA), result)
        assertNull(repo.savedInfo)
    }

    @Test
    fun localSaveFailureReturnsLocalStorageError() = runTest {
        val result = useCase(repo = FakeSyncDataRepo(throwOnSave = true))()

        assertEquals(CheckPhoneTimeResult.Failed(DataError.Local.LOCAL_STORAGE_ERROR), result)
    }

    @Test
    fun usesMidpointToCalculateEstimatedPhoneTime() = runTest {
        val repo = FakeSyncDataRepo(serverTime = 2000)
        val result = useCase(repo = repo, clock = FakeClockProvider(1000, 3000))()

        assertIs<CheckPhoneTimeResult.Valid>(result)
        assertEquals(2000, result.info.phoneTimeMillis)
        assertEquals(0, result.info.offsetMillis)
    }

    private fun useCase(
        repo: FakeSyncDataRepo = FakeSyncDataRepo(serverTime = 1_000),
        clock: ClockProvider = FakeClockProvider(1_000, 1_000),
    ) = CheckPhoneTimeUseCase(repo, clock)

    private class FakeSyncDataRepo(
        private val serverTime: Long? = 1_000,
        var savedInfo: TimeSyncInfo? = null,
        private val throwOnSave: Boolean = false,
    ) : SyncDataRepo {
        override suspend fun getServerTimeStamp(): Long? = serverTime

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
