package com.sahm.pos.domain.entity

data class RefundDetails(
    val refund: Refund,
    val items: List<RefundItem>,
)