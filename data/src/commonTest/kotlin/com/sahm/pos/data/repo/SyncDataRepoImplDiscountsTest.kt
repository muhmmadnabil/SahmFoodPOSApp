package com.sahm.pos.data.repo

import com.sahm.pos.data.local.SqlDelightLocalDataSource
import com.sahm.pos.data.model.RemoteDiscountDocument
import com.sahm.pos.data.model.RemoteMenuItemDocument
import com.sahm.pos.data.model.RemoteUserDocument
import com.sahm.pos.data.remote.RemoteDataException
import com.sahm.pos.data.remote.RemoteDataSource
import com.sahm.pos.data.remote.image.MenuItemImageCache
import com.sahm.pos.domain.results.SyncResult
import com.sahm.pos.domain.entity.Discount
import com.sahm.pos.domain.entity.MenuItem
import com.sahm.pos.domain.entity.User
import com.sahm.pos.domain.CurrentEpochMillisProvider
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SyncDataRepoImplDiscountsTest {
    @Test
    fun syncValidDiscounts() = runTest {
        val local = FakeLocalDataSource()
        val result = repo(local, FakeRemoteDataSource(listOf(validDocument))).syncDiscounts()

        assertEquals(SyncResult.Success(1, 0), result)
        assertEquals(listOf(discount()), local.savedDiscountSnapshots.single())
    }

    @Test
    fun remoteReturnsEmptyListClearsDiscounts() = runTest {
        val local = FakeLocalDataSource(existingDiscounts = listOf(discount()))
        val result = repo(local, FakeRemoteDataSource(emptyList())).syncDiscounts()

        assertEquals(SyncResult.Success(0, 0), result)
        assertEquals(emptyList(), local.savedDiscountSnapshots.single())
        assertEquals(emptyList(), local.discounts)
    }

    @Test
    fun remoteFailsDoesNotClearLocalDiscounts() = runTest {
        val local = FakeLocalDataSource(existingDiscounts = listOf(discount()))
        val result = repo(local, FakeRemoteDataSource(throwable = RemoteDataException.NoInternet)).syncDiscounts()

        assertEquals(SyncResult.NoInternet, result)
        assertTrue(local.savedDiscountSnapshots.isEmpty())
        assertEquals(listOf(discount()), local.discounts)
    }

    @Test
    fun localSaveFailsReturnsLocalStorageError() = runTest {
        val result = repo(
            FakeLocalDataSource(throwOnReplaceDiscounts = true),
            FakeRemoteDataSource(listOf(validDocument)),
        ).syncDiscounts()

        assertEquals(SyncResult.LocalStorageError, result)
    }

    @Test
    fun partialSuccessWithInvalidDiscountSavesValidOnly() = runTest {
        val local = FakeLocalDataSource()
        val result = repo(
            local,
            FakeRemoteDataSource(listOf(validDocument, validDocument.copy(id = "", promo = ""))),
        ).syncDiscounts()

        assertEquals(SyncResult.Success(1, 1), result)
        assertEquals(listOf(discount()), local.savedDiscountSnapshots.single())
    }

    @Test
    fun allDiscountsInvalidDoesNotReplaceLocalData() = runTest {
        val local = FakeLocalDataSource(existingDiscounts = listOf(discount()))
        val result = repo(local, FakeRemoteDataSource(listOf(validDocument.copy(id = "", promo = "")))).syncDiscounts()

        assertEquals(SyncResult.InvalidRemoteData, result)
        assertTrue(local.savedDiscountSnapshots.isEmpty())
        assertEquals(listOf(discount()), local.discounts)
    }

    @Test
    fun duplicatePromoCodeSameCaseFails() = runTest {
        val local = FakeLocalDataSource()
        val result = repo(
            local,
            FakeRemoteDataSource(listOf(validDocument, validDocument.copy(id = "discount-2"))),
        ).syncDiscounts()

        assertEquals(SyncResult.DuplicatePromoCode, result)
        assertTrue(local.savedDiscountSnapshots.isEmpty())
    }

    @Test
    fun promoCodeDifferentCaseIsAllowed() = runTest {
        val local = FakeLocalDataSource()
        val result = repo(
            local,
            FakeRemoteDataSource(listOf(validDocument, validDocument.copy(id = "discount-2", promo = "hello22"))),
        ).syncDiscounts()

        assertEquals(SyncResult.Success(2, 0), result)
        assertEquals(listOf("Hello22", "hello22"), local.discounts.map { it.promoCode })
    }

    @Test
    fun expiredDiscountsAreSaved() = runTest {
        val expired = validDocument.copy(startAt = 1, endAt = 2)
        val local = FakeLocalDataSource()

        repo(local, FakeRemoteDataSource(listOf(expired))).syncDiscounts()

        assertEquals(1, local.discounts.size)
    }

    @Test
    fun remoteDeletedDiscountLeavesLocalMatchingRemoteOnly() = runTest {
        val local = FakeLocalDataSource(existingDiscounts = listOf(discount(), discount(id = "old", promoCode = "Old22")))

        repo(local, FakeRemoteDataSource(listOf(validDocument))).syncDiscounts()

        assertEquals(listOf("discount-1"), local.discounts.map { it.id })
    }

    private fun repo(
        localDataSource: FakeLocalDataSource = FakeLocalDataSource(),
        remoteDataSource: RemoteDataSource = FakeRemoteDataSource(listOf(validDocument)),
    ) = SyncDataRepoImpl(
        sqlDelightLocalDataSource = localDataSource,
        remoteDataSource = remoteDataSource,
        currentEpochMillisProvider = CurrentEpochMillisProvider { syncAt },
        menuItemImageCache = NoOpImageCache,
    )

    private class FakeLocalDataSource(
        existingDiscounts: List<Discount> = emptyList(),
        private val throwOnReplaceDiscounts: Boolean = false,
    ) : SqlDelightLocalDataSource {
        val savedDiscountSnapshots = mutableListOf<List<Discount>>()
        var discounts = existingDiscounts.toMutableList()
        override suspend fun hasUsers(): Boolean = false
        override suspend fun upsertUsers(users: List<User>) = Unit
        override suspend fun replaceUsersSnapshot(users: List<User>) = Unit
        override suspend fun getUserByPhone(phone: String): User? = null
        override suspend fun updateUserLastLoginAt(userId: String, timestamp: String) = Unit
        override suspend fun getUserCount(): Long = 0
        override suspend fun getLastUsersSyncAt(): Long? = null
        override suspend fun replaceMenuItemsSnapshot(items: List<MenuItem>) = Unit
        override suspend fun getActiveMenuItems(): List<MenuItem> = emptyList()
        override suspend fun getMenuItemById(id: String): MenuItem? = null
        override suspend fun getMenuItemCountById(id: String): Long = 0
        override suspend fun getMenuItemCount(): Long = 0
        override suspend fun getLastMenuItemsSyncAt(): Long? = null
        override suspend fun replaceAllDiscounts(discounts: List<Discount>) {
            if (throwOnReplaceDiscounts) error("local failed")
            savedDiscountSnapshots += discounts
            this.discounts = discounts.toMutableList()
        }
        override suspend fun getAllDiscounts(): List<Discount> = discounts
        override suspend fun getDiscountByPromoCode(promoCode: String): Discount? =
            discounts.firstOrNull { it.promoCode == promoCode }
        override suspend fun getDiscountCount(): Long = discounts.size.toLong()
    }

    private class FakeRemoteDataSource(
        private val documents: List<RemoteDiscountDocument> = emptyList(),
        private val throwable: Throwable? = null,
    ) : RemoteDataSource {
        override suspend fun getUsers(): List<User> = emptyList()
        override suspend fun getUserDocuments(): List<RemoteUserDocument> = emptyList()
        override suspend fun getMenuItemDocuments(): List<RemoteMenuItemDocument> = emptyList()
        override suspend fun getDiscountDocuments(): List<RemoteDiscountDocument> {
            throwable?.let { throw it }
            return documents
        }
    }

    private object NoOpImageCache : MenuItemImageCache {
        override suspend fun cacheImage(itemId: String, imageUrl: String): String? = null
    }

    private companion object {
        const val syncAt = 1234L
        val validDocument = RemoteDiscountDocument(
            id = "discount-1",
            promo = "Hello22",
            percent = 10.0,
            minValue = 5.0,
            maxValue = 50.0,
            startAt = 1_000,
            endAt = 3_000,
        )

        fun discount(
            id: String = "discount-1",
            promoCode: String = "Hello22",
        ) = Discount(id, promoCode, 10.0, 5.0, 50.0, 1_000, 3_000, syncAt)
    }
}
