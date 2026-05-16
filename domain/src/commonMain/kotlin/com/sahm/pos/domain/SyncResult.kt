package com.sahm.pos.domain

sealed interface SyncResult {
    data class Success(
        val syncedCount: Int,
        val skippedInvalidCount: Int,
    ) : SyncResult

    data object EmptyRemoteData : SyncResult
    data object NoInternet : SyncResult
    data object RequestTimeout : SyncResult
    data object PermissionDenied : SyncResult
    data object InvalidRemoteData : SyncResult
    data object DuplicatePromoCode : SyncResult
    data object LocalStorageError : SyncResult
    data object UnknownError : SyncResult
}
