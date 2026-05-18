package com.sahm.pos.domain.usecase

import com.sahm.pos.domain.ReceiptPrinter
import com.sahm.pos.domain.repository.OrderRepo

class RetryPrintRefundReceiptUseCase(
    private val repo: OrderRepo,
    private val receiptPrinter: ReceiptPrinter,
) {
    suspend operator fun invoke(refundId: String): Result<Unit> {
        repo.getRefundDetails(refundId)
            ?: return Result.failure(IllegalArgumentException("Refund not found"))
        receiptPrinter.printRefundReceipt(refundId)
        return Result.success(Unit)
    }
}
