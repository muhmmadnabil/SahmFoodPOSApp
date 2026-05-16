package com.sahm.pos.domain.usecase

import com.sahm.pos.domain.SyncResult
import com.sahm.pos.domain.entity.Discount
import com.sahm.pos.domain.entity.MenuItem
import com.sahm.pos.domain.repository.SyncDataRepo
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SyncDiscountsUseCaseTest {
    @Test
    fun successReturnsRepositoryResult() = runTest {
        val result = SyncResult.Success(2, 0)

        assertEquals(result, SyncDiscountsUseCase(FakeSyncDataRepo(result))())
    }

    @Test
    fun failureReturnsRepositoryResult() = runTest {
        assertEquals(
            SyncResult.InvalidRemoteData,
            SyncDiscountsUseCase(FakeSyncDataRepo(SyncResult.InvalidRemoteData))(),
        )
    }

    @Test
    fun dependsOnlyOnRepositoryAndCallsSyncDiscounts() = runTest {
        val repo = FakeSyncDataRepo()

        SyncDiscountsUseCase(repo)()

        assertEquals(1, repo.syncDiscountsCalls)
    }

    private class FakeSyncDataRepo(
        private val result: SyncResult = SyncResult.Success(0, 0),
    ) : SyncDataRepo {
        var syncDiscountsCalls = 0
        override suspend fun hasUsers(): Boolean = false
        override suspend fun syncUsers(): SyncResult = SyncResult.EmptyRemoteData
        override suspend fun syncMenuItems(): SyncResult = SyncResult.EmptyRemoteData
        override suspend fun syncDiscounts(): SyncResult {
            syncDiscountsCalls += 1
            return result
        }
        override suspend fun getActiveMenuItems(): List<MenuItem> = emptyList()
        override suspend fun getDiscountByPromoCode(promoCode: String): Discount? = null
        override suspend fun getUserCount(): Long = 0
        override suspend fun getMenuItemCount(): Long = 0
        override suspend fun getLastUsersSyncAt(): Long? = null
        override suspend fun getLastMenuItemsSyncAt(): Long? = null
    }
}
