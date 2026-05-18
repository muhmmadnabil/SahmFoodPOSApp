package com.sahm.pos.domain.entity

data class CardRefundRequest(
    val orderId: String,
    val paymentId: String,
    val originalTransactionId: String,
    val refundAmount: Int,
)