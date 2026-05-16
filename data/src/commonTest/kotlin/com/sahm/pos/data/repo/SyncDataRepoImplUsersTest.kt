package com.sahm.pos.data.repo

import com.sahm.pos.data.local.SqlDelightLocalDataSource
import com.sahm.pos.data.model.RemoteDiscountDocument
import com.sahm.pos.data.remote.RemoteDataException
import com.sahm.pos.data.remote.RemoteDataSource
import com.sahm.pos.data.model.RemoteMenuItemDocument
import com.sahm.pos.data.model.RemoteUserDocument
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

class SyncDataRepoImplUsersTest {

    @Test
    fun syncUsersFetchesUserDocumentsFromRemote() = runTest {
        val remote = FakeRemoteDataSource(userDocuments = listOf(validUserDocument))

        repo(remoteDataSource = remote).syncUsers()

        assertEquals(1, remote.getUserDocumentsCalls)
    }

    @Test
    fun syncUsersSavesValidRemoteUsersLocallyWithLastSyncAt() = runTest {
        val local = FakeSqlDelightLocalDataSource()

        repo(localDataSource = local, remoteDataSource = FakeRemoteDataSource(listOf(validUserDocument)))
            .syncUsers()

        assertEquals(listOf(user()), local.savedUserSnapshots.single())
    }

    @Test
    fun syncUsersReturnsSuccessWithSkippedInvalidCount() = runTest {
        val result = repo(
            remoteDataSource = FakeRemoteDataSource(
                listOf(validUserDocument, validUserDocument.copy(id = "bad", username = null))
            )
        ).syncUsers()

        assertEquals(SyncResult.Success(syncedCount = 1, skippedInvalidCount = 1), result)
    }

    @Test
    fun syncUsersReturnsEmptyRemoteDataAndDoesNotChangeLocalWhenRemoteEmpty() = runTest {
        val local = FakeSqlDelightLocalDataSource(existingUsers = listOf(user(id = "local")))

        val result = repo(localDataSource = local, remoteDataSource = FakeRemoteDataSource(emptyList()))
            .syncUsers()

        assertEquals(SyncResult.EmptyRemoteData, result)
        assertTrue(local.savedUserSnapshots.isEmpty())
        assertEquals(listOf(user(id = "local")), local.users)
    }

    @Test
    fun syncUsersMapsNoInternetAndDoesNotChangeLocal() = runTest {
        val local = FakeSqlDelightLocalDataSource(existingUsers = listOf(user(id = "local")))

        val result = repo(
            localDataSource = local,
            remoteDataSource = FakeRemoteDataSource(throwable = RemoteDataException.NoInternet),
        ).syncUsers()

        assertEquals(SyncResult.NoInternet, result)
        assertTrue(local.savedUserSnapshots.isEmpty())
        assertEquals(listOf(user(id = "local")), local.users)
    }

    @Test
    fun syncUsersMapsPermissionDenied() = runTest {
        val result = repo(
            remoteDataSource = FakeRemoteDataSource(throwable = RemoteDataException.PermissionDenied),
        ).syncUsers()

        assertEquals(SyncResult.PermissionDenied, result)
    }

    @Test
    fun syncUsersReturnsUnknownErrorWhenLocalDatabaseThrows() = runTest {
        val result = repo(
            localDataSource = FakeSqlDelightLocalDataSource(throwOnReplaceUsers = true),
            remoteDataSource = FakeRemoteDataSource(listOf(validUserDocument)),
        ).syncUsers()

        assertEquals(SyncResult.UnknownError, result)
    }

    private fun repo(
        localDataSource: FakeSqlDelightLocalDataSource = FakeSqlDelightLocalDataSource(),
        remoteDataSource: RemoteDataSource = FakeRemoteDataSource(listOf(validUserDocument)),
    ) = SyncDataRepoImpl(
        sqlDelightLocalDataSource = localDataSource,
        remoteDataSource = remoteDataSource,
        currentEpochMillisProvider = CurrentEpochMillisProvider { syncTimestamp },
        menuItemImageCache = NoOpImageCache,
    )

    private class FakeSqlDelightLocalDataSource(
        existingUsers: List<User> = emptyList(),
        private val throwOnReplaceUsers: Boolean = false,
    ) : SqlDelightLocalDataSource {
        val savedUserSnapshots = mutableListOf<List<User>>()
        var users = existingUsers.toMutableList()

        override suspend fun hasUsers(): Boolean = users.isNotEmpty()

        override suspend fun upsertUsers(users: List<User>) {
            replaceUsersSnapshot(users)
        }

        override suspend fun replaceUsersSnapshot(users: List<User>) {
            if (throwOnReplaceUsers) error("Database unavailable")
            savedUserSnapshots += users
            this.users = users.toMutableList()
        }

        override suspend fun getUserByPhone(phone: String): User? = null

        override suspend fun updateUserLastLoginAt(userId: String, timestamp: String) = Unit

        override suspend fun getUserCount(): Long = users.size.toLong()

        override suspend fun getLastUsersSyncAt(): Long? =
            users.map { it.lastSyncAt }.maxOrNull()

        override suspend fun replaceMenuItemsSnapshot(items: List<MenuItem>) = Unit

        override suspend fun getActiveMenuItems(): List<MenuItem> = emptyList()

        override suspend fun getMenuItemById(id: String): MenuItem? = null

        override suspend fun getMenuItemCountById(id: String): Long = 0

        override suspend fun getMenuItemCount(): Long = 0

        override suspend fun getLastMenuItemsSyncAt(): Long? = null

        override suspend fun replaceAllDiscounts(discounts: List<Discount>) = Unit

        override suspend fun getAllDiscounts(): List<Discount> = emptyList()

        override suspend fun getDiscountByPromoCode(promoCode: String): Discount? = null

        override suspend fun getDiscountCount(): Long = 0
    }

    private class FakeRemoteDataSource(
        private val userDocuments: List<RemoteUserDocument> = emptyList(),
        private val throwable: Throwable? = null,
    ) : RemoteDataSource {
        var getUserDocumentsCalls = 0

        override suspend fun getUsers(): List<User> = emptyList()

        override suspend fun getUserDocuments(): List<RemoteUserDocument> {
            getUserDocumentsCalls += 1
            throwable?.let { throw it }
            return userDocuments
        }

        override suspend fun getMenuItemDocuments(): List<RemoteMenuItemDocument> = emptyList()

        override suspend fun getDiscountDocuments(): List<RemoteDiscountDocument> = emptyList()
    }

    private object NoOpImageCache : MenuItemImageCache {
        override suspend fun cacheImage(itemId: String, imageUrl: String): String? = null
    }

    private companion object {
        const val syncTimestamp = 1234L

        val validUserDocument = RemoteUserDocument(
            id = "cashier-1",
            username = "Noura",
            phone = "021012345678",
            password = "1234567",
            createdAt = "2026-01-01T00:00:00Z",
            isActive = true,
            lastLoginAt = "",
        )

        fun user(id: String = validUserDocument.id) = User(
            id = id,
            username = validUserDocument.username.orEmpty(),
            phone = validUserDocument.phone.orEmpty(),
            password = validUserDocument.password.orEmpty(),
            createdAt = validUserDocument.createdAt,
            isActive = validUserDocument.isActive ?: true,
            lastLoginAt = validUserDocument.lastLoginAt,
            lastSyncAt = syncTimestamp,
        )
    }
}
