package com.sahm.pos.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sahm.pos.domain.usecase.GetCurrentUserUseCase
import com.sahm.pos.domain.usecase.LogoutUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sahmfoodposapp.composeapp.generated.resources.Res
import sahmfoodposapp.composeapp.generated.resources.settings_logout_failed

class SettingsViewModel(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val logoutUseCase: LogoutUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    private val _action = MutableSharedFlow<SettingsAction>()
    val action: SharedFlow<SettingsAction> = _action.asSharedFlow()

    fun onIntent(intent: SettingsIntent) {
        when (intent) {
            SettingsIntent.ScreenOpened -> loadCashier()
            SettingsIntent.LogoutClicked -> logout()
            SettingsIntent.SyncClicked -> navigateToSync()
        }
    }

    private fun loadCashier() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val currentUser = getCurrentUserUseCase()
            _state.update {
                it.copy(
                    currentUser = currentUser,
                    isLoading = false,
                )
            }
        }
    }

    private fun logout() {
        if (_state.value.isLoading) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            runCatching { logoutUseCase() }
                .onSuccess {
                    _state.update { state -> state.copy(isLoading = false) }
                    _action.emit(SettingsAction.NavigateToLogin)
                }
                .onFailure {
                    _state.update { state -> state.copy(isLoading = false) }
                    _action.emit(SettingsAction.ShowMessage(Res.string.settings_logout_failed))
                }
        }
    }

    private fun navigateToSync() {
        viewModelScope.launch {
            _action.emit(SettingsAction.NavigateToSync)
        }
    }
}
