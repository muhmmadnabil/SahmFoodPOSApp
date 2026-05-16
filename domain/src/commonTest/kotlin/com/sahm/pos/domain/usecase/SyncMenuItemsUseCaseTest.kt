package com.sahm.pos.domain.usecase

import com.sahm.pos.domain.SyncResult
import com.sahm.pos.domain.entity.Discount
import com.sahm.pos.domain.entity.MenuItem
import com.sahm.pos.domain.repository.SyncDataRepo
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SyncMenuItemsUseCaseTest {

    @Test
    fun invokeCallsRepositorySyncMenuItems() = runTest {
        val repo = FakeSyncDataRepo()

        SyncMenuItemsUseCase(repo).invoke()

        assertEquals(1, repo.syncMenuItemsCalls)
    }

    @Test
    fun invokeReturnsRepositorySuccessResult() = runTest {
        val expected = SyncResult.Success(syncedCount = 2, skippedInvalidCount = 0)
        val useCase = SyncMenuItemsUseCase(FakeSyncDataRepo(result = expected))

        assertEquals(expected, useCase())
    }

    @Test
    fun invokeReturnsRepositoryNoInternetResult() = runTest {
        val useCase = SyncMenuItemsUseCase(FakeSyncDataRepo(SyncResult.NoInternet))

        assertEquals(SyncResult.NoInternet, useCase())
    }

    @Test
    fun invokeReturnsRepositoryEmptyRemoteDataResult() = runTest {
        val useCase = SyncMenuItemsUseCase(FakeSyncDataRepo(SyncResult.EmptyRemoteData))

        assertEquals(SyncResult.EmptyRemoteData, useCase())
    }

    @Test
    fun invokeReturnsRepositoryPermissionDeniedResult() = runTest {
        val useCase = SyncMenuItemsUseCase(FakeSyncDataRepo(SyncResult.PermissionDenied))

        assertEquals(SyncResult.PermissionDenied, useCase())
    }

    @Test
    fun invokeReturnsRepositoryUnknownErrorResult() = runTest {
        val useCase = SyncMenuItemsUseCase(FakeSyncDataRepo(SyncResult.UnknownError))

        assertEquals(SyncResult.UnknownError, useCase())
    }

    private class FakeSyncDataRepo(
        private val result: SyncResult = SyncResult.Success(0, 0),
    ) : SyncDataRepo {
        var syncMenuItemsCalls = 0

        override suspend fun hasUsers(): Boolean = false

        override suspend fun syncUsers(): SyncResult = SyncResult.EmptyRemoteData

        override suspend fun syncMenuItems(): SyncResult {
            syncMenuItemsCalls += 1
            return result
        }

        override suspend fun syncDiscounts(): SyncResult = SyncResult.EmptyRemoteData

        override suspend fun getActiveMenuItems(): List<MenuItem> = emptyList()

        override suspend fun getDiscountByPromoCode(promoCode: String): Discount? = null

        override suspend fun getUserCount(): Long = 0

        override suspend fun getMenuItemCount(): Long = 0

        override suspend fun getLastUsersSyncAt(): Long? = null

        override suspend fun getLastMenuItemsSyncAt(): Long? = null
    }
}
