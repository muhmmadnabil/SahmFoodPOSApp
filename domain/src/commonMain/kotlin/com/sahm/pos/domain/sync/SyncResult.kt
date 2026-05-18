package com.sahm.pos.domain.sync

sealed interface SyncResult {
    data object Success : SyncResult
    data object NothingToSync : SyncResult
    data class TransientFailure(val throwable: Throwable? = null) : SyncResult
    data class PermanentFailure(val reason: String? = null) : SyncResult
}
