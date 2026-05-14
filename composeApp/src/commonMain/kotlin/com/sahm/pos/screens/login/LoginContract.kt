package com.sahm.pos.screens.login

import org.jetbrains.compose.resources.StringResource

data class LoginUiState(
    val phone: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val phoneError: StringResource? = null,
    val passwordError: StringResource? = null,
    val generalError: StringResource? = null,
)

sealed interface LoginIntent {
    data class PhoneChanged(val phone: String) : LoginIntent
    data class PasswordChanged(val password: String) : LoginIntent
    data object TogglePasswordVisibility : LoginIntent
    data object SubmitLogin : LoginIntent
}

sealed interface LoginEffect {
    data object NavigateToPos : LoginEffect
    data class ShowMessage(val message: StringResource) : LoginEffect
}