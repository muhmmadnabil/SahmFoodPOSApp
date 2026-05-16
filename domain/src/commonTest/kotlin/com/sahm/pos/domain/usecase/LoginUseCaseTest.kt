package com.sahm.pos.domain.usecase

import com.sahm.pos.domain.CurrentTimestampProvider
import com.sahm.pos.domain.results.LoginResult
import com.sahm.pos.domain.entity.CurrentUser
import com.sahm.pos.domain.entity.User
import com.sahm.pos.domain.repository.AuthRepo
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class LoginUseCaseTest {

    @Test
    fun givenEmptyPhone_whenLogin_thenReturnsEmptyPhone() = runTest {
        val result = loginUseCase().invoke("", validPassword)

        assertEquals(LoginResult.EmptyPhone, result)
    }

    @Test
    fun givenBlankPhone_whenLogin_thenReturnsEmptyPhone() = runTest {
        val result = loginUseCase().invoke("   ", validPassword)

        assertEquals(LoginResult.EmptyPhone, result)
    }

    @Test
    fun givenEmptyPassword_whenLogin_thenReturnsEmptyPassword() = runTest {
        val result = loginUseCase().invoke(validPhone, "")

        assertEquals(LoginResult.EmptyPassword, result)
    }

    @Test
    fun givenBlankPassword_whenLogin_thenReturnsEmptyPassword() = runTest {
        val result = loginUseCase().invoke(validPhone, "   ")

        assertEquals(LoginResult.EmptyPassword, result)
    }

    @Test
    fun givenExistingPhoneAndCorrectPassword_whenLogin_thenReturnsSuccess() = runTest {
        val result = loginUseCase().invoke(validPhone, validPassword)

        assertIs<LoginResult.Success>(result)
    }

    @Test
    fun givenSuccessLogin_whenLogin_thenUpdatesUserLastLoginAtWithCurrentTime() = runTest {
        val repo = FakeAuthRepo()

        loginUseCase(repo).invoke(validPhone, validPassword)

        assertEquals(cashier.id, repo.updatedLastLoginUserId)
        assertEquals(currentTimestamp, repo.updatedLastLoginTimestamp)
    }

    @Test
    fun givenSuccessLogin_whenLogin_thenSavesCurrentUserDetails() = runTest {
        val repo = FakeAuthRepo()

        loginUseCase(repo).invoke(validPhone, validPassword)

        assertEquals(
            CurrentUser(
                id = cashier.id,
                username = cashier.username,
                phone = cashier.phone,
            ),
            repo.savedCurrentUser,
        )
    }

    @Test
    fun givenUnknownPhone_whenLogin_thenReturnsInvalidCredentials() = runTest {
        val result = loginUseCase().invoke("01000000000", validPassword)

        assertEquals(LoginResult.InvalidCredentials, result)
    }

    @Test
    fun givenWrongPassword_whenLogin_thenReturnsInvalidCredentials() = runTest {
        val result = loginUseCase().invoke(validPhone, "9999999")

        assertEquals(LoginResult.InvalidCredentials, result)
    }

    @Test
    fun givenWrongPassword_whenLogin_thenDoesNotWriteCurrentUserOrLastLoginAt() = runTest {
        val repo = FakeAuthRepo()

        loginUseCase(repo).invoke(validPhone, "9999999")

        assertNull(repo.savedCurrentUser)
        assertNull(repo.updatedLastLoginUserId)
        assertNull(repo.updatedLastLoginTimestamp)
    }

    @Test
    fun givenUnknownPhone_whenLogin_thenDoesNotWriteCurrentUserOrLastLoginAt() = runTest {
        val repo = FakeAuthRepo()

        loginUseCase(repo).invoke("01000000000", validPassword)

        assertNull(repo.savedCurrentUser)
        assertNull(repo.updatedLastLoginUserId)
        assertNull(repo.updatedLastLoginTimestamp)
    }

    @Test
    fun givenPhoneWithLeadingAndTrailingSpaces_whenLogin_thenStillSucceeds() = runTest {
        val result = loginUseCase().invoke("  $validPhone  ", validPassword)

        assertIs<LoginResult.Success>(result)
    }

    @Test
    fun givenPhoneWithSpacesInside_whenLogin_thenNormalizesAndSucceedsIfSupported() = runTest {
        val result = loginUseCase().invoke("010 123 456 78", validPassword)

        assertIs<LoginResult.Success>(result)
    }

    @Test
    fun givenRepositoryThrowsWhenFindingUser_whenLogin_thenReturnsFailureAndDoesNotWrite() = runTest {
        val repo = FakeAuthRepo(throwOnFind = true)

        val result = loginUseCase(repo).invoke(validPhone, validPassword)

        assertIs<LoginResult.Failure>(result)
        assertNull(repo.savedCurrentUser)
        assertNull(repo.updatedLastLoginUserId)
    }

    @Test
    fun givenLastLoginUpdateThrows_whenLogin_thenReturnsFailureAndDoesNotSaveCurrentUser() = runTest {
        val repo = FakeAuthRepo(throwOnLastLoginUpdate = true)

        val result = loginUseCase(repo).invoke(validPhone, validPassword)

        assertIs<LoginResult.Failure>(result)
        assertNull(repo.savedCurrentUser)
    }

    @Test
    fun givenCurrentUserSaveThrows_whenLogin_thenReturnsFailure() = runTest {
        val result = loginUseCase(FakeAuthRepo(throwOnSaveCurrentUser = true))
            .invoke(validPhone, validPassword)

        assertIs<LoginResult.Failure>(result)
    }

    private fun loginUseCase(repo: FakeAuthRepo = FakeAuthRepo()) =
        LoginUseCase(
            authRepo = repo,
            currentTimestampProvider = CurrentTimestampProvider { currentTimestamp },
        )

    private class FakeAuthRepo(
        private val throwOnFind: Boolean = false,
        private val throwOnLastLoginUpdate: Boolean = false,
        private val throwOnSaveCurrentUser: Boolean = false,
    ) : AuthRepo {
        var savedCurrentUser: CurrentUser? = null
        var updatedLastLoginUserId: String? = null
        var updatedLastLoginTimestamp: String? = null

        override suspend fun getUserByPhone(phone: String): User? {
            if (throwOnFind) error("Database unavailable")
            return cashier.takeIf { it.phone == phone }
        }

        override suspend fun saveCurrentUser(currentUser: CurrentUser) {
            if (throwOnSaveCurrentUser) error("Preferences unavailable")
            savedCurrentUser = currentUser
        }

        override suspend fun getCurrentUser(): CurrentUser? = savedCurrentUser

        override suspend fun updateUserLastLoginAt(userId: String, timestamp: String) {
            if (throwOnLastLoginUpdate) error("Database write failed")
            updatedLastLoginUserId = userId
            updatedLastLoginTimestamp = timestamp
        }
    }

    private companion object {
        const val validPhone = "01012345678"
        const val validPassword = "1234567"
        const val currentTimestamp = "2026-05-14T10:15:30Z"

        val cashier = User(
            id = "cashier-1",
            username = "Noura",
            phone = validPhone,
            createdAt = "2026-01-01T00:00:00Z",
            isActive = true,
            lastLoginAt = "",
            password = validPassword,
            lastSyncAt = 1000,
        )
    }
}
