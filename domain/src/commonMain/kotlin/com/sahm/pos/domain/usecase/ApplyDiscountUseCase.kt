package com.sahm.pos.domain.usecase

import com.sahm.pos.domain.entity.Discount
import com.sahm.pos.domain.repository.SyncDataRepo
import com.sahm.pos.domain.results.ApplyDiscountResult

class ApplyDiscountUseCase(
    private val syncDataRepo: SyncDataRepo,
    private val getAppTimeUseCase: GetAppTimeUseCase,
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

        val now = getAppTimeUseCase.nowMillis()
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