package com.sahm.pos.domain.repository

import com.sahm.pos.domain.entity.MenuItem
import com.sahm.pos.domain.entity.Discount
import com.sahm.pos.domain.entity.Order
import com.sahm.pos.domain.entity.OrderDetails
import com.sahm.pos.domain.entity.OrderItem
import com.sahm.pos.domain.entity.OrderStatus
import com.sahm.pos.domain.entity.Payment
import com.sahm.pos.domain.entity.PaymentStatus
import com.sahm.pos.domain.entity.PrintStatus
import com.sahm.pos.domain.entity.Refund
import com.sahm.pos.domain.entity.RefundDetails
import com.sahm.pos.domain.entity.RefundItem
import com.sahm.pos.domain.entity.RefundStatus

interface OrderRepo {
    suspend fun getMenuItemById(id: String): MenuItem?
    suspend fun getMenuItemsByIds(ids: Collection<String>): List<MenuItem> =
        ids.distinct().mapNotNull { getMenuItemById(it) }

    suspend fun getDiscountByPromoCode(promoCode: String): Discount?
    suspend fun createOrder(order: Order, items: List<OrderItem>)
    suspend fun getOrders(): List<Order>
    suspend fun getOrderDetails(orderId: String): OrderDetails?
    suspend fun updateOrderAfterPayment(
        orderId: String,
        orderStatus: OrderStatus,
        paymentStatus: PaymentStatus,
        paidAt: Long?,
    )
    suspend fun updateOrderPaymentStatus(orderId: String, paymentStatus: PaymentStatus)
    suspend fun updateOrderPrintStatus(orderId: String, printStatus: PrintStatus)
    suspend fun upsertPayment(payment: Payment)
    suspend fun getCompletedPaymentForOrder(orderId: String): Payment?
    suspend fun createRefund(refund: Refund, items: List<RefundItem>)
    suspend fun getRefundDetails(refundId: String): RefundDetails?
    suspend fun updateRefundStatus(refundId: String, status: RefundStatus, completedAt: Long?)
    suspend fun updateOrderRefundStatus(orderId: String, orderStatus: OrderStatus, paymentStatus: PaymentStatus)
}
