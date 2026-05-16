package com.sahm.pos.domain.usecase

import com.sahm.pos.domain.repository.SyncDataRepo

class GetDiscountsLastSyncAtUseCase(private val repo: SyncDataRepo) {
    suspend operator fun invoke() = repo.getLastDiscountsSyncAt()
}