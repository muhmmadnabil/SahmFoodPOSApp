package com.sahm.pos.domain.usecase

import com.sahm.pos.domain.repository.AuthRepo

class LogoutUseCase(
    private val authRepo: AuthRepo,
) {
    suspend operator fun invoke() {
        authRepo.clearCurrentUser()
    }
}
