package com.sahm.pos.domain.usecase

import com.sahm.pos.domain.entity.OrderStatus
import com.sahm.pos.domain.entity.RefundStatus
import com.sahm.pos.domain.entity.RefundableOrderItem
import com.sahm.pos.domain.repository.OrderRepo

class GetRefundableItemsUseCase(
    private val repo: OrderRepo,
) {
    suspend operator fun invoke(orderId: String): Result<List<RefundableOrderItem>> {
        val details = repo.getOrderDetails(orderId)
            ?: return Result.failure(IllegalArgumentException("Order not found"))
        if (details.order.orderStatus !in setOf(OrderStatus.Paid, OrderStatus.PartiallyRefunded)) {
            return Result.failure(IllegalStateException("Order is not refundable"))
        }
        return Result.success(
            details.items.mapNotNull { item ->
                val refundedQuantity = details.refunds
                    .filter { it.refund.refundStatus == RefundStatus.Completed || it.refund.refundStatus == RefundStatus.Pending || it.refund.refundStatus == RefundStatus.Processing }
                    .flatMap { it.items }
                    .filter { it.orderItemId == item.id }
                    .sumOf { it.quantity }
                val refundableQuantity = item.quantity - refundedQuantity
                if (refundableQuantity <= 0) null else RefundableOrderItem(item, refundedQuantity, refundableQuantity)
            }
        )
    }
}