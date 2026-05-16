package com.sahm.pos.domain.usecase

import com.sahm.pos.domain.ClockProvider
import com.sahm.pos.domain.ReceiptPrinter
import com.sahm.pos.domain.UUIDProvider
import com.sahm.pos.domain.entity.OrderStatus
import com.sahm.pos.domain.entity.Payment
import com.sahm.pos.domain.entity.PaymentStatus
import com.sahm.pos.domain.entity.PaymentType
import com.sahm.pos.domain.entity.PrintStatus
import com.sahm.pos.domain.repository.OrderRepo
import com.sahm.pos.domain.results.PrintResult

class PayOrderByCashUseCase(
    private val repo: OrderRepo,
    private val clockProvider: ClockProvider,
    private val uuidProvider: UUIDProvider,
    private val receiptPrinter: ReceiptPrinter,
) {
    suspend operator fun invoke(orderId: String): Result<Unit> {
        val details = repo.getOrderDetails(orderId)
            ?: return Result.failure(IllegalArgumentException("Order not found"))
        if (details.order.orderStatus != OrderStatus.PendingPayment) {
            return Result.failure(IllegalStateException("Order is not pending payment"))
        }
        val now = clockProvider.nowMillis()
        repo.upsertPayment(
            Payment(
                id = uuidProvider.randomUuid(),
                orderId = orderId,
                type = PaymentType.CASH,
                status = PaymentStatus.Paid,
                amount = details.order.totalAmount,
                transactionId = null,
                gatewayReference = null,
                authorizationCode = null,
                cardBrand = null,
                cardLast4 = null,
                failureReason = null,
                createdAt = now,
                completedAt = now,
                syncedAt = null,
            )
        )
        repo.updateOrderAfterPayment(orderId, OrderStatus.Paid, PaymentStatus.Paid, now)
        printOrder(orderId)
        return Result.success(Unit)
    }

    private suspend fun printOrder(orderId: String) {
        repo.updateOrderPrintStatus(orderId, PrintStatus.Printing)
        when (receiptPrinter.printOrderReceipt(orderId)) {
            PrintResult.Success -> repo.updateOrderPrintStatus(orderId, PrintStatus.Printed)
            is PrintResult.Failed -> repo.updateOrderPrintStatus(orderId, PrintStatus.Failed)
        }
    }
}