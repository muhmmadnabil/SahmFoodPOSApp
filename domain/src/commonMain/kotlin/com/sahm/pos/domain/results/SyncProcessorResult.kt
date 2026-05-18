package com.sahm.pos.domain.results

sealed interface SyncProcessorResult {
    data object Success : SyncProcessorResult
    data object NeedsRetry : SyncProcessorResult
    data class Failure(val message: String) : SyncProcessorResult
}