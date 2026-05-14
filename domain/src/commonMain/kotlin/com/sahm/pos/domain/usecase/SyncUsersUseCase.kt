package com.sahm.pos.domain.usecase

import com.sahm.pos.domain.repository.SyncDataRepo

class SyncUsersUseCase(
    private val syncRepo: SyncDataRepo,
) {
    suspend operator fun invoke() = syncRepo.syncUsers()
}