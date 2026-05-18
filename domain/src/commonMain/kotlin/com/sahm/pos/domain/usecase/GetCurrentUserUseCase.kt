package com.sahm.pos.domain.usecase

import com.sahm.pos.domain.entity.CurrentUser
import com.sahm.pos.domain.repository.AuthRepo

class GetCurrentUserUseCase(
    private val authRepo: AuthRepo,
) {
    suspend operator fun invoke(): CurrentUser? =
        authRepo.getCurrentUser()
}
