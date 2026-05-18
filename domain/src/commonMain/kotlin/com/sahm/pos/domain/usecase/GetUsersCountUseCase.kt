package com.sahm.pos.domain.usecase

import com.sahm.pos.domain.repository.SyncDataRepo

class GetUsersCountUseCase(private val syncDataRepo: SyncDataRepo) {
    suspend operator fun invoke(): Int = syncDataRepo.getUserCount().toInt()
}