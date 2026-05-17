package com.sahm.pos.domain.usecase

import com.sahm.pos.domain.ClockProvider
import com.sahm.pos.domain.PaymentGateway
import com.sahm.pos.domain.ReceiptPrinter
import com.sahm.pos.domain.UUIDProvider
import com.sahm.pos.domain.entity.CardPaymentRequest
import com.sahm.pos.domain.entity.OrderStatus
import com.sahm.pos.domain.entity.Payment
import com.sahm.pos.domain.entity.PaymentStatus
import com.sahm.pos.domain.entity.PaymentType
import com.sahm.pos.domain.entity.PrintStatus
import com.sahm.pos.domain.repository.OrderRepo
import com.sahm.pos.domain.results.PaymentGatewayResult
import com.sahm.pos.domain.results.PrintResult
import com.sahm.pos.domain.sync.SyncScheduler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PayOrderByCardUseCase(
    private val repo: OrderRepo,
    private val clockProvider: ClockProvider,
    private val uuidProvider: UUIDProvider,
    private val paymentGateway: PaymentGateway,
    private val receiptPrinter: ReceiptPrinter,
    private val syncScheduler: SyncScheduler? = null,
    private val printScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    suspend operator fun invoke(request: CardPaymentRequest): Result<Unit> {
        val details = repo.getOrderDetails(request.orderId)
            ?: return Result.failure(IllegalArgumentException("Order not found"))
        if (details.order.orderStatus != OrderStatus.PendingPayment) {
            return Result.failure(IllegalStateException("Order is not pending payment"))
        }
        validateCard(request)?.let { return Result.failure(IllegalArgumentException(it)) }

        val existingFailedCardPayment = details.payments.lastOrNull {
            it.type == PaymentType.CARD && it.status in setOf(
                PaymentStatus.Failed,
                PaymentStatus.Processing,
            )
        }
        val now = clockProvider.nowMillis()
        val paymentId = existingFailedCardPayment?.id ?: uuidProvider.randomUuid()
        repo.upsertPayment(
            Payment(
                id = paymentId,
                orderId = request.orderId,
                type = PaymentType.CARD,
                status = PaymentStatus.Processing,
                amount = details.order.totalAmount,
                transactionId = null,
                gatewayReference = null,
                authorizationCode = null,
                cardBrand = null,
                cardLast4 = null,
                failureReason = null,
                createdAt = existingFailedCardPayment?.createdAt ?: now,
                completedAt = null,
                syncedAt = null,
            )
        )
        repo.updateOrderPaymentStatus(request.orderId, PaymentStatus.Processing)

        return when (val gatewayResult =
            paymentGateway.pay(request.copy(amount = details.order.totalAmount.toInt()))) {
            is PaymentGatewayResult.Success -> {
                val completedAt = clockProvider.nowMillis()
                repo.upsertPayment(
                    Payment(
                        id = paymentId,
                        orderId = request.orderId,
                        type = PaymentType.CARD,
                        status = PaymentStatus.Paid,
                        amount = details.order.totalAmount,
                        transactionId = gatewayResult.transactionId,
                        gatewayReference = gatewayResult.gatewayReference,
                        authorizationCode = gatewayResult.authorizationCode,
                        cardBrand = gatewayResult.cardBrand,
                        cardLast4 = gatewayResult.cardLast4,
                        failureReason = null,
                        createdAt = existingFailedCardPayment?.createdAt ?: now,
                        completedAt = completedAt,
                        syncedAt = null,
                    )
                )
                repo.updateOrderAfterPayment(
                    request.orderId,
                    OrderStatus.Paid,
                    PaymentStatus.Paid,
                    completedAt,
                )
                printOrderInBackground(request.orderId)
                runCatching { syncScheduler?.scheduleSync() }
                Result.success(Unit)
            }

            is PaymentGatewayResult.Failed -> {
                repo.upsertPayment(
                    Payment(
                        id = paymentId,
                        orderId = request.orderId,
                        type = PaymentType.CARD,
                        status = PaymentStatus.Failed,
                        amount = details.order.totalAmount,
                        transactionId = null,
                        gatewayReference = null,
                        authorizationCode = null,
                        cardBrand = null,
                        cardLast4 = request.cardNumber
                            .replace(" ", "")
                            .takeLast(4)
                            .takeIf { it.length == 4 },
                        failureReason = gatewayResult.reason,
                        createdAt = existingFailedCardPayment?.createdAt ?: now,
                        completedAt = clockProvider.nowMillis(),
                        syncedAt = null,
                    )
                )
                repo.updateOrderPaymentStatus(request.orderId, PaymentStatus.Failed)
                Result.failure(IllegalStateException(gatewayResult.reason))
            }
        }
    }

    private fun validateCard(request: CardPaymentRequest): String? {
        val number = request.cardNumber.replace(" ", "")
        if (number.isBlank() || number.any { !it.isDigit() }) return "Invalid card number"
        if (request.cvv.length !in 3..4 || request.cvv.any { !it.isDigit() }) return "Invalid CVV"
        val month = request.expiryMonth.toIntOrNull() ?: return "Invalid expiry"
        if (month !in 1..12) return "Invalid expiry"
        val year = request.expiryYear.toIntOrNull() ?: return "Invalid expiry"
        if (year < 2026) return "Invalid expiry"
        if (request.cardHolderName.isBlank()) return "Card holder name is required"
        return null
    }

    private fun printOrderInBackground(orderId: String) {
        printScope.launch {
            try {
                printOrder(orderId)
            } finally {
                runCatching { syncScheduler?.scheduleSync() }
            }
        }
    }

    private suspend fun printOrder(orderId: String) {
        try {
            repo.updateOrderPrintStatus(orderId, PrintStatus.Printing)
            when (receiptPrinter.printOrderReceipt(orderId)) {
                PrintResult.Success -> repo.updateOrderPrintStatus(orderId, PrintStatus.Printed)
                is PrintResult.Failed -> repo.updateOrderPrintStatus(orderId, PrintStatus.Failed)
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            runCatching { repo.updateOrderPrintStatus(orderId, PrintStatus.Failed) }
        }
    }
}
