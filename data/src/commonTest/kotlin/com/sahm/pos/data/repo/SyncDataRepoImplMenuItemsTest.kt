package com.sahm.pos.data.repo

import com.sahm.pos.data.local.SqlDelightLocalDataSource
import com.sahm.pos.data.model.RemoteDiscountDocument
import com.sahm.pos.data.remote.RemoteDataException
import com.sahm.pos.data.remote.RemoteDataSource
import com.sahm.pos.data.model.RemoteMenuItemDocument
import com.sahm.pos.data.remote.image.MenuItemImageCache
import com.sahm.pos.data.model.RemoteUserDocument
import com.sahm.pos.domain.SyncResult
import com.sahm.pos.domain.entity.Discount
import com.sahm.pos.domain.entity.MenuItem
import com.sahm.pos.domain.entity.User
import com.sahm.pos.domain.usecase.CurrentEpochMillisProvider
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SyncDataRepoImplMenuItemsTest {

    @Test
    fun syncMenuItemsFetchesItemsFromRemote() = runTest {
        val remote = FakeRemoteDataSource(documents = listOf(validDocument))

        repo(remoteDataSource = remote).syncMenuItems()

        assertEquals(1, remote.getMenuItemDocumentsCalls)
    }

    @Test
    fun syncMenuItemsSavesValidRemoteItemsLocally() = runTest {
        val local = FakeSqlDelightLocalDataSource()

        repo(localDataSource = local, remoteDataSource = FakeRemoteDataSource(listOf(validDocument)))
            .syncMenuItems()

        assertEquals(listOf(menuItem()), local.savedSnapshots.single())
    }

    @Test
    fun syncMenuItemsReturnsSuccessWithSyncedCount() = runTest {
        val result = repo(remoteDataSource = FakeRemoteDataSource(listOf(validDocument))).syncMenuItems()

        assertEquals(SyncResult.Success(syncedCount = 1, skippedInvalidCount = 0), result)
    }

    @Test
    fun syncMenuItemsReturnsSkippedInvalidCountWhenSomeDocumentsAreInvalid() = runTest {
        val result = repo(
            remoteDataSource = FakeRemoteDataSource(
                listOf(validDocument, validDocument.copy(id = "bad", name = null))
            )
        ).syncMenuItems()

        assertEquals(SyncResult.Success(syncedCount = 1, skippedInvalidCount = 1), result)
    }

    @Test
    fun syncMenuItemsReturnsEmptyRemoteDataWhenRemoteReturnsEmptyList() = runTest {
        val result = repo(remoteDataSource = FakeRemoteDataSource(emptyList())).syncMenuItems()

        assertEquals(SyncResult.EmptyRemoteData, result)
    }

    @Test
    fun syncMenuItemsDoesNotClearLocalDataWhenRemoteReturnsEmptyList() = runTest {
        val local = FakeSqlDelightLocalDataSource(existingItems = listOf(menuItem(id = "local")))

        repo(localDataSource = local, remoteDataSource = FakeRemoteDataSource(emptyList()))
            .syncMenuItems()

        assertTrue(local.savedSnapshots.isEmpty())
        assertEquals(listOf(menuItem(id = "local")), local.items)
    }

    @Test
    fun syncMenuItemsDoesNotClearLocalDataWhenRemoteThrowsNetworkError() = runTest {
        val local = FakeSqlDelightLocalDataSource(existingItems = listOf(menuItem(id = "local")))

        repo(
            localDataSource = local,
            remoteDataSource = FakeRemoteDataSource(throwable = RemoteDataException.NoInternet),
        ).syncMenuItems()

        assertTrue(local.savedSnapshots.isEmpty())
        assertEquals(listOf(menuItem(id = "local")), local.items)
    }

    @Test
    fun syncMenuItemsMapsNetworkErrorToNoInternet() = runTest {
        val result = repo(
            remoteDataSource = FakeRemoteDataSource(throwable = RemoteDataException.NoInternet),
        ).syncMenuItems()

        assertEquals(SyncResult.NoInternet, result)
    }

    @Test
    fun syncMenuItemsMapsPermissionDeniedToPermissionDenied() = runTest {
        val result = repo(
            remoteDataSource = FakeRemoteDataSource(throwable = RemoteDataException.PermissionDenied),
        ).syncMenuItems()

        assertEquals(SyncResult.PermissionDenied, result)
    }

    @Test
    fun syncMenuItemsMapsUnknownExceptionToUnknownError() = runTest {
        val result = repo(remoteDataSource = FakeRemoteDataSource(throwable = IllegalStateException()))
            .syncMenuItems()

        assertEquals(SyncResult.UnknownError, result)
    }

    @Test
    fun syncMenuItemsUpdatesExistingLocalItemWhenRemoteItemChanges() = runTest {
        val local = FakeSqlDelightLocalDataSource(existingItems = listOf(menuItem(name = "Old")))

        repo(
            localDataSource = local,
            remoteDataSource = FakeRemoteDataSource(listOf(validDocument.copy(name = "New"))),
        ).syncMenuItems()

        assertEquals("New", local.items.single().name)
    }

    @Test
    fun syncMenuItemsMarksRemovedRemoteItemInactiveIfSupported() = runTest {
        val removed = menuItem(id = "removed")
        val local = FakeSqlDelightLocalDataSource(existingItems = listOf(removed))

        repo(localDataSource = local, remoteDataSource = FakeRemoteDataSource(listOf(validDocument)))
            .syncMenuItems()

        assertFalse(local.items.single { it.id == removed.id }.isActive)
    }

    @Test
    fun syncMenuItemsUpdatesLastSyncedAtForEachSyncedItem() = runTest {
        val local = FakeSqlDelightLocalDataSource()

        repo(localDataSource = local, remoteDataSource = FakeRemoteDataSource(listOf(validDocument)))
            .syncMenuItems()

        assertEquals(syncTimestamp, local.items.single().lastSyncedAt)
    }

    @Test
    fun syncMenuItemsCachesImageAndSavesLocalImageUrlWhenAvailable() = runTest {
        val local = FakeSqlDelightLocalDataSource()

        repo(
            localDataSource = local,
            remoteDataSource = FakeRemoteDataSource(listOf(validDocument)),
            imageCache = FakeMenuItemImageCache(localPath = "file:///cache/burger.webp"),
        ).syncMenuItems()

        assertEquals("file:///cache/burger.webp", local.items.single().localImageUrl)
    }

    @Test
    fun syncMenuItemsDoesNotFailWhenImageCacheFails() = runTest {
        val result = repo(
            remoteDataSource = FakeRemoteDataSource(listOf(validDocument)),
            imageCache = FakeMenuItemImageCache(throwOnCache = true),
        ).syncMenuItems()

        assertIs<SyncResult.Success>(result)
    }

    @Test
    fun syncMenuItemsDoesNotUpdateLocalDataIfAllRemoteDocumentsAreInvalid() = runTest {
        val local = FakeSqlDelightLocalDataSource(existingItems = listOf(menuItem(id = "local")))

        val result = repo(
            localDataSource = local,
            remoteDataSource = FakeRemoteDataSource(listOf(validDocument.copy(id = "", name = null))),
        ).syncMenuItems()

        assertEquals(SyncResult.EmptyRemoteData, result)
        assertTrue(local.savedSnapshots.isEmpty())
        assertEquals(listOf(menuItem(id = "local")), local.items)
    }

    @Test
    fun syncMenuItemsIsIdempotentWhenCalledTwiceWithSameRemoteData() = runTest {
        val local = FakeSqlDelightLocalDataSource()
        val repo = repo(localDataSource = local, remoteDataSource = FakeRemoteDataSource(listOf(validDocument)))

        repo.syncMenuItems()
        repo.syncMenuItems()

        assertEquals(1, local.items.size)
        assertEquals(menuItem(), local.items.single())
    }

    @Test
    fun syncMenuItemsReturnsUnknownErrorWhenLocalDatabaseThrowsException() = runTest {
        val result = repo(
            localDataSource = FakeSqlDelightLocalDataSource(throwOnReplace = true),
            remoteDataSource = FakeRemoteDataSource(listOf(validDocument)),
        ).syncMenuItems()

        assertEquals(SyncResult.UnknownError, result)
    }

    private fun repo(
        localDataSource: FakeSqlDelightLocalDataSource = FakeSqlDelightLocalDataSource(),
        remoteDataSource: RemoteDataSource = FakeRemoteDataSource(listOf(validDocument)),
        imageCache: MenuItemImageCache = FakeMenuItemImageCache(),
    ) = SyncDataRepoImpl(
        sqlDelightLocalDataSource = localDataSource,
        remoteDataSource = remoteDataSource,
        currentEpochMillisProvider = CurrentEpochMillisProvider { syncTimestamp },
        menuItemImageCache = imageCache,
    )

    private class FakeSqlDelightLocalDataSource(
        existingItems: List<MenuItem> = emptyList(),
        private val throwOnReplace: Boolean = false,
    ) : SqlDelightLocalDataSource {
        val savedSnapshots = mutableListOf<List<MenuItem>>()
        var items = existingItems.toMutableList()

        override suspend fun hasUsers(): Boolean = false

        override suspend fun upsertUsers(users: List<User>) = Unit

        override suspend fun replaceUsersSnapshot(users: List<User>) = Unit

        override suspend fun getUserByPhone(phone: String): User? = null

        override suspend fun updateUserLastLoginAt(userId: String, timestamp: String) = Unit

        override suspend fun getUserCount(): Long = 0

        override suspend fun getLastUsersSyncAt(): Long? = null

        override suspend fun replaceMenuItemsSnapshot(items: List<MenuItem>) {
            if (throwOnReplace) error("Database unavailable")
            savedSnapshots += items
            val remoteIds = items.map { it.id }.toSet()
            this.items = this.items
                .map { local -> if (local.id in remoteIds) local else local.copy(isActive = false) }
                .filterNot { local -> local.id in remoteIds }
                .toMutableList()
            this.items += items
        }

        override suspend fun getActiveMenuItems(): List<MenuItem> =
            items.filter { it.isActive }

        override suspend fun getMenuItemById(id: String): MenuItem? =
            items.firstOrNull { it.id == id }

        override suspend fun getMenuItemCountById(id: String): Long =
            items.count { it.id == id }.toLong()

        override suspend fun getMenuItemCount(): Long = items.size.toLong()

        override suspend fun getLastMenuItemsSyncAt(): Long? =
            items.mapNotNull { it.lastSyncedAt }.maxOrNull()

        override suspend fun replaceAllDiscounts(discounts: List<Discount>) = Unit

        override suspend fun getAllDiscounts(): List<Discount> = emptyList()

        override suspend fun getDiscountByPromoCode(promoCode: String): Discount? = null

        override suspend fun getDiscountCount(): Long = 0
    }

    private class FakeRemoteDataSource(
        private val documents: List<RemoteMenuItemDocument> = emptyList(),
        private val throwable: Throwable? = null,
    ) : RemoteDataSource {
        var getMenuItemDocumentsCalls = 0

        override suspend fun getUsers(): List<User> = emptyList()

        override suspend fun getUserDocuments(): List<RemoteUserDocument> = emptyList()

        override suspend fun getMenuItemDocuments(): List<RemoteMenuItemDocument> {
            getMenuItemDocumentsCalls += 1
            throwable?.let { throw it }
            return documents
        }

        override suspend fun getDiscountDocuments(): List<RemoteDiscountDocument> = emptyList()
    }

    private class FakeMenuItemImageCache(
        private val localPath: String? = null,
        private val throwOnCache: Boolean = false,
    ) : MenuItemImageCache {
        override suspend fun cacheImage(itemId: String, imageUrl: String): String? {
            if (throwOnCache) error("Image download failed")
            return localPath
        }
    }

    private companion object {
        const val syncTimestamp = 1234L

        val validDocument = RemoteMenuItemDocument(
            id = "burger_animal_style",
            category = "Beef Burgers",
            name = "Animal Style",
            description = "Crispy onion rings",
            imageUrl = "https://example.com/burger.webp",
            price = 5702,
        )

        fun menuItem(
            id: String = validDocument.id,
            category: String = validDocument.category.orEmpty(),
            name: String = validDocument.name.orEmpty(),
            description: String = validDocument.description.orEmpty(),
            imageUrl: String = validDocument.imageUrl.orEmpty(),
            price: Long = validDocument.price ?: 0,
            localImageUrl: String? = null,
        ) = MenuItem(
            id = id,
            category = category,
            name = name,
            description = description,
            imageUrl = imageUrl,
            price = price,
            lastSyncedAt = syncTimestamp,
            isActive = true,
            localImageUrl = localImageUrl,
        )
    }
}
