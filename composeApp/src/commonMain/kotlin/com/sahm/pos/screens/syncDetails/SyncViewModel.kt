package com.sahm.pos.screens.syncDetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sahm.pos.domain.results.SyncResult
import com.sahm.pos.domain.usecase.GetDiscountsCountUseCase
import com.sahm.pos.domain.usecase.GetDiscountsLastSyncAtUseCase
import com.sahm.pos.domain.usecase.GetMenuItemsCountUseCase
import com.sahm.pos.domain.usecase.GetMenuItemsLastSyncUseCase
import com.sahm.pos.domain.usecase.GetUsersCountUseCase
import com.sahm.pos.domain.usecase.GetUsersLastSyncAtUseCase
import com.sahm.pos.domain.usecase.SyncDiscountsUseCase
import com.sahm.pos.domain.usecase.SyncMenuItemsUseCase
import com.sahm.pos.domain.usecase.SyncUsersUseCase
import com.sahm.pos.screens.sync.SyncDetailType
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
import sahmfoodposapp.composeapp.generated.resources.sync_discounts_duplicate_promo
import sahmfoodposapp.composeapp.generated.resources.sync_discounts_permission_denied
import sahmfoodposapp.composeapp.generated.resources.sync_discounts_success
import sahmfoodposapp.composeapp.generated.resources.sync_discounts_success_with_warnings
import sahmfoodposapp.composeapp.generated.resources.sync_empty_remote_data
import sahmfoodposapp.composeapp.generated.resources.sync_invalid_remote_data
import sahmfoodposapp.composeapp.generated.resources.sync_items_permission_denied
import sahmfoodposapp.composeapp.generated.resources.sync_items_success
import sahmfoodposapp.composeapp.generated.resources.sync_items_success_with_warnings
import sahmfoodposapp.composeapp.generated.resources.sync_local_storage_error
import sahmfoodposapp.composeapp.generated.resources.sync_no_internet
import sahmfoodposapp.composeapp.generated.resources.sync_request_timeout
import sahmfoodposapp.composeapp.generated.resources.sync_unknown_error
import sahmfoodposapp.composeapp.generated.resources.sync_users_permission_denied
import sahmfoodposapp.composeapp.generated.resources.sync_users_success
import sahmfoodposapp.composeapp.generated.resources.sync_users_success_with_warnings

class SyncViewModel(
    private val syncMenuItemsUseCase: SyncMenuItemsUseCase,
    private val syncUsersUseCase: SyncUsersUseCase,
    private val syncDiscountsUseCase: SyncDiscountsUseCase,
    private val getUsersCountUseCase: GetUsersCountUseCase,
    private val getMenuItemsCountUseCase: GetMenuItemsCountUseCase,
    private val getMenuItemsLastSyncUseCase: GetMenuItemsLastSyncUseCase,
    private val getUsersLastSyncAtUseCase: GetUsersLastSyncAtUseCase,
    private val getDiscountsLastSyncAtUseCase: GetDiscountsLastSyncAtUseCase,
    private val getDiscountsCountUseCase: GetDiscountsCountUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(SyncUiState())
    val state: StateFlow<SyncUiState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<SyncEffect>()
    val effect: SharedFlow<SyncEffect> = _effect.asSharedFlow()

    fun onIntent(intent: SyncIntent) {
        when (intent) {
            is SyncIntent.ScreenOpened -> {
                _state.update { it.copy(selectedType = intent.type) }
                loadSyncDetails()
            }

            SyncIntent.SyncItemsClicked -> syncItems()
            SyncIntent.SyncUsersClicked -> syncUsers()
            SyncIntent.SyncDiscountsClicked -> syncDiscounts()
        }
    }

    private fun loadSyncDetails() {
        viewModelScope.launch {
            val type = _state.value.selectedType ?: return@launch

            val details = when (type) {
                SyncDetailType.Users -> {
                    val lastSyncAt = runCatching { getUsersLastSyncAtUseCase() }.getOrDefault(0)
                    val count = runCatching { getUsersCountUseCase() }.getOrDefault(0)
                    Pair(lastSyncAt, count)
                }

                SyncDetailType.Items -> {
                    val lastSyncAt = runCatching { getMenuItemsLastSyncUseCase() }.getOrDefault(0)
                    val count = runCatching { getMenuItemsCountUseCase() }.getOrDefault(0)
                    Pair(lastSyncAt, count)
                }

                SyncDetailType.Discounts -> {
                    val lastSyncAt = runCatching { getDiscountsLastSyncAtUseCase() }.getOrDefault(0)
                    val count = runCatching { getDiscountsCountUseCase() }.getOrDefault(0)
                    Pair(lastSyncAt, count)
                }
            }
            _state.update {
                it.copy(
                    lastSyncAt = details.first,
                    count = details.second,
                )
            }
        }
    }

    private fun syncUsers() {
        if (_state.value.isSyncing) return

        _state.update { it.copy(isSyncing = true) }
        viewModelScope.launch {
            val message = when (val result = syncUsersUseCase()) {
                SyncResult.EmptyRemoteData -> Res.string.sync_empty_remote_data
                SyncResult.NoInternet -> Res.string.sync_no_internet
                SyncResult.RequestTimeout -> Res.string.sync_request_timeout
                SyncResult.PermissionDenied -> Res.string.sync_users_permission_denied
                SyncResult.InvalidRemoteData -> Res.string.sync_invalid_remote_data
                SyncResult.DuplicatePromoCode -> Res.string.sync_discounts_duplicate_promo
                SyncResult.LocalStorageError -> Res.string.sync_local_storage_error
                is SyncResult.Success -> {
                    loadSyncDetails()
                    _state.update {
                        it.copy(
                            lastSyncedCount = result.syncedCount,
                            skippedCount = result.skippedInvalidCount,
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
            _state.update { it.copy(isSyncing = false) }
            emitMessage(message)
        }
    }

    private fun syncItems() {
        if (_state.value.isSyncing) return

        _state.update { it.copy(isSyncing = true) }
        viewModelScope.launch {
            val message = when (val result = syncMenuItemsUseCase()) {
                SyncResult.EmptyRemoteData -> Res.string.sync_empty_remote_data
                SyncResult.NoInternet -> Res.string.sync_no_internet
                SyncResult.RequestTimeout -> Res.string.sync_request_timeout
                SyncResult.PermissionDenied -> Res.string.sync_items_permission_denied
                SyncResult.InvalidRemoteData -> Res.string.sync_invalid_remote_data
                SyncResult.DuplicatePromoCode -> Res.string.sync_discounts_duplicate_promo
                SyncResult.LocalStorageError -> Res.string.sync_local_storage_error
                is SyncResult.Success -> {
                    loadSyncDetails()
                    _state.update {
                        it.copy(
                            lastSyncedCount = result.syncedCount,
                            skippedCount = result.skippedInvalidCount,
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
            _state.update { it.copy(isSyncing = false) }
            emitMessage(message)
        }
    }

    private fun syncDiscounts() {
        if (_state.value.isSyncing) return

        _state.update { it.copy(isSyncing = true) }
        viewModelScope.launch {
            val message = when (val result = syncDiscountsUseCase()) {
                SyncResult.EmptyRemoteData -> Res.string.sync_empty_remote_data
                SyncResult.NoInternet -> Res.string.sync_no_internet
                SyncResult.RequestTimeout -> Res.string.sync_request_timeout
                SyncResult.PermissionDenied -> Res.string.sync_discounts_permission_denied
                SyncResult.InvalidRemoteData -> Res.string.sync_invalid_remote_data
                SyncResult.DuplicatePromoCode -> Res.string.sync_discounts_duplicate_promo
                SyncResult.LocalStorageError -> Res.string.sync_local_storage_error
                is SyncResult.Success -> {
                    loadSyncDetails()
                    _state.update {
                        it.copy(
                            lastSyncedCount = result.syncedCount,
                            skippedCount = result.skippedInvalidCount,
                        )
                    }
                    if (result.skippedInvalidCount > 0) {
                        Res.string.sync_discounts_success_with_warnings
                    } else {
                        Res.string.sync_discounts_success
                    }
                }

                SyncResult.UnknownError -> Res.string.sync_unknown_error
            }
            _state.update { it.copy(isSyncing = false) }
            emitMessage(message)
        }
    }

    private suspend fun emitMessage(message: StringResource) {
        _effect.emit(SyncEffect.ShowMessage(message))
    }
}