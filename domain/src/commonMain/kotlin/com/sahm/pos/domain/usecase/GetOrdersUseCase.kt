package com.sahm.pos.domain.usecase

import com.sahm.pos.domain.entity.Order
import com.sahm.pos.domain.repository.OrderRepo

class GetOrdersUseCase(
    private val orderRepo: OrderRepo,
) {
    suspend operator fun invoke(): List<Order> =
        orderRepo.getOrders()
}
