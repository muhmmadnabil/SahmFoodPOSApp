package com.sahm.pos.domain.results

sealed interface LoginResult {
    data object EmptyPhone : LoginResult
    data object EmptyPassword : LoginResult
    data object InvalidCredentials : LoginResult
    data object Success : LoginResult
    data class Failure(val throwable: Throwable) : LoginResult
}