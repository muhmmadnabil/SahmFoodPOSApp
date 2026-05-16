package com.sahm.pos.domain.usecase

import com.sahm.pos.domain.ClockProvider
import com.sahm.pos.domain.ReceiptPrinter
import com.sahm.pos.domain.entity.OrderStatus
import com.sahm.pos.domain.entity.PaymentStatus
import com.sahm.pos.domain.entity.PaymentType
import com.sahm.pos.domain.entity.RefundStatus
import com.sahm.pos.domain.repository.OrderRepo

class RefundByCashUseCase(
    private val repo: OrderRepo,
    private val clockProvider: ClockProvider,
    private val receiptPrinter: ReceiptPrinter,
) {
    suspend operator fun invoke(refundId: String): Result<Unit> {
        val details = repo.getRefundDetails(refundId)
            ?: return Result.failure(IllegalArgumentException("Refund not found"))
        if (details.refund.refundType != PaymentType.CASH) {
            return Result.failure(IllegalStateException("Refund is not cash"))
        }
        repo.updateRefundStatus(refundId, RefundStatus.Completed, clockProvider.nowMillis())
        updateAggregateStatus(details.refund.orderId)
        receiptPrinter.printRefundReceipt(refundId)
        return Result.success(Unit)
    }

    private suspend fun updateAggregateStatus(orderId: String) {
        val order = repo.getOrderDetails(orderId) ?: return
        val paidAmount = order.payments.firstOrNull { it.status == PaymentStatus.Paid }?.amount ?: order.order.totalAmount
        val refunded = order.refunds
            .filter { it.refund.refundStatus == RefundStatus.Completed || it.refund.id == order.refunds.lastOrNull()?.refund?.id }
            .sumOf { it.refund.amount }
        val status = if (refunded >= paidAmount) OrderStatus.Refunded else OrderStatus.PartiallyRefunded
        val paymentStatus = if (refunded >= paidAmount) PaymentStatus.Refunded else PaymentStatus.PartiallyRefunded
        repo.updateOrderRefundStatus(orderId, status, paymentStatus)
    }
}