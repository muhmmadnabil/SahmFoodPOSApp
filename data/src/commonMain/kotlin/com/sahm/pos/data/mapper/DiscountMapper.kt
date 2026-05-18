package com.sahm.pos.data.mapper

import com.sahm.pos.data.model.RemoteDiscountDocument
import com.sahm.pos.domain.entity.Discount

fun RemoteDiscountDocument.toDiscountOrNull(syncAt: Long): Discount? {
    val trimmedPromo = promo?.trim() ?: return null
    val discount = Discount(
        id = id,
        promoCode = trimmedPromo,
        percent = percent ?: return null,
        minValue = minValue ?: return null,
        maxValue = maxValue ?: return null,
        startAt = startAt ?: return null,
        endAt = endAt ?: return null,
        syncAt = syncAt,
    )

    return discount.takeIf { it.isStructurallyValid() }
}

fun Discount.isStructurallyValid(): Boolean =
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
