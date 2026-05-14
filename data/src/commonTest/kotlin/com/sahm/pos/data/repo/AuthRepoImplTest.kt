package com.sahm.pos.data.repo

import com.sahm.pos.data.local.CurrentUserLocalDataSource
import com.sahm.pos.data.local.LocalDataSource
import com.sahm.pos.data.remote.RemoteDataSource
import com.sahm.pos.domain.entity.CurrentUser
import com.sahm.pos.domain.entity.User
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AuthRepoImplTest {

    @Test
    fun givenExistingPhone_whenGetUserByPhone_thenReturnsUser() = runTest {
        val repo = authRepo()

        val result = repo.getUserByPhone(user.phone)

        assertEquals(user, result)
    }

    @Test
    fun givenUnknownPhone_whenGetUserByPhone_thenReturnsNull() = runTest {
        val repo = authRepo()

        val result = repo.getUserByPhone("01000000000")

        assertNull(result)
    }

    @Test
    fun givenRemoteUsers_whenSyncUsers_thenUpsertsUsersLocally() = runTest {
        val local = FakeLocalDataSource()
        val repo = authRepo(
            localDataSource = local,
            remoteDataSource = FakeRemoteDataSource(users = listOf(user)),
        )

        repo.syncUsers()

        assertEquals(listOf(user), local.upsertedUsers)
    }

    @Test
    fun givenCurrentUser_whenSaveCurrentUser_thenSavesInCurrentUserLocalDataSource() = runTest {
        val currentUserLocal = FakeCurrentUserLocalDataSource()
        val repo = authRepo(currentUserLocalDataSource = currentUserLocal)

        repo.saveCurrentUser(currentUser)

        assertEquals(currentUser, currentUserLocal.savedCurrentUser)
    }

    @Test
    fun givenSavedCurrentUser_whenGetCurrentUser_thenReturnsCurrentUser() = runTest {
        val repo = authRepo(
            currentUserLocalDataSource = FakeCurrentUserLocalDataSource(
                currentUser = currentUser,
            )
        )

        val result = repo.getCurrentUser()

        assertEquals(currentUser, result)
    }

    @Test
    fun givenNoSavedCurrentUser_whenGetCurrentUser_thenReturnsNull() = runTest {
        val repo = authRepo()

        val result = repo.getCurrentUser()

        assertNull(result)
    }

    @Test
    fun givenLastLoginTimestamp_whenUpdateUserLastLoginAt_thenUpdatesLocalUserTable() = runTest {
        val local = FakeLocalDataSource()
        val repo = authRepo(localDataSource = local)

        repo.updateUserLastLoginAt(user.id, currentTimestamp)

        assertEquals(user.id, local.updatedLastLoginUserId)
        assertEquals(currentTimestamp, local.updatedLastLoginTimestamp)
    }

    private fun authRepo(
        localDataSource: FakeLocalDataSource = FakeLocalDataSource(),
        currentUserLocalDataSource: FakeCurrentUserLocalDataSource = FakeCurrentUserLocalDataSource(),
        remoteDataSource: RemoteDataSource = FakeRemoteDataSource(),
    ) = AuthRepoImpl(
        localDataSource = localDataSource,
        currentUserLocalDataSource = currentUserLocalDataSource,
        remoteDataSource = remoteDataSource,
    )

    private class FakeLocalDataSource : LocalDataSource {
        var upsertedUsers: List<User>? = null
        var updatedLastLoginUserId: String? = null
        var updatedLastLoginTimestamp: String? = null

        override suspend fun hasUsers(): Boolean = true

        override suspend fun upsertUsers(users: List<User>) {
            upsertedUsers = users
        }

        override suspend fun getUserByPhone(phone: String): User? =
            user.takeIf { it.phone == phone }

        override suspend fun updateUserLastLoginAt(userId: String, timestamp: String) {
            updatedLastLoginUserId = userId
            updatedLastLoginTimestamp = timestamp
        }
    }

    private class FakeCurrentUserLocalDataSource(
        private val currentUser: CurrentUser? = null,
    ) : CurrentUserLocalDataSource {
        var savedCurrentUser: CurrentUser? = null

        override suspend fun saveCurrentUser(currentUser: CurrentUser) {
            savedCurrentUser = currentUser
        }

        override suspend fun getCurrentUser(): CurrentUser? =
            savedCurrentUser ?: currentUser
    }

    private class FakeRemoteDataSource(
        private val users: List<User> = emptyList(),
    ) : RemoteDataSource {
        override suspend fun getUsers(): List<User> = users
    }

    private companion object {
        const val currentTimestamp = "2026-05-14T10:15:30Z"

        val user = User(
            id = "cashier-1",
            username = "Noura",
            phone = "01012345678",
            createdAt = "2026-01-01T00:00:00Z",
            isActive = true,
            lastLoginAt = "",
            password = "1234567",
        )

        val currentUser = CurrentUser(
            id = user.id,
            username = user.username,
            phone = user.phone,
        )
    }
}
