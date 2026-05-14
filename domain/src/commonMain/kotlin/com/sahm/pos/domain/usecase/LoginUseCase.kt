package com.sahm.pos.domain.usecase

import com.sahm.pos.domain.LoginResult
import com.sahm.pos.domain.entity.CurrentUser
import com.sahm.pos.domain.repository.AuthRepo

class LoginUseCase(
    private val authRepo: AuthRepo,
    private val currentTimestampProvider: CurrentTimestampProvider = SystemCurrentTimestampProvider(),
) {
    suspend operator fun invoke(
        phone: String,
        password: String
    ): LoginResult {
        val normalizedPhone = phone.trim().filterNot { it.isWhitespace() }
        val normalizedPassword = password.trim()

        if (normalizedPhone.isBlank()) return LoginResult.EmptyPhone
        if (normalizedPassword.isBlank()) return LoginResult.EmptyPassword

        return try {
            val user = authRepo.getUserByPhone(normalizedPhone)
                ?: return LoginResult.InvalidCredentials

            if (user.password != normalizedPassword) {
                return LoginResult.InvalidCredentials
            }

            val lastLoginAt = currentTimestampProvider.now()
            authRepo.updateUserLastLoginAt(
                userId = user.id,
                timestamp = lastLoginAt,
            )
            authRepo.saveCurrentUser(
                CurrentUser(
                    id = user.id,
                    username = user.username,
                    phone = user.phone,
                )
            )

            LoginResult.Success
        } catch (throwable: Throwable) {
            LoginResult.Failure(throwable)
        }
    }
}
