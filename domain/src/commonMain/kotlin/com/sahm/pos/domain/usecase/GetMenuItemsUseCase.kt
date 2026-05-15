package com.sahm.pos.domain.usecase

import com.sahm.pos.domain.entity.MenuItem
import com.sahm.pos.domain.repository.SyncDataRepo

class GetMenuItemsUseCase(
    private val syncRepo: SyncDataRepo,
) {
    suspend operator fun invoke(): List<MenuItem> =
        syncRepo.getActiveMenuItems()
}
