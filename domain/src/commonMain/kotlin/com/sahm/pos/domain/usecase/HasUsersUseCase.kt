package com.sahm.pos.domain.usecase

import com.sahm.pos.domain.repository.SyncDataRepo

class HasUsersUseCase(
    private val syncRepo: SyncDataRepo,
) {
    suspend operator fun invoke(): Boolean = syncRepo.hasUsers()
}
