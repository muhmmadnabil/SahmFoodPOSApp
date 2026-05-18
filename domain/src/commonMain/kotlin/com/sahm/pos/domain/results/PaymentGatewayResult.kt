package com.sahm.pos.domain.results

sealed interface PaymentGatewayResult {
    data class Success(
        val transactionId: String,
        val gatewayReference: String,
        val authorizationCode: String,
        val cardBrand: String,
        val cardLast4: String,
    ) : PaymentGatewayResult

    data class Failed(val reason: String) : PaymentGatewayResult
}