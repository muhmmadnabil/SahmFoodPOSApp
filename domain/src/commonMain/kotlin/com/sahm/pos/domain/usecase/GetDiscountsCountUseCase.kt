package com.sahm.pos.domain.usecase

import com.sahm.pos.domain.repository.SyncDataRepo

class GetDiscountsCountUseCase(private val repo: SyncDataRepo) {
    suspend operator fun invoke() = repo.getDiscountsCount()
}