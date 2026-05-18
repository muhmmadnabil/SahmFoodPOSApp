package com.sahm.pos.domain.entity

data class OrderDetails(
    val order: Order,
    val items: List<OrderItem>,
    val payments: List<Payment>,
    val refunds: List<RefundDetails>,
)
