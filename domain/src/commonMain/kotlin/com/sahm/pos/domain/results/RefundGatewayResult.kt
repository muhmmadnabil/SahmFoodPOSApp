package com.sahm.pos.domain.results

sealed interface RefundGatewayResult {
    data class Success(
        val refundTransactionId: String,
        val gatewayReference: String,
    ) : RefundGatewayResult

    data class Failed(val reason: String) : RefundGatewayResult
}