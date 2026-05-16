package com.sahm.pos.screens.syncDetails

import com.sahm.pos.screens.sync.SyncDetailType
import org.jetbrains.compose.resources.StringResource

data class SyncUiState(
    val selectedType: SyncDetailType? = null,
    val isSyncing: Boolean = false,
    val lastSyncAt: Long? = null,
    val count: Int = 0,
    val skippedCount: Int = 0,
    val lastSyncedCount: Int = 0,
)

sealed interface SyncIntent {
    data class ScreenOpened(val type: SyncDetailType) : SyncIntent
    data object SyncItemsClicked : SyncIntent
    data object SyncUsersClicked : SyncIntent
    data object SyncDiscountsClicked : SyncIntent
}

sealed interface SyncEffect {
    data class ShowMessage(val message: StringResource) : SyncEffect
}