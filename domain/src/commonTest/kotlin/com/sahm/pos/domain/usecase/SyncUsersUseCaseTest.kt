package com.sahm.pos.domain.usecase

import com.sahm.pos.domain.SyncResult
import com.sahm.pos.domain.entity.MenuItem
import com.sahm.pos.domain.repository.SyncDataRepo
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SyncUsersUseCaseTest {

    @Test
    fun invokeCallsRepositorySyncUsers() = runTest {
        val repo = FakeSyncDataRepo()

        SyncUsersUseCase(repo).invoke()

        assertEquals(1, repo.syncUsersCalls)
    }

    @Test
    fun invokeReturnsRepositoryResult() = runTest {
        val expected = SyncResult.Success(syncedCount = 2, skippedInvalidCount = 1)

        val result = SyncUsersUseCase(FakeSyncDataRepo(result = expected)).invoke()

        assertEquals(expected, result)
    }

    private class FakeSyncDataRepo(
        private val result: SyncResult = SyncResult.Success(0, 0),
    ) : SyncDataRepo {
        var syncUsersCalls = 0

        override suspend fun hasUsers(): Boolean = false

        override suspend fun syncUsers(): SyncResult {
            syncUsersCalls += 1
            return result
        }

        override suspend fun syncMenuItems(): SyncResult = SyncResult.EmptyRemoteData

        override suspend fun getActiveMenuItems(): List<MenuItem> = emptyList()

        override suspend fun getUserCount(): Long = 0

        override suspend fun getMenuItemCount(): Long = 0

        override suspend fun getLastUsersSyncAt(): Long? = null

        override suspend fun getLastMenuItemsSyncAt(): Long? = null
    }
}
