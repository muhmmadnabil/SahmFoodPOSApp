package com.sahm.pos.domain.usecase

import com.sahm.pos.domain.repository.OrderRepo

class GetOrderDetailsUseCase(
    private val repo: OrderRepo,
) {
    suspend operator fun invoke(orderId: String) = repo.getOrderDetails(orderId)
}