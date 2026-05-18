package com.sahm.pos.domain

import com.sahm.pos.domain.results.PrintResult

interface ReceiptPrinter {
    suspend fun printOrderReceipt(orderId: String): PrintResult
    suspend fun printRefundReceipt(refundId: String): PrintResult
}