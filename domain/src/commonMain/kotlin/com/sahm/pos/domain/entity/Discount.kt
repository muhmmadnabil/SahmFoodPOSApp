package com.sahm.pos.domain.entity

data class Discount(
    val id: String,
    val promoCode: String,
    val percent: Double,
    val minValue: Double,
    val maxValue: Double,
    val startAt: Long,
    val endAt: Long,
    val syncAt: Long,
)
