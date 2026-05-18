package com.sahm.pos.domain.usecase

import com.sahm.pos.domain.repository.SyncDataRepo

class GetMenuItemsCountUseCase(private val syncDataRepo: SyncDataRepo) {
    suspend operator fun invoke() = syncDataRepo.getMenuItemCount().toInt()
}