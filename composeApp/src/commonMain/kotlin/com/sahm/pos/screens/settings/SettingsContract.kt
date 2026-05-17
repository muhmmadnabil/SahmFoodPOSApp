package com.sahm.pos.screens.settings

import com.sahm.pos.domain.entity.CurrentUser
import org.jetbrains.compose.resources.StringResource

data class SettingsUiState(
    val currentUser: CurrentUser? = null,
    val isLoading: Boolean = false,
)

sealed interface SettingsIntent {
    data object ScreenOpened : SettingsIntent
    data object LogoutClicked : SettingsIntent
    data object SyncClicked : SettingsIntent
}

sealed interface SettingsAction {
    data object NavigateToLogin : SettingsAction
    data object NavigateToSync : SettingsAction
    data class ShowMessage(val message: StringResource) : SettingsAction
}
