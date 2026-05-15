package com.sahm.pos.login

import com.sahm.pos.domain.entity.CurrentUser
import com.sahm.pos.domain.entity.User
import com.sahm.pos.domain.repository.AuthRepo
import com.sahm.pos.domain.usecase.LoginUseCase
import com.sahm.pos.screens.login.LoginEffect
import com.sahm.pos.screens.login.LoginIntent
import com.sahm.pos.screens.login.LoginUiState
import com.sahm.pos.screens.login.LoginViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import sahmfoodposapp.composeapp.generated.resources.Res
import sahmfoodposapp.composeapp.generated.resources.login_error_invalid_credentials
import sahmfoodposapp.composeapp.generated.resources.login_error_password_required
import sahmfoodposapp.composeapp.generated.resources.login_error_phone_invalid
import sahmfoodposapp.composeapp.generated.resources.login_error_phone_required

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun whenViewModelCreated_thenStateIsDefault() = runTest {
        val viewModel = viewModel()

        assertEquals(LoginUiState(), viewModel.state.value)
    }

    @Test
    fun whenPhoneChanged_thenStatePhoneIsUpdated() = runTest {
        val viewModel = viewModel()

        viewModel.onIntent(LoginIntent.PhoneChanged(validPhone))

        assertEquals(validPhone, viewModel.state.value.phone)
    }

    @Test
    fun givenPhoneError_whenPhoneChanged_thenPhoneErrorIsCleared() = runTest {
        val viewModel = viewModel()
        viewModel.onIntent(LoginIntent.SubmitLogin)

        viewModel.onIntent(LoginIntent.PhoneChanged(validPhone))

        assertNull(viewModel.state.value.phoneError)
    }

    @Test
    fun givenGeneralError_whenPhoneChanged_thenGeneralErrorIsCleared() = runTest {
        val viewModel = viewModel(FakeAuthRepo(cashierPassword = "7654321"))
        viewModel.onIntent(LoginIntent.PhoneChanged(validPhone))
        viewModel.onIntent(LoginIntent.PasswordChanged(validPassword))
        viewModel.onIntent(LoginIntent.SubmitLogin)
        advanceUntilIdle()

        viewModel.onIntent(LoginIntent.PhoneChanged("01099999999"))

        assertNull(viewModel.state.value.generalError)
    }

    @Test
    fun whenPasswordChanged_thenStatePasswordIsUpdated() = runTest {
        val viewModel = viewModel()

        viewModel.onIntent(LoginIntent.PasswordChanged(validPassword))

        assertEquals(validPassword, viewModel.state.value.password)
    }

    @Test
    fun givenPasswordError_whenPasswordChanged_thenPasswordErrorIsCleared() = runTest {
        val viewModel = viewModel()
        viewModel.onIntent(LoginIntent.PhoneChanged(validPhone))
        viewModel.onIntent(LoginIntent.SubmitLogin)

        viewModel.onIntent(LoginIntent.PasswordChanged(validPassword))

        assertNull(viewModel.state.value.passwordError)
    }

    @Test
    fun givenGeneralError_whenPasswordChanged_thenGeneralErrorIsCleared() = runTest {
        val viewModel = viewModel(FakeAuthRepo(cashierPassword = "7654321"))
        viewModel.onIntent(LoginIntent.PhoneChanged(validPhone))
        viewModel.onIntent(LoginIntent.PasswordChanged(validPassword))
        viewModel.onIntent(LoginIntent.SubmitLogin)
        advanceUntilIdle()

        viewModel.onIntent(LoginIntent.PasswordChanged("1111111"))

        assertNull(viewModel.state.value.generalError)
    }

    @Test
    fun whenTogglePasswordVisibility_thenStateIsPasswordVisibleChanges() = runTest {
        val viewModel = viewModel()

        viewModel.onIntent(LoginIntent.TogglePasswordVisibility)

        assertTrue(viewModel.state.value.isPasswordVisible)
    }

    @Test
    fun whenTogglePasswordVisibilityTwice_thenStateReturnsToOriginal() = runTest {
        val viewModel = viewModel()

        viewModel.onIntent(LoginIntent.TogglePasswordVisibility)
        viewModel.onIntent(LoginIntent.TogglePasswordVisibility)

        assertFalse(viewModel.state.value.isPasswordVisible)
    }

    @Test
    fun whenTogglePasswordVisibility_thenPasswordValueIsNotCleared() = runTest {
        val viewModel = viewModel()
        viewModel.onIntent(LoginIntent.PasswordChanged(validPassword))

        viewModel.onIntent(LoginIntent.TogglePasswordVisibility)

        assertEquals(validPassword, viewModel.state.value.password)
    }

    @Test
    fun givenEmptyPhone_whenSubmitLogin_thenPhoneErrorIsShown() = runTest {
        val viewModel = viewModel()
        viewModel.onIntent(LoginIntent.PasswordChanged(validPassword))

        viewModel.onIntent(LoginIntent.SubmitLogin)

        assertEquals(Res.string.login_error_phone_required, viewModel.state.value.phoneError)
    }

    @Test
    fun givenEmptyPassword_whenSubmitLogin_thenPasswordErrorIsShown() = runTest {
        val viewModel = viewModel()
        viewModel.onIntent(LoginIntent.PhoneChanged(validPhone))

        viewModel.onIntent(LoginIntent.SubmitLogin)

        assertEquals(Res.string.login_error_password_required, viewModel.state.value.passwordError)
    }

    @Test
    fun givenEmptyPhoneAndPassword_whenSubmitLogin_thenBothErrorsAreShown() = runTest {
        val viewModel = viewModel()

        viewModel.onIntent(LoginIntent.SubmitLogin)

        assertEquals(Res.string.login_error_phone_required, viewModel.state.value.phoneError)
        assertEquals(Res.string.login_error_password_required, viewModel.state.value.passwordError)
    }

    @Test
    fun givenValidationError_whenSubmitLogin_thenUseCaseIsNotCalled() = runTest {
        val repo = FakeAuthRepo()
        val viewModel = viewModel(repo)

        viewModel.onIntent(LoginIntent.SubmitLogin)

        assertEquals(0, repo.getCashierCalls)
    }

    @Test
    fun givenInvalidPhone_whenSubmitLogin_thenPhoneErrorIsShown() = runTest {
        val viewModel = viewModel()
        viewModel.onIntent(LoginIntent.PhoneChanged("01012345678"))
        viewModel.onIntent(LoginIntent.PasswordChanged(validPassword))

        viewModel.onIntent(LoginIntent.SubmitLogin)

        assertEquals(Res.string.login_error_phone_invalid, viewModel.state.value.phoneError)
    }

    @Test
    fun givenInvalidPhone_whenSubmitLogin_thenUseCaseIsNotCalled() = runTest {
        val repo = FakeAuthRepo()
        val viewModel = viewModel(repo)
        viewModel.onIntent(LoginIntent.PhoneChanged("02101234567a"))
        viewModel.onIntent(LoginIntent.PasswordChanged(validPassword))

        viewModel.onIntent(LoginIntent.SubmitLogin)

        assertEquals(0, repo.getCashierCalls)
    }

    @Test
    fun givenArabicDigitsPhone_whenSubmitLogin_thenNormalizedPhoneIsUsed() = runTest {
        val viewModel = viewModel()
        viewModel.onIntent(LoginIntent.PhoneChanged("٠٢١٠١٢٣٤٥٦٧٨"))
        viewModel.onIntent(LoginIntent.PasswordChanged(validPassword))

        viewModel.onIntent(LoginIntent.SubmitLogin)
        advanceUntilIdle()

        assertNull(viewModel.state.value.phoneError)
        assertNull(viewModel.state.value.generalError)
    }

    @Test
    fun givenValidCredentials_whenSubmitLogin_thenLoadingStateIsShown() = runTest {
        val repo = FakeAuthRepo(waitForLookup = CompletableDeferred())
        val viewModel = viewModel(repo)
        viewModel.onIntent(LoginIntent.PhoneChanged(validPhone))
        viewModel.onIntent(LoginIntent.PasswordChanged(validPassword))

        viewModel.onIntent(LoginIntent.SubmitLogin)
        runCurrent()

        assertTrue(viewModel.state.value.isLoading)
        repo.waitForLookup?.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun givenValidCredentials_whenUseCaseSuccess_thenNavigateToHomeEffectIsEmitted() = runTest {
        val effects = mutableListOf<LoginEffect>()
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effect.toList(effects)
        }
        viewModel.onIntent(LoginIntent.PhoneChanged(validPhone))
        viewModel.onIntent(LoginIntent.PasswordChanged(validPassword))

        viewModel.onIntent(LoginIntent.SubmitLogin)
        advanceUntilIdle()

        assertTrue(effects.any { it == LoginEffect.NavigateToHome })
    }

    @Test
    fun givenValidCredentials_whenUseCaseSuccess_thenLoadingStateIsFalse() = runTest {
        val viewModel = viewModel()
        viewModel.onIntent(LoginIntent.PhoneChanged(validPhone))
        viewModel.onIntent(LoginIntent.PasswordChanged(validPassword))

        viewModel.onIntent(LoginIntent.SubmitLogin)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun givenValidCredentials_whenUseCaseSuccess_thenGeneralErrorIsNull() = runTest {
        val viewModel = viewModel()
        viewModel.onIntent(LoginIntent.PhoneChanged(validPhone))
        viewModel.onIntent(LoginIntent.PasswordChanged(validPassword))

        viewModel.onIntent(LoginIntent.SubmitLogin)
        advanceUntilIdle()

        assertNull(viewModel.state.value.generalError)
    }

    @Test
    fun givenInvalidCredentials_whenSubmitLogin_thenGeneralErrorIsInvalidCredentials() = runTest {
        val viewModel = viewModel(FakeAuthRepo(cashierPassword = "7654321"))
        viewModel.onIntent(LoginIntent.PhoneChanged(validPhone))
        viewModel.onIntent(LoginIntent.PasswordChanged(validPassword))

        viewModel.onIntent(LoginIntent.SubmitLogin)
        advanceUntilIdle()

        assertEquals(Res.string.login_error_invalid_credentials, viewModel.state.value.generalError)
    }

    @Test
    fun givenInvalidCredentials_whenSubmitLogin_thenNoNavigateEffect() = runTest {
        val effects = mutableListOf<LoginEffect>()
        val viewModel = viewModel(FakeAuthRepo(cashierPassword = "7654321"))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effect.toList(effects)
        }
        viewModel.onIntent(LoginIntent.PhoneChanged(validPhone))
        viewModel.onIntent(LoginIntent.PasswordChanged(validPassword))

        viewModel.onIntent(LoginIntent.SubmitLogin)
        advanceUntilIdle()

        assertFalse(effects.any { it == LoginEffect.NavigateToHome })
    }

    @Test
    fun givenInvalidCredentials_whenSubmitLogin_thenLoadingStateIsFalse() = runTest {
        val viewModel = viewModel(FakeAuthRepo(cashierPassword = "7654321"))
        viewModel.onIntent(LoginIntent.PhoneChanged(validPhone))
        viewModel.onIntent(LoginIntent.PasswordChanged(validPassword))

        viewModel.onIntent(LoginIntent.SubmitLogin)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun givenLoadingState_whenSubmitLoginAgain_thenUseCaseCalledOnlyOnce() = runTest {
        val repo = FakeAuthRepo(waitForLookup = CompletableDeferred())
        val viewModel = viewModel(repo)
        viewModel.onIntent(LoginIntent.PhoneChanged(validPhone))
        viewModel.onIntent(LoginIntent.PasswordChanged(validPassword))

        viewModel.onIntent(LoginIntent.SubmitLogin)
        runCurrent()
        viewModel.onIntent(LoginIntent.SubmitLogin)

        assertEquals(1, repo.getCashierCalls)
        repo.waitForLookup?.complete(Unit)
        advanceUntilIdle()
    }

    private fun TestScope.viewModel(repo: FakeAuthRepo = FakeAuthRepo()): LoginViewModel {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        return LoginViewModel(
            loginUseCase = LoginUseCase(repo)
        )
    }

    private class FakeAuthRepo(
        private val cashierPassword: String = validPassword,
        val waitForLookup: CompletableDeferred<Unit>? = null,
    ) : AuthRepo {
        var getCashierCalls = 0

        override suspend fun getUserByPhone(phone: String): User? {
            getCashierCalls += 1
            waitForLookup?.await()
            return cashier.copy(password = cashierPassword).takeIf { it.phone == phone }
        }

        override suspend fun saveCurrentUser(currentUser: CurrentUser) = Unit

        override suspend fun getCurrentUser(): CurrentUser? = null

        override suspend fun updateUserLastLoginAt(userId: String, timestamp: String) = Unit
    }

    private companion object {
        const val validPhone = "021012345678"
        const val validPassword = "1234567"

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
