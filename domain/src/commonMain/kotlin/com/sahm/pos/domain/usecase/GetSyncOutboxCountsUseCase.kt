package com.sahm.pos.domain.usecase

import com.sahm.pos.domain.entity.SyncOutboxCounts
import com.sahm.pos.domain.repository.SyncDataRepo

class GetSyncOutboxCountsUseCase(
    private val repo: SyncDataRepo,
) {
    suspend operator fun invoke(): SyncOutboxCounts =
        SyncOutboxCounts(
            pending = repo.getCountSyncItemsPending(),
            failed = repo.getCountSyncItemsFailed(),
            conflicts = repo.getCountSyncItemsConflicts(),
        )
}