package com.sahm.pos.domain.usecase

import com.sahm.pos.domain.entity.SyncAggregateStats
import com.sahm.pos.domain.repository.SyncDataRepo

class GetOrderSyncStatsUseCase(
    private val repo: SyncDataRepo,
) {
    suspend operator fun invoke(): SyncAggregateStats = repo.getOrderSyncStats()
}
