package com.sahm.pos.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sahm.pos.domain.LoginResult
import com.sahm.pos.domain.usecase.LoginUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import sahmfoodposapp.composeapp.generated.resources.Res
import sahmfoodposapp.composeapp.generated.resources.login_error_generic
import sahmfoodposapp.composeapp.generated.resources.login_error_invalid_credentials
import sahmfoodposapp.composeapp.generated.resources.login_error_password_required
import sahmfoodposapp.composeapp.generated.resources.login_error_password_too_short
import sahmfoodposapp.composeapp.generated.resources.login_error_phone_invalid
import sahmfoodposapp.composeapp.generated.resources.login_error_phone_required

class LoginViewModel(
    private val loginUseCase: LoginUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<LoginEffect>()
    val effect: SharedFlow<LoginEffect> = _effect.asSharedFlow()

    fun onIntent(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.PhoneChanged -> onPhoneChanged(intent.phone)
            is LoginIntent.PasswordChanged -> onPasswordChanged(intent.password)
            LoginIntent.TogglePasswordVisibility -> togglePasswordVisibility()
            LoginIntent.SubmitLogin -> submitLogin()
            LoginIntent.SyncClicked -> onSyncClicked()
        }
    }

    private fun onSyncClicked() {
        viewModelScope.launch {
            _effect.emit(LoginEffect.NavigateToSync)
        }
    }

    private fun onPhoneChanged(phone: String) {
        _state.update {
            it.copy(
                phone = phone,
                phoneError = null,
                generalError = null,
            )
        }
    }

    private fun onPasswordChanged(password: String) {
        _state.update {
            it.copy(
                password = password,
                passwordError = null,
                generalError = null,
            )
        }
    }

    private fun togglePasswordVisibility() {
        _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    private fun submitLogin() {
        val currentState = state.value
        if (currentState.isLoading) return

        val normalizedPhone = currentState.phone.toNormalizedPhoneOrNull()
        val phoneError = when {
            normalizedPhone?.isBlank() == true -> Res.string.login_error_phone_required
            normalizedPhone?.isValidPhone() != true -> Res.string.login_error_phone_invalid
            else -> null
        }
        val passwordError = when {
            currentState.password.isBlank() -> Res.string.login_error_password_required
            currentState.password.trim().length <= MinimumPasswordLength -> Res.string.login_error_password_too_short
            else -> null
        }

        if (phoneError != null || passwordError != null) {
            _state.update {
                it.copy(
                    phoneError = phoneError,
                    passwordError = passwordError,
                    generalError = null,
                )
            }
            return
        }

        _state.update {
            it.copy(
                isLoading = true,
                phoneError = null,
                passwordError = null,
                generalError = null,
            )
        }

        viewModelScope.launch {
            when (
                loginUseCase(
                    phone = normalizedPhone.orEmpty(),
                    password = currentState.password
                )
            ) {
                LoginResult.EmptyPhone -> showValidationError(phoneError = Res.string.login_error_phone_required)
                LoginResult.EmptyPassword -> showValidationError(passwordError = Res.string.login_error_password_required)
                LoginResult.InvalidCredentials -> showGeneralError(Res.string.login_error_invalid_credentials)
                is LoginResult.Failure -> showGeneralError(Res.string.login_error_generic)
                is LoginResult.Success -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            generalError = null,
                        )
                    }
                    _effect.emit(LoginEffect.NavigateToHome)
                }
            }
        }
    }

    private fun showValidationError(
        phoneError: StringResource? = null,
        passwordError: StringResource? = null,
    ) {
        _state.update {
            it.copy(
                isLoading = false,
                phoneError = phoneError,
                passwordError = passwordError,
                generalError = null,
            )
        }
    }

    private suspend fun showGeneralError(message: StringResource) {
        _state.update {
            it.copy(
                isLoading = false,
                generalError = message,
            )
        }
        _effect.emit(LoginEffect.ShowMessage(message))
    }

    private companion object {
        const val MinimumPasswordLength = 6
    }
}

private const val PhoneLength = 12
private const val PhonePrefix = "02"

private fun String.toNormalizedPhoneOrNull(): String? = buildString {
    for (char in this@toNormalizedPhoneOrNull) {
        if (char.isWhitespace()) continue
        append(char.toLatinDigitOrNull() ?: return null)
    }
}

private fun String.isValidPhone(): Boolean =
    length == PhoneLength &&
            startsWith(PhonePrefix) &&
            all { it.isLatinDigit() }

private fun Char.isLatinDigit(): Boolean = this in '0'..'9'

private fun Char.toLatinDigitOrNull(): Char? = when (this) {
    in '0'..'9' -> this
    in '\u0660'..'\u0669' -> '0' + (this - '\u0660')
    in '\u06F0'..'\u06F9' -> '0' + (this - '\u06F0')
    else -> null
}
