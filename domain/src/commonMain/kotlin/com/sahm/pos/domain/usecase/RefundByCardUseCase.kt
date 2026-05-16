package com.sahm.pos.domain.usecase

import com.sahm.pos.domain.ClockProvider
import com.sahm.pos.domain.PaymentGateway
import com.sahm.pos.domain.ReceiptPrinter
import com.sahm.pos.domain.entity.CardRefundRequest
import com.sahm.pos.domain.entity.OrderStatus
import com.sahm.pos.domain.entity.PaymentStatus
import com.sahm.pos.domain.entity.PaymentType
import com.sahm.pos.domain.entity.RefundStatus
import com.sahm.pos.domain.repository.OrderRepo
import com.sahm.pos.domain.results.RefundGatewayResult

class RefundByCardUseCase(
    private val repo: OrderRepo,
    private val clockProvider: ClockProvider,
    private val paymentGateway: PaymentGateway,
    private val receiptPrinter: ReceiptPrinter,
) {
    suspend operator fun invoke(refundId: String): Result<Unit> {
        val details = repo.getRefundDetails(refundId)
            ?: return Result.failure(IllegalArgumentException("Refund not found"))
        val payment = repo.getCompletedPaymentForOrder(details.refund.orderId)
            ?: return Result.failure(IllegalStateException("Original card payment not found"))
        if (payment.type != PaymentType.CARD || payment.transactionId == null) {
            return Result.failure(IllegalStateException("Original card transaction not found"))
        }
        repo.updateRefundStatus(refundId, RefundStatus.Processing, null)
        return when (val result = paymentGateway.refund(
            CardRefundRequest(
                details.refund.orderId,
                payment.id,
                payment.transactionId,
                details.refund.amount.toInt()
            )
        )) {
            is RefundGatewayResult.Success -> {
                repo.updateRefundStatus(refundId, RefundStatus.Completed, clockProvider.nowMillis())
                updateAggregateStatus(details.refund.orderId)
                receiptPrinter.printRefundReceipt(refundId)
                Result.success(Unit)
            }

            is RefundGatewayResult.Failed -> {
                repo.updateRefundStatus(refundId, RefundStatus.Failed, clockProvider.nowMillis())
                Result.failure(IllegalStateException(result.reason))
            }
        }
    }

    private suspend fun updateAggregateStatus(orderId: String) {
        val order = repo.getOrderDetails(orderId) ?: return
        val paidAmount = order.payments.firstOrNull { it.status == PaymentStatus.Paid }?.amount
            ?: order.order.totalAmount
        val refunded = order.refunds
            .filter { it.refund.refundStatus == RefundStatus.Completed }
            .sumOf { it.refund.amount }
        val status =
            if (refunded >= paidAmount) OrderStatus.Refunded else OrderStatus.PartiallyRefunded
        val paymentStatus =
            if (refunded >= paidAmount) PaymentStatus.Refunded else PaymentStatus.PartiallyRefunded
        repo.updateOrderRefundStatus(orderId, status, paymentStatus)
    }
}