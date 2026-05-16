package com.sahm.pos.domain.usecase

import com.sahm.pos.domain.ClockProvider
import com.sahm.pos.domain.UUIDProvider
import com.sahm.pos.domain.entity.CreateRefundRequest
import com.sahm.pos.domain.entity.OrderItem
import com.sahm.pos.domain.entity.OrderStatus
import com.sahm.pos.domain.entity.Refund
import com.sahm.pos.domain.entity.RefundItem
import com.sahm.pos.domain.entity.RefundStatus
import com.sahm.pos.domain.repository.OrderRepo
import com.sahm.pos.domain.results.CreateRefundResult
import kotlin.math.floor

class CreateRefundUseCase(
    private val repo: OrderRepo,
    private val clockProvider: ClockProvider,
    private val uuidProvider: UUIDProvider,
) {
    suspend operator fun invoke(request: CreateRefundRequest): CreateRefundResult {
        if (request.selections.isEmpty()) return CreateRefundResult.EmptySelection
        if (request.selections.any { it.quantity <= 0 }) return CreateRefundResult.InvalidQuantity

        val details = repo.getOrderDetails(request.orderId)
            ?: return CreateRefundResult.OrderNotFound
        if (details.order.orderStatus !in setOf(OrderStatus.Paid, OrderStatus.PartiallyRefunded)) {
            return CreateRefundResult.OrderNotRefundable
        }

        val refundableItems = GetRefundableItemsUseCase(repo)(request.orderId)
            .getOrElse { return CreateRefundResult.OrderNotRefundable }
            .associateBy { it.orderItem.id }
        val refundItems = request.selections.map { selection ->
            val refundable = refundableItems[selection.orderItemId]
                ?: return CreateRefundResult.ItemNotInOrder
            if (selection.quantity > refundable.refundableQuantity) {
                return CreateRefundResult.QuantityExceedsRefundable
            }
            refundable.orderItem.toRefundItem(
                refundId = "pending",
                refundItemId = uuidProvider.randomUuid(),
                quantity = selection.quantity,
                previouslyRefundedQuantity = refundable.refundedQuantity,
            )
        }
        val refundId = uuidProvider.randomUuid()
        val materializedItems = refundItems.map { it.copy(refundId = refundId) }
        val refund = Refund(
            id = refundId,
            orderId = request.orderId,
            paymentId = repo.getCompletedPaymentForOrder(request.orderId)?.id,
            refundStatus = RefundStatus.Pending,
            refundType = request.type,
            amount = materializedItems.sumOf { it.totalRefundAmount },
            reason = request.reason,
            createdAt = clockProvider.nowMillis(),
            completedAt = null,
            syncedAt = null,
        )

        return runCatching {
            repo.createRefund(refund, materializedItems)
            CreateRefundResult.Success(refundId)
        }.getOrElse { CreateRefundResult.Failed(it.message ?: "Could not create refund.") }
    }

    private fun OrderItem.toRefundItem(
        refundId: String,
        refundItemId: String,
        quantity: Int,
        previouslyRefundedQuantity: Int,
    ): RefundItem {
        val subtotal = prorateAmount(subtotalAmount, this.quantity, quantity, previouslyRefundedQuantity)
        val discount = prorateAmount(discountAmount, this.quantity, quantity, previouslyRefundedQuantity)
        val tax = prorateAmount(taxAmount, this.quantity, quantity, previouslyRefundedQuantity)
        val total = prorateAmount(totalAmount, this.quantity, quantity, previouslyRefundedQuantity)
        return RefundItem(
            id = refundItemId,
            refundId = refundId,
            orderItemId = id,
            menuItemId = menuItemId,
            itemName = name,
            quantity = quantity,
            unitPrice = unitPrice,
            subtotalAmount = subtotal,
            discountAmount = discount,
            taxAmount = tax,
            totalRefundAmount = total,
        )
    }

    private fun prorateAmount(amount: Long, originalQuantity: Int, refundQuantity: Int, previouslyRefundedQuantity: Int): Long {
        val previousAmount = floor(amount.toDouble() * previouslyRefundedQuantity / originalQuantity).toLong()
        val newCumulativeAmount = floor(amount.toDouble() * (previouslyRefundedQuantity + refundQuantity) / originalQuantity).toLong()
        return (newCumulativeAmount - previousAmount).coerceAtMost(amount - previousAmount)
    }
}