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
    val pendingSyncCount: Long = 0,
    val failedSyncCount: Long = 0,
    val conflictSyncCount: Long = 0,
    val hasSyncWarning: Boolean = false,
)

sealed interface LoginIntent {
    data object ScreenStarted : LoginIntent
    data class PhoneChanged(val phone: String) : LoginIntent
    data class PasswordChanged(val password: String) : LoginIntent
    data object TogglePasswordVisibility : LoginIntent
    data object SubmitLogin : LoginIntent
    data object SyncClicked : LoginIntent
}

sealed interface LoginEffect {
    data object NavigateToHome : LoginEffect
    data object NavigateToSync : LoginEffect
    data class ShowMessage(val message: StringResource) : LoginEffect
}
