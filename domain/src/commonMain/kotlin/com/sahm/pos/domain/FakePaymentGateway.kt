package com.sahm.pos.domain

import com.sahm.pos.domain.entity.CardPaymentRequest
import com.sahm.pos.domain.entity.CardRefundRequest
import com.sahm.pos.domain.results.PaymentGatewayResult
import com.sahm.pos.domain.results.RefundGatewayResult

class FakePaymentGateway : PaymentGateway {
    override suspend fun pay(request: CardPaymentRequest): PaymentGatewayResult {
        return when (val normalizedNumber = request.cardNumber.replace(" ", "")) {
            "4242424242424242" -> PaymentGatewayResult.Success(
                transactionId = "txn_${request.orderId}",
                gatewayReference = "gw_${request.orderId}",
                authorizationCode = "auth_${request.orderId}",
                cardBrand = "Visa",
                cardLast4 = normalizedNumber.takeLast(4),
            )
            "4000000000000002" -> PaymentGatewayResult.Failed("Card declined")
            "4000000000009995" -> PaymentGatewayResult.Failed("Insufficient funds")
            else -> PaymentGatewayResult.Failed("Unsupported test card")
        }
    }

    override suspend fun refund(request: CardRefundRequest): RefundGatewayResult =
        RefundGatewayResult.Success(
            refundTransactionId = "refund_${request.paymentId}",
            gatewayReference = "refund_gw_${request.paymentId}",
        )
}