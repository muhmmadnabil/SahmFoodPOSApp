package com.sahm.pos.domain.entity

data class RefundableOrderItem(
    val orderItem: OrderItem,
    val refundedQuantity: Int,
    val refundableQuantity: Int,
)