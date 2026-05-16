package com.sahm.pos.domain.usecase

import com.sahm.pos.domain.results.SyncResult
import com.sahm.pos.domain.repository.SyncDataRepo

class SyncDiscountsUseCase(
    private val syncDataRepo: SyncDataRepo,
) {
    suspend operator fun invoke(): SyncResult =
        syncDataRepo.syncDiscounts()
}
