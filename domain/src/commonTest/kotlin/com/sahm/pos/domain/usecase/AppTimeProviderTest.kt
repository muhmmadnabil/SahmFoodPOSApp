package com.sahm.pos.domain.usecase

import com.sahm.pos.domain.ClockProvider
import com.sahm.pos.domain.entity.TimeSyncInfo
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AppTimeProviderTest {
    @Test
    fun usesSavedOffset() = runTest {
        val provider = provider(phoneTime = 10_000, offset = 5_000)

        assertEquals(15_000, provider.nowMillis())
    }

    @Test
    fun noSavedOffsetUsesPhoneTime() = runTest {
        val provider = GetAppTimeUseCase(FakeSyncDataRepo(null), ClockProvider { 10_000 })

        assertEquals(10_000, provider.nowMillis())
    }

    @Test
    fun negativeOffset() = runTest {
        val provider = provider(phoneTime = 10_000, offset = -3_000)

        assertEquals(7_000, provider.nowMillis())
    }

    private fun provider(phoneTime: Long, offset: Long) =
        GetAppTimeUseCase(
            FakeSyncDataRepo(TimeSyncInfo(offset, 0, 0, 0)),
            ClockProvider { phoneTime },
        )

    private class FakeSyncDataRepo(
        private val info: TimeSyncInfo?,
    ) : com.sahm.pos.domain.repository.SyncDataRepo {
        override suspend fun getTimeSyncInfo(): TimeSyncInfo? = info
    }
}
