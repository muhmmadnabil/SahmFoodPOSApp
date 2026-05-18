package com.sahm.pos.domain.entity

data class OrderItem(
    val id: String,
    val orderId: String,
    val menuItemId: String,
    val category: String,
    val name: String,
    val description: String,
    val imageUrl: String,
    val localImageUrl: String?,
    val quantity: Int,
    val unitPrice: Long,
    val subtotalAmount: Long,
    val discountAmount: Long,
    val taxAmount: Long,
    val totalAmount: Long,
)