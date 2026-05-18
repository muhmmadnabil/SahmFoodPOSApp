package com.sahm.pos.domain.usecase

import com.sahm.pos.domain.ReceiptPrinter
import com.sahm.pos.domain.entity.OrderStatus
import com.sahm.pos.domain.entity.PrintStatus
import com.sahm.pos.domain.repository.OrderRepo
import com.sahm.pos.domain.results.PrintResult
import com.sahm.pos.domain.sync.SyncReason
import com.sahm.pos.domain.sync.SyncScheduler

class RetryPrintOrderReceiptUseCase(
    private val repo: OrderRepo,
    private val receiptPrinter: ReceiptPrinter,
    private val syncScheduler: SyncScheduler? = null,
) {
    suspend operator fun invoke(orderId: String): Result<Unit> {
        val details = repo.getOrderDetails(orderId)
            ?: return Result.failure(IllegalArgumentException("Order not found"))
        if (details.order.orderStatus !in setOf(OrderStatus.Paid, OrderStatus.PartiallyRefunded, OrderStatus.Refunded)) {
            return Result.failure(IllegalStateException("Order is not paid"))
        }
        repo.updateOrderPrintStatus(orderId, PrintStatus.Printing)
        when (receiptPrinter.printOrderReceipt(orderId)) {
            PrintResult.Success -> repo.updateOrderPrintStatus(orderId, PrintStatus.Printed)
            is PrintResult.Failed -> repo.updateOrderPrintStatus(orderId, PrintStatus.Failed)
        }
        syncScheduler?.scheduleSync(SyncReason.PaymentCreated)
        return Result.success(Unit)
    }
}
