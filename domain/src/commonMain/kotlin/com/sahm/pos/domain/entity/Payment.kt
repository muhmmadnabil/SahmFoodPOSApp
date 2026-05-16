package com.sahm.pos.domain.entity

data class Payment(
    val id: String,
    val orderId: String,
    val type: PaymentType,
    val status: PaymentStatus,
    val amount: Long,
    val transactionId: String?,
    val gatewayReference: String?,
    val authorizationCode: String?,
    val cardBrand: String?,
    val cardLast4: String?,
    val failureReason: String?,
    val createdAt: Long,
    val completedAt: Long?,
    val syncedAt: Long?,
)
