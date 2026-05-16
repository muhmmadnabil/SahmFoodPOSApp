package com.sahm.pos.domain.usecase

import com.sahm.pos.domain.repository.SyncDataRepo

class GetUsersLastSyncAtUseCase(private val repo: SyncDataRepo) {
    suspend operator fun invoke() = repo.getLastUsersSyncAt()
}