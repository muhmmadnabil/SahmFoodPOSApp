package com.sahm.pos.domain.entity

data class Refund(
    val id: String,
    val orderId: String,
    val paymentId: String?,
    val refundStatus: RefundStatus,
    val refundType: PaymentType,
    val amount: Long,
    val reason: String?,
    val createdAt: Long,
    val completedAt: Long?,
    val syncedAt: Long?,
)
