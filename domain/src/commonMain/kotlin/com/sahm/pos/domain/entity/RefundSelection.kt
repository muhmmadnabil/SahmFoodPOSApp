package com.sahm.pos.domain.entity

data class RefundSelection(
    val orderItemId: String,
    val quantity: Int,
)