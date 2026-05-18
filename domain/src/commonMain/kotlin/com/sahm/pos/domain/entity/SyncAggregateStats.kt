package com.sahm.pos.domain.entity

data class SyncAggregateStats(
    val totalCount: Long,
    val syncedCount: Long,
    val unsyncedCount: Long,
    val lastSyncAt: Long?,
) {
    companion object {
        val Empty = SyncAggregateStats(
            totalCount = 0,
            syncedCount = 0,
            unsyncedCount = 0,
            lastSyncAt = null,
        )
    }
}
