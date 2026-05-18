package com.sahm.pos.domain.results

sealed interface SyncUploadResult {
    data object Success : SyncUploadResult
    data object DuplicateIdempotencyKey : SyncUploadResult
    data class RetryableError(val code: String, val message: String) : SyncUploadResult
    data class NonRetryableError(val code: String, val message: String) : SyncUploadResult
    data class Conflict(val code: String, val message: String) : SyncUploadResult
}