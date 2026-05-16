package com.sahm.pos.domain.entity

data class CreateRefundRequest(
    val orderId: String,
    val selections: List<RefundSelection>,
    val type: PaymentType,
    val reason: String? = null,
)