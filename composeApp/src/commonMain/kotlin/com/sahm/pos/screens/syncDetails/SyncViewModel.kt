package com.sahm.pos.screens.syncDetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sahm.pos.domain.SyncResult
import com.sahm.pos.domain.repository.SyncDataRepo
import com.sahm.pos.domain.usecase.GetMenuItemsUseCase
import com.sahm.pos.domain.usecase.SyncMenuItemsUseCase
import com.sahm.pos.domain.usecase.SyncUsersUseCase
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
import sahmfoodposapp.composeapp.generated.resources.sync_empty_remote_data
import sahmfoodposapp.composeapp.generated.resources.sync_items_permission_denied
import sahmfoodposapp.composeapp.generated.resources.sync_items_success
import sahmfoodposapp.composeapp.generated.resources.sync_items_success_with_warnings
import sahmfoodposapp.composeapp.generated.resources.sync_no_internet
import sahmfoodposapp.composeapp.generated.resources.sync_unknown_error
import sahmfoodposapp.composeapp.generated.resources.sync_users_permission_denied
import sahmfoodposapp.composeapp.generated.resources.sync_users_success
import sahmfoodposapp.composeapp.generated.resources.sync_users_success_with_warnings

class SyncViewModel(
    private val syncMenuItemsUseCase: SyncMenuItemsUseCase,
    private val syncUsersUseCase: SyncUsersUseCase,
    private val getMenuItemsUseCase: GetMenuItemsUseCase,
    private val syncDataRepo: SyncDataRepo,
) : ViewModel() {

    private val _state = MutableStateFlow(SyncUiState())
    val state: StateFlow<SyncUiState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<SyncEffect>()
    val effect: SharedFlow<SyncEffect> = _effect.asSharedFlow()

    fun onIntent(intent: SyncIntent) {
        when (intent) {
            SyncIntent.ScreenOpened -> loadSyncDetails()
            SyncIntent.SyncItemsClicked -> syncItems()
            SyncIntent.SyncUsersClicked -> syncUsers()
        }
    }

    private fun loadSyncDetails() {
        viewModelScope.launch {
            val itemCount = runCatching { getMenuItemsUseCase().size }.getOrDefault(0)
            val userCount = runCatching { syncDataRepo.getUserCount().toInt() }.getOrDefault(0)
            val itemLastSyncAt = runCatching { syncDataRepo.getLastMenuItemsSyncAt() }.getOrNull()
            val userLastSyncAt = runCatching { syncDataRepo.getLastUsersSyncAt() }.getOrNull()
            _state.update {
                it.copy(
                    localItemCount = itemCount,
                    localUserCount = userCount,
                    lastItemSyncAt = itemLastSyncAt,
                    lastUserSyncAt = userLastSyncAt,
                )
            }
        }
    }

    private fun syncUsers() {
        if (_state.value.isSyncingUsers) return

        _state.update { it.copy(isSyncingUsers = true) }
        viewModelScope.launch {
            val message = when (val result = syncUsersUseCase()) {
                SyncResult.EmptyRemoteData -> Res.string.sync_empty_remote_data
                SyncResult.NoInternet -> Res.string.sync_no_internet
                SyncResult.PermissionDenied -> Res.string.sync_users_permission_denied
                is SyncResult.Success -> {
                    loadSyncDetails()
                    _state.update {
                        it.copy(
                            lastUsersSyncedCount = result.syncedCount,
                            skippedInvalidUsersCount = result.skippedInvalidCount,
                        )
                    }
                    if (result.skippedInvalidCount > 0) {
                        Res.string.sync_users_success_with_warnings
                    } else {
                        Res.string.sync_users_success
                    }
                }
                SyncResult.UnknownError -> Res.string.sync_unknown_error
            }
            _state.update { it.copy(isSyncingUsers = false) }
            emitMessage(message)
        }
    }

    private fun syncItems() {
        if (_state.value.isSyncingItems) return

        _state.update { it.copy(isSyncingItems = true) }
        viewModelScope.launch {
            val message = when (val result = syncMenuItemsUseCase()) {
                SyncResult.EmptyRemoteData -> Res.string.sync_empty_remote_data
                SyncResult.NoInternet -> Res.string.sync_no_internet
                SyncResult.PermissionDenied -> Res.string.sync_items_permission_denied
                is SyncResult.Success -> {
                    loadSyncDetails()
                    _state.update {
                        it.copy(
                            lastItemsSyncedCount = result.syncedCount,
                            skippedInvalidItemsCount = result.skippedInvalidCount,
                        )
                    }
                    if (result.skippedInvalidCount > 0) {
                        Res.string.sync_items_success_with_warnings
                    } else {
                        Res.string.sync_items_success
                    }
                }

                SyncResult.UnknownError -> Res.string.sync_unknown_error
            }
            _state.update { it.copy(isSyncingItems = false) }
            emitMessage(message)
        }
    }

    private suspend fun emitMessage(message: StringResource) {
        _effect.emit(SyncEffect.ShowMessage(message))
    }
}
