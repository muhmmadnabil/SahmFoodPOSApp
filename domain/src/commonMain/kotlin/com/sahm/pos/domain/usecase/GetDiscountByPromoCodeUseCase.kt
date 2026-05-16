package com.sahm.pos.domain.usecase

import com.sahm.pos.domain.repository.SyncDataRepo

class GetDiscountByPromoCodeUseCase(private val syncRepo: SyncDataRepo) {
    suspend operator fun invoke(promoCode: String) =
        syncRepo.getDiscountByPromoCode(promoCode = promoCode)
}