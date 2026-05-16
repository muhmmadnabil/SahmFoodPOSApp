package com.sahm.pos.domain.usecase

import com.sahm.pos.domain.ClockProvider
import com.sahm.pos.domain.UUIDProvider
import com.sahm.pos.domain.entity.CreateOrderRequest
import com.sahm.pos.domain.entity.Discount
import com.sahm.pos.domain.entity.Order
import com.sahm.pos.domain.entity.OrderItem
import com.sahm.pos.domain.entity.OrderStatus
import com.sahm.pos.domain.entity.OrderType
import com.sahm.pos.domain.entity.PaymentStatus
import com.sahm.pos.domain.entity.PrintStatus
import com.sahm.pos.domain.repository.AuthRepo
import com.sahm.pos.domain.repository.OrderRepo
import com.sahm.pos.domain.results.CreateOrderResult
import kotlin.math.roundToLong

class CreateOrderUseCase(
    private val orderRepo: OrderRepo,
    private val authRepo: AuthRepo,
    private val uuidProvider: UUIDProvider,
    private val clockProvider: ClockProvider,
) {
    suspend operator fun invoke(request: CreateOrderRequest): CreateOrderResult {
        if (request.items.isEmpty()) return CreateOrderResult.EmptyCart
        if (request.items.any { it.quantity <= 0 }) return CreateOrderResult.InvalidQuantity

        val cashier = authRepo.getCurrentUser() ?: return CreateOrderResult.CashierMissing
        for (item in request.items) {
            val localItem = orderRepo.getMenuItemById(item.menuItem.id)
                ?: return CreateOrderResult.MenuItemNotFound
            if (!localItem.isActive) return CreateOrderResult.MenuItemInactive
        }

        val now = clockProvider.nowMillis()

        var subtotal = request.items.sumOf { it.menuItem.price * it.quantity }
        val service = if (request.orderType == OrderType.DINE_IN) calculatePercent(
            subtotal,
            SERVICE_PERCENT
        ) else 0L
        subtotal += service
        val discount = request.promoCode
            ?.takeIf { it.isNotBlank() }
            ?.let { promo ->
                val discount = orderRepo.getDiscountByPromoCode(promo)
                    ?: return CreateOrderResult.DiscountNotFound
                validateDiscount(discount, subtotal, now)?.let { return it }
                discount
            }
        val discountAmount = discount?.calculateDiscountAmount(subtotal) ?: 0L
        val taxableAmount = (subtotal - discountAmount).coerceAtLeast(0)
        val taxAmount = calculatePercent(taxableAmount, TAX_PERCENT)
        val totalAmount = (taxableAmount + taxAmount).coerceAtLeast(0)
        val orderId = uuidProvider.randomUuid()

        val order = Order(
            id = orderId,
            cashierId = cashier.id,
            cashierName = cashier.username,
            subtotalAmount = subtotal,
            taxAmount = taxAmount,
            discountAmount = discountAmount,
            totalAmount = totalAmount,
            serviceAmount = service,
            discountId = discount?.id,
            discountPromoCode = discount?.promoCode,
            discountPercent = discount?.percent,
            discountMinValue = discount?.minValue,
            discountMaxValue = discount?.maxValue,
            orderStatus = OrderStatus.PendingPayment,
            paymentStatus = PaymentStatus.NotStarted,
            printStatus = PrintStatus.NotPrinted,
            createdAt = now,
            paidAt = null,
            cancelledAt = null,
            syncedAt = null,
        )

        val remainingDiscount = mutableMapOf<String, Long>()
        val remainingTax = mutableMapOf<String, Long>()
        val itemSubtotals = request.items.map { it.menuItem.id to it.menuItem.price * it.quantity }
        allocateAmount(
            discountAmount,
            itemSubtotals
        ).forEach { (id, amount) -> remainingDiscount[id] = amount }

        allocateAmount(taxAmount, itemSubtotals).forEach { (id, amount) ->
            remainingTax[id] = amount
        }

        val orderItems = request.items.map { item ->
            val lineSubtotal = item.menuItem.price * item.quantity
            val lineDiscount = remainingDiscount[item.menuItem.id] ?: 0L
            val lineTax = remainingTax[item.menuItem.id] ?: 0L
            OrderItem(
                id = uuidProvider.randomUuid(),
                orderId = orderId,
                menuItemId = item.menuItem.id,
                category = item.menuItem.category,
                name = item.menuItem.name,
                description = item.menuItem.description,
                imageUrl = item.menuItem.imageUrl,
                localImageUrl = item.menuItem.localImageUrl,
                quantity = item.quantity,
                unitPrice = item.menuItem.price,
                subtotalAmount = lineSubtotal,
                discountAmount = lineDiscount,
                taxAmount = lineTax,
                totalAmount = (lineSubtotal - lineDiscount + lineTax).coerceAtLeast(0),
            )
        }

        return runCatching {
            orderRepo.createOrder(order, orderItems)
            CreateOrderResult.Success(orderId)
        }.getOrElse { CreateOrderResult.Failed(it.message ?: "Could not create order.") }
    }

    private fun validateDiscount(
        discount: Discount,
        subtotal: Long,
        now: Long
    ): CreateOrderResult? {
        if (now < discount.startAt) return CreateOrderResult.DiscountNotStartedYet
        if (now > discount.endAt) return CreateOrderResult.DiscountExpired
        if (subtotal.toMajorAmount() < discount.minValue) return CreateOrderResult.DiscountMinValueNotReached
        return null
    }

    private fun Discount.calculateDiscountAmount(subtotal: Long): Long {
        val rawDiscount = (subtotal * percent / 100.0).roundToLong()
        val maxDiscount = maxValue.toMinorAmount()
        return minOf(rawDiscount, maxDiscount, subtotal).coerceAtLeast(0)
    }

    private fun calculatePercent(amount: Long, percent: Int): Long =
        (amount * percent + 50) / 100

    private fun allocateAmount(
        totalAmount: Long,
        weightedLines: List<Pair<String, Long>>
    ): Map<String, Long> {
        if (totalAmount <= 0 || weightedLines.isEmpty()) return emptyMap()
        val totalWeight = weightedLines.sumOf { it.second }.takeIf { it > 0 } ?: return emptyMap()
        var allocated = 0L
        return weightedLines.mapIndexed { index, (id, weight) ->
            val amount = if (index == weightedLines.lastIndex) {
                totalAmount - allocated
            } else {
                ((totalAmount * weight).toDouble() / totalWeight).roundToLong()
                    .also { allocated += it }
            }
            id to amount
        }.toMap()
    }

    private fun Double.toMinorAmount(): Long = (this * 100.0).roundToLong()
    private fun Long.toMajorAmount(): Double = this / 100.0

    private companion object {
        const val TAX_PERCENT = 14
        const val SERVICE_PERCENT = 10
    }
}
