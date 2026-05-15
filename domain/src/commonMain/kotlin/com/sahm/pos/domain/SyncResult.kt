package com.sahm.pos.domain

sealed interface SyncResult {
    data class Success(
        val syncedCount: Int,
        val skippedInvalidCount: Int,
    ) : SyncResult

    data object EmptyRemoteData : SyncResult
    data object NoInternet : SyncResult
    data object PermissionDenied : SyncResult
    data object UnknownError : SyncResult
}