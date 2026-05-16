package com.sahm.pos.data.model

data class RemoteDiscountDocument(
    val id: String,
    val promo: String?,
    val percent: Double?,
    val minValue: Double?,
    val maxValue: Double?,
    val startAt: Long?,
    val endAt: Long?,
)
