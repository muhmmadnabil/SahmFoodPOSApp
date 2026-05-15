package com.sahm.pos.screens.syncDetails

import org.jetbrains.compose.resources.StringResource

data class SyncUiState(
    val isSyncingItems: Boolean = false,
    val isSyncingUsers: Boolean = false,
    val localItemCount: Int = 0,
    val localUserCount: Int = 0,
    val lastItemSyncAt: Long? = null,
    val lastUserSyncAt: Long? = null,
    val lastItemsSyncedCount: Int = 0,
    val lastUsersSyncedCount: Int = 0,
    val skippedInvalidItemsCount: Int = 0,
    val skippedInvalidUsersCount: Int = 0,
)

sealed interface SyncIntent {
    data object ScreenOpened : SyncIntent
    data object SyncItemsClicked : SyncIntent
    data object SyncUsersClicked : SyncIntent
}

sealed interface SyncEffect {
    data class ShowMessage(val message: StringResource) : SyncEffect
}
