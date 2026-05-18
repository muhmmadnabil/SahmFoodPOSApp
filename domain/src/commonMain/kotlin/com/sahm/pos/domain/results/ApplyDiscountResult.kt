package com.sahm.pos.domain.results

import com.sahm.pos.domain.entity.Discount

sealed interface ApplyDiscountResult {
    data class Success(
        val discountAmount: Double,
        val totalAfterDiscount: Double,
        val discount: Discount,
    ) : ApplyDiscountResult

    data object PromoCodeNotFound : ApplyDiscountResult
    data object PromoCodeExpired : ApplyDiscountResult
    data object PromoCodeNotStartedYet : ApplyDiscountResult
    data object InvalidDiscountConfiguration : ApplyDiscountResult
}