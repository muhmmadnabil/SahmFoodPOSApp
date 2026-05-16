package com.sahm.pos.domain.usecase

import com.sahm.pos.domain.entity.Discount
import com.sahm.pos.domain.repository.SyncDataRepo

class ApplyDiscountUseCase(
    private val syncDataRepo: SyncDataRepo,
    private val appTimeProvider: AppTimeProvider,
) {
    suspend operator fun invoke(
        promoCode: String,
        orderTotal: Double,
    ): ApplyDiscountResult {
        val discount = syncDataRepo.getDiscountByPromoCode(promoCode.trim())
            ?: return ApplyDiscountResult.PromoCodeNotFound

        if (!discount.isStructurallyValid()) {
            return ApplyDiscountResult.InvalidDiscountConfiguration
        }

        val now = appTimeProvider.nowMillis()
        if (now < discount.startAt) return ApplyDiscountResult.PromoCodeNotStartedYet
        if (now > discount.endAt) return ApplyDiscountResult.PromoCodeExpired

        val amount = calculateDiscountAmount(orderTotal, discount)
        return ApplyDiscountResult.Success(
            discountAmount = amount,
            totalAfterDiscount = orderTotal - amount,
            discount = discount,
        )
    }

    private fun Discount.isStructurallyValid(): Boolean =
        id.isNotBlank() &&
            promoCode.trim().isNotBlank() &&
            percent > 0.0 &&
            percent <= 100.0 &&
            minValue >= 0.0 &&
            maxValue > 0.0 &&
            minValue <= maxValue &&
            startAt > 0 &&
            endAt > 0 &&
            startAt < endAt

    private fun calculateDiscountAmount(orderTotal: Double, discount: Discount): Double {
        val rawDiscount = orderTotal * discount.percent / 100.0
        val discountAfterMin = maxOf(rawDiscount, discount.minValue)
        return minOf(discountAfterMin, discount.maxValue, orderTotal)
    }
}

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
