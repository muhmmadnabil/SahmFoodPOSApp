package com.sahm.pos.domain.entity

data class CardPaymentRequest(
    val orderId: String,
    val amount: Int,
    val cardNumber: String,
    val expiryMonth: String,
    val expiryYear: String,
    val cvv: String,
    val cardHolderName: String,
)