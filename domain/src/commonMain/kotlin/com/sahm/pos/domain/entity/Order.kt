package com.sahm.pos.domain.entity

data class Order(
    val id: String,
    val cashierId: String,
    val cashierName: String,
    val subtotalAmount: Long,
    val taxAmount: Long,
    val discountAmount: Long,
    val totalAmount: Long,
    val serviceAmount: Long,
    val discountId: String?,
    val discountPromoCode: String?,
    val discountPercent: Double?,
    val discountMinValue: Double?,
    val discountMaxValue: Double?,
    val orderStatus: OrderStatus,
    val paymentStatus: PaymentStatus,
    val printStatus: PrintStatus,
    val createdAt: Long,
    val paidAt: Long?,
    val cancelledAt: Long?,
    val syncedAt: Long?,
)