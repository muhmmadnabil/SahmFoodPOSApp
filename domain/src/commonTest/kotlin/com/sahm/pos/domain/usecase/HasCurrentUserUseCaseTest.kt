package com.sahm.pos.domain.usecase

import com.sahm.pos.domain.entity.CurrentUser
import com.sahm.pos.domain.entity.User
import com.sahm.pos.domain.repository.AuthRepo
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HasCurrentUserUseCaseTest {

    @Test
    fun givenSavedCurrentUser_whenCheckCurrentUser_thenReturnsTrue() = runTest {
        val result = HasCurrentUserUseCase(FakeAuthRepo(currentUser = currentUser)).invoke()

        assertTrue(result)
    }

    @Test
    fun givenNoSavedCurrentUser_whenCheckCurrentUser_thenReturnsFalse() = runTest {
        val result = HasCurrentUserUseCase(FakeAuthRepo(currentUser = null)).invoke()

        assertFalse(result)
    }

    @Test
    fun givenRepositoryThrows_whenCheckCurrentUser_thenReturnsFalse() = runTest {
        val result = HasCurrentUserUseCase(FakeAuthRepo(throwOnCurrentUser = true)).invoke()

        assertFalse(result)
    }

    private class FakeAuthRepo(
        private val currentUser: CurrentUser? = null,
        private val throwOnCurrentUser: Boolean = false,
    ) : AuthRepo {
        override suspend fun getUserByPhone(phone: String): User? = null

        override suspend fun saveCurrentUser(currentUser: CurrentUser) = Unit

        override suspend fun getCurrentUser(): CurrentUser? {
            if (throwOnCurrentUser) error("Preferences unavailable")
            return currentUser
        }

        override suspend fun updateUserLastLoginAt(userId: String, timestamp: String) = Unit
    }

    private companion object {
        val currentUser = CurrentUser(
            id = "cashier-1",
            username = "Noura",
            phone = "01012345678",
        )
    }
}
