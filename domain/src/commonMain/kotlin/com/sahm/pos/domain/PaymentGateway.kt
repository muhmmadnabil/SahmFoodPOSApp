package com.sahm.pos.domain

import com.sahm.pos.domain.entity.CardPaymentRequest
import com.sahm.pos.domain.entity.CardRefundRequest
import com.sahm.pos.domain.results.PaymentGatewayResult
import com.sahm.pos.domain.results.RefundGatewayResult

interface PaymentGateway {
    suspend fun pay(request: CardPaymentRequest): PaymentGatewayResult
    suspend fun refund(request: CardRefundRequest): RefundGatewayResult
}