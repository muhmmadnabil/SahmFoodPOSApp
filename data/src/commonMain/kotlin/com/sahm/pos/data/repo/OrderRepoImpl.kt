package com.sahm.pos.data.repo

import com.sahm.pos.data.local.database.SahmPosDatabase
import com.sahm.pos.domain.entity.Discount
import com.sahm.pos.domain.entity.MenuItem
import com.sahm.pos.domain.entity.Order
import com.sahm.pos.domain.entity.OrderDetails
import com.sahm.pos.domain.entity.OrderItem
import com.sahm.pos.domain.entity.OrderStatus
import com.sahm.pos.domain.entity.OrderType
import com.sahm.pos.domain.entity.Payment
import com.sahm.pos.domain.entity.PaymentStatus
import com.sahm.pos.domain.entity.PaymentType
import com.sahm.pos.domain.entity.PrintStatus
import com.sahm.pos.domain.entity.Refund
import com.sahm.pos.domain.entity.RefundDetails
import com.sahm.pos.domain.entity.RefundItem
import com.sahm.pos.domain.entity.RefundStatus
import com.sahm.pos.domain.entity.SyncAggregateType
import com.sahm.pos.domain.entity.SyncOutboxStatus
import com.sahm.pos.domain.entity.SyncOutboxType
import com.sahm.pos.domain.repository.OrderRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class OrderRepoImpl(
    private val database: SahmPosDatabase,
) : OrderRepo {
    override suspend fun getMenuItemById(id: String): MenuItem? =
        database.menuItemQueries.selectById(id) { itemId, category, name, description, imageUrl, localImageUrl, price, lastSyncedAt, isActive ->
            MenuItem(
                id = itemId,
                category = category,
                name = name,
                description = description,
                imageUrl = imageUrl,
                localImageUrl = localImageUrl,
                price = price,
                lastSyncedAt = lastSyncedAt,
                isActive = isActive == 1L,
            )
        }.executeAsOneOrNull()

    override suspend fun getMenuItemsByIds(ids: Collection<String>): List<MenuItem> {
        val distinctIds = ids.distinct()
        if (distinctIds.isEmpty()) return emptyList()
        return database.menuItemQueries.selectByIds(distinctIds) { itemId, category, name, description, imageUrl, localImageUrl, price, lastSyncedAt, isActive ->
            MenuItem(
                id = itemId,
                category = category,
                name = name,
                description = description,
                imageUrl = imageUrl,
                localImageUrl = localImageUrl,
                price = price,
                lastSyncedAt = lastSyncedAt,
                isActive = isActive == 1L,
            )
        }.executeAsList()
    }

    override suspend fun getDiscountByPromoCode(promoCode: String): Discount? =
        database.discountQueries.selectByPromoCode(promoCode) { id, code, percent, minValue, maxValue, startAt, endAt, syncedAt ->
            Discount(id, code, percent, minValue, maxValue, startAt, endAt, syncedAt)
        }.executeAsOneOrNull()

    override suspend fun createOrder(order: Order, items: List<OrderItem>) =
        withContext(Dispatchers.IO) {
            database.transaction {
                // The order and its outbox row are saved together so an offline order is never
                // created without a matching upload task.
                database.orderFlowQueries.insertOrder(
                    id = order.id,
                    cashier_id = order.cashierId,
                    cashier_name = order.cashierName,
                    subtotal_amount = order.subtotalAmount,
                    tax_amount = order.taxAmount,
                    discount_amount = order.discountAmount,
                    service_amount = order.serviceAmount,
                    total_amount = order.totalAmount,
                    discount_id = order.discountId,
                    discount_promo_code = order.discountPromoCode,
                    discount_percent = order.discountPercent,
                    discount_min_value = order.discountMinValue,
                    discount_max_value = order.discountMaxValue,
                    order_type = order.orderType.name,
                    order_status = order.orderStatus.name,
                    payment_status = order.paymentStatus.name,
                    print_status = order.printStatus.name,
                    created_at = order.createdAt,
                    paid_at = order.paidAt,
                    cancelled_at = order.cancelledAt,
                    synced_at = order.syncedAt,
                )

                items.forEach { item ->
                    database.orderFlowQueries.insertOrderItem(
                        id = item.id,
                        order_id = item.orderId,
                        menu_item_id = item.menuItemId,
                        category = item.category,
                        name = item.name,
                        description = item.description,
                        image_url = item.imageUrl,
                        local_image_url = item.localImageUrl,
                        quantity = item.quantity.toLong(),
                        unit_price = item.unitPrice,
                        subtotal_amount = item.subtotalAmount,
                        discount_amount = item.discountAmount,
                        tax_amount = item.taxAmount,
                        total_amount = item.totalAmount,
                    )
                }

                insertOutboxIfAbsent(
                    type = SyncOutboxType.CREATE_ORDER,
                    aggregateType = SyncAggregateType.ORDER,
                    aggregateId = order.id,
                    payload = order.toPayload(items),
                    now = order.createdAt,
                )
            }
        }

    override suspend fun getOrders(): List<Order> =
        database.orderFlowQueries.selectOrders(::mapOrder).executeAsList()

    override suspend fun getOrderDetails(orderId: String): OrderDetails? {
        val order =
            database.orderFlowQueries.selectOrderById(orderId, ::mapOrder).executeAsOneOrNull()
                ?: return null
        val items = database.orderFlowQueries.selectOrderItemsByOrderId(orderId, ::mapOrderItem)
            .executeAsList()
        val payments =
            database.orderFlowQueries.selectPaymentsByOrderId(orderId, ::mapPayment).executeAsList()
        val refunds =
            database.orderFlowQueries.selectRefundsByOrderId(orderId, ::mapRefund).executeAsList()
                .map { refund ->
                    RefundDetails(
                        refund = refund,
                        items = database.orderFlowQueries.selectRefundItemsByRefundId(
                            refund.id,
                            ::mapRefundItem
                        ).executeAsList(),
                    )
                }
        return OrderDetails(order, items, payments, refunds)
    }

    override suspend fun updateOrderAfterPayment(
        orderId: String,
        orderStatus: OrderStatus,
        paymentStatus: PaymentStatus,
        paidAt: Long?,
    ) {
        withContext(Dispatchers.IO) {
            database.orderFlowQueries.updateOrderAfterPayment(
                order_status = orderStatus.name,
                payment_status = paymentStatus.name,
                paid_at = paidAt,
                id = orderId
            )
        }
    }

    override suspend fun updateOrderPaymentStatus(orderId: String, paymentStatus: PaymentStatus) {
        database.orderFlowQueries.updateOrderPaymentStatus(paymentStatus.name, orderId)
    }

    override suspend fun updateOrderPrintStatus(orderId: String, printStatus: PrintStatus) {
        database.transaction {
            database.orderFlowQueries.updateOrderPrintStatus(printStatus.name, orderId)
        }
    }

    override suspend fun updateOrderRefundStatus(
        orderId: String,
        orderStatus: OrderStatus,
        paymentStatus: PaymentStatus,
    ) {
        database.orderFlowQueries.updateOrderRefundStatus(
            orderStatus.name,
            paymentStatus.name,
            orderId
        )
    }

    override suspend fun upsertPayment(payment: Payment) = withContext(Dispatchers.IO) {
        database.transaction {
            database.orderFlowQueries.insertOrReplacePayment(
                id = payment.id,
                order_id = payment.orderId,
                method = payment.type.name,
                status = payment.status.name,
                amount = payment.amount,
                transaction_id = payment.transactionId,
                gateway_reference = payment.gatewayReference,
                authorization_code = payment.authorizationCode,
                card_brand = payment.cardBrand,
                card_last4 = payment.cardLast4,
                failure_reason = payment.failureReason,
                created_at = payment.createdAt,
                completed_at = payment.completedAt,
                synced_at = payment.syncedAt,
            )
            if (payment.status == PaymentStatus.Paid) {
                // Only completed payments are uploaded. Failed/processing attempts stay local so
                // retrying a card payment does not create remote noise or duplicate charges.
                insertOutboxIfAbsent(
                    type = SyncOutboxType.CREATE_PAYMENT,
                    aggregateType = SyncAggregateType.PAYMENT,
                    aggregateId = payment.id,
                    payload = payment.toPayload(),
                    now = payment.completedAt ?: payment.createdAt,
                )
            }
        }
    }

    override suspend fun getCompletedPaymentForOrder(orderId: String): Payment? =
        database.orderFlowQueries.selectCompletedPaymentForOrder(orderId, ::mapPayment)
            .executeAsOneOrNull()

    override suspend fun createRefund(refund: Refund, items: List<RefundItem>) {
        database.transaction {
            database.orderFlowQueries.insertRefund(
                id = refund.id,
                order_id = refund.orderId,
                payment_id = refund.paymentId,
                refund_status = refund.refundStatus.name,
                refund_method = refund.refundType.name,
                amount = refund.amount,
                reason = refund.reason,
                created_at = refund.createdAt,
                completed_at = refund.completedAt,
                synced_at = refund.syncedAt,
            )
            items.forEach { item ->
                database.orderFlowQueries.insertRefundItem(
                    id = item.id,
                    refund_id = item.refundId,
                    order_item_id = item.orderItemId,
                    menu_item_id = item.menuItemId,
                    item_name = item.itemName,
                    quantity = item.quantity.toLong(),
                    unit_price = item.unitPrice,
                    subtotal_amount = item.subtotalAmount,
                    discount_amount = item.discountAmount,
                    tax_amount = item.taxAmount,
                    total_refund_amount = item.totalRefundAmount,
                )
            }
            // Refunds also go through the outbox because the cashier can complete the local action
            // before the order/payment upload has reached the server.
            insertOutboxIfAbsent(
                type = SyncOutboxType.CREATE_REFUND,
                aggregateType = SyncAggregateType.REFUND,
                aggregateId = refund.id,
                payload = refund.toPayload(items),
                now = refund.createdAt,
            )
        }
    }

    override suspend fun getRefundDetails(refundId: String): RefundDetails? {
        val refund =
            database.orderFlowQueries.selectRefundById(refundId, ::mapRefund).executeAsOneOrNull()
                ?: return null
        return RefundDetails(
            refund = refund,
            items = database.orderFlowQueries.selectRefundItemsByRefundId(refundId, ::mapRefundItem)
                .executeAsList(),
        )
    }

    override suspend fun updateRefundStatus(
        refundId: String,
        status: RefundStatus,
        completedAt: Long?
    ) {
        database.orderFlowQueries.updateRefundStatus(status.name, completedAt, refundId)
    }

    private fun mapOrder(
        id: String,
        cashier_id: String,
        cashier_name: String,
        subtotal_amount: Long,
        tax_amount: Long,
        discount_amount: Long,
        service_amount: Long,
        total_amount: Long,
        discount_id: String?,
        discount_promo_code: String?,
        discount_percent: Double?,
        discount_min_value: Double?,
        discount_max_value: Double?,
        order_type: String,
        order_status: String,
        payment_status: String,
        print_status: String,
        created_at: Long,
        paid_at: Long?,
        cancelled_at: Long?,
        synced_at: Long?,
    ) = Order(
        id = id,
        cashierId = cashier_id,
        cashierName = cashier_name,
        subtotalAmount = subtotal_amount,
        taxAmount = tax_amount,
        discountAmount = discount_amount,
        totalAmount = total_amount,
        discountId = discount_id,
        discountPromoCode = discount_promo_code,
        discountPercent = discount_percent,
        discountMinValue = discount_min_value,
        discountMaxValue = discount_max_value,
        orderType = OrderType.valueOf(order_type),
        orderStatus = OrderStatus.valueOf(order_status),
        paymentStatus = PaymentStatus.valueOf(payment_status),
        printStatus = PrintStatus.valueOf(print_status),
        createdAt = created_at,
        paidAt = paid_at,
        cancelledAt = cancelled_at,
        syncedAt = synced_at,
        serviceAmount = service_amount,
    )

    private fun mapOrderItem(
        id: String,
        order_id: String,
        menu_item_id: String,
        category: String,
        name: String,
        description: String,
        image_url: String,
        local_image_url: String?,
        quantity: Long,
        unit_price: Long,
        subtotal_amount: Long,
        discount_amount: Long,
        tax_amount: Long,
        total_amount: Long,
    ) = OrderItem(
        id = id,
        orderId = order_id,
        menuItemId = menu_item_id,
        category = category,
        name = name,
        description = description,
        imageUrl = image_url,
        localImageUrl = local_image_url,
        quantity = quantity.toInt(),
        unitPrice = unit_price,
        subtotalAmount = subtotal_amount,
        discountAmount = discount_amount,
        taxAmount = tax_amount,
        totalAmount = total_amount,
    )

    private fun mapPayment(
        id: String,
        order_id: String,
        method: String,
        status: String,
        amount: Long,
        transaction_id: String?,
        gateway_reference: String?,
        authorization_code: String?,
        card_brand: String?,
        card_last4: String?,
        failure_reason: String?,
        created_at: Long,
        completed_at: Long?,
        synced_at: Long?,
    ) = Payment(
        id = id,
        orderId = order_id,
        type = PaymentType.valueOf(method),
        status = PaymentStatus.valueOf(status),
        amount = amount,
        transactionId = transaction_id,
        gatewayReference = gateway_reference,
        authorizationCode = authorization_code,
        cardBrand = card_brand,
        cardLast4 = card_last4,
        failureReason = failure_reason,
        createdAt = created_at,
        completedAt = completed_at,
        syncedAt = synced_at,
    )

    private fun mapRefund(
        id: String,
        order_id: String,
        payment_id: String?,
        refund_status: String,
        refund_method: String,
        amount: Long,
        reason: String?,
        created_at: Long,
        completed_at: Long?,
        synced_at: Long?,
    ) = Refund(
        id = id,
        orderId = order_id,
        paymentId = payment_id,
        refundStatus = RefundStatus.valueOf(refund_status),
        refundType = PaymentType.valueOf(refund_method),
        amount = amount,
        reason = reason,
        createdAt = created_at,
        completedAt = completed_at,
        syncedAt = synced_at,
    )

    private fun mapRefundItem(
        id: String,
        refund_id: String,
        order_item_id: String,
        menu_item_id: String,
        item_name: String,
        quantity: Long,
        unit_price: Long,
        subtotal_amount: Long,
        discount_amount: Long,
        tax_amount: Long,
        total_refund_amount: Long,
    ) = RefundItem(
        id = id,
        refundId = refund_id,
        orderItemId = order_item_id,
        menuItemId = menu_item_id,
        itemName = item_name,
        quantity = quantity.toInt(),
        unitPrice = unit_price,
        subtotalAmount = subtotal_amount,
        discountAmount = discount_amount,
        taxAmount = tax_amount,
        totalRefundAmount = total_refund_amount,
    )

    private fun insertOutboxIfAbsent(
        type: SyncOutboxType,
        aggregateType: SyncAggregateType,
        aggregateId: String,
        payload: JsonObject,
        now: Long,
    ) {
        val idempotencyKey = createSyncIdempotencyKey(type, aggregateId)

        // The idempotency key is also the row id, and INSERT OR IGNORE makes repeated local saves
        // safe. A retry should upload the same business action, not create a second outbox task.
        database.syncOutboxQueries.insertOutboxItemOrIgnore(
            id = idempotencyKey, type = type.name,
            aggregate_id = aggregateId,
            aggregate_type = aggregateType.name,
            payload_json = payload.toString(),
            idempotency_key = idempotencyKey,
            status = SyncOutboxStatus.PENDING.name,
            retry_count = 0,
            max_retries = 10,
            next_attempt_at = null,
            created_at = now,
            updated_at = now,
            locked_at = null,
            last_error_code = null,
            last_error_message = null
        )
    }

    private fun Order.toPayload(items: List<OrderItem>): JsonObject =
        buildJsonObject {
            put("orderId", id)
            put("cashierId", cashierId)
            put("cashierName", cashierName)
            put("subtotalAmount", subtotalAmount)
            put("taxAmount", taxAmount)
            put("discountAmount", discountAmount)
            put("serviceAmount", serviceAmount)
            put("totalAmount", totalAmount)
            put("orderType", orderType.name)
            put("orderStatus", orderStatus.name)
            put("paymentStatus", paymentStatus.name)
            put("printStatus", printStatus.name)
            put("createdAt", createdAt)
            put("itemsCount", items.size)
        }

    private fun Payment.toPayload(): JsonObject =
        buildJsonObject {
            put("paymentId", id)
            put("orderId", orderId)
            put("method", type.name)
            put("status", status.name)
            put("amount", amount)
            put("transactionId", transactionId)
            put("gatewayReference", gatewayReference)
            put("authorizationCode", authorizationCode)
            put("cardBrand", cardBrand)
            put("cardLast4", cardLast4)
            put("createdAt", createdAt)
            put("completedAt", completedAt)
        }

    private fun Refund.toPayload(items: List<RefundItem>): JsonObject =
        buildJsonObject {
            put("refundId", id)
            put("orderId", orderId)
            put("paymentId", paymentId)
            put("status", refundStatus.name)
            put("method", refundType.name)
            put("amount", amount)
            put("reason", reason)
            put("createdAt", createdAt)
            put("itemsCount", items.size)
        }

    fun createSyncIdempotencyKey(type: SyncOutboxType, aggregateId: String): String =
        "${type.name}:$aggregateId"
}
