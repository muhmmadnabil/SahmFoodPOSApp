package com.sahm.pos.domain

import com.sahm.pos.domain.results.PrintResult

class FakeReceiptPrinter(
    private val orderResult: PrintResult = PrintResult.Success,
    private val refundResult: PrintResult = PrintResult.Success,
) : ReceiptPrinter {
    override suspend fun printOrderReceipt(orderId: String): PrintResult = orderResult
    override suspend fun printRefundReceipt(refundId: String): PrintResult = refundResult
}