package com.sahm.pos.domain.results

sealed interface PrintResult {
    data object Success : PrintResult
    data class Failed(val reason: String) : PrintResult
}