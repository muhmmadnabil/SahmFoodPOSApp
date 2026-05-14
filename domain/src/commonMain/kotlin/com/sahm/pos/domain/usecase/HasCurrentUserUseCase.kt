package com.sahm.pos.domain.usecase

import com.sahm.pos.domain.repository.AuthRepo

class HasCurrentUserUseCase(
    private val authRepo: AuthRepo,
) {
    suspend operator fun invoke(): Boolean =
        try {
            authRepo.getCurrentUser() != null
        } catch (_: Throwable) {
            false
        }
}
