package com.sahm.pos.domain.usecase

import com.sahm.pos.domain.repository.AuthRepo

class HasUsersUseCase(
    private val authRepo: AuthRepo,
) {
    suspend operator fun invoke(): Boolean = authRepo.hasUsers()
}
