package com.sahm.pos.domain.entity

data class RefundItem(
    val id: String,
    val refundId: String,
    val orderItemId: String,
    val menuItemId: String,
    val itemName: String,
    val quantity: Int,
    val unitPrice: Long,
    val subtotalAmount: Long,
    val discountAmount: Long,
    val taxAmount: Long,
    val totalRefundAmount: Long,
)