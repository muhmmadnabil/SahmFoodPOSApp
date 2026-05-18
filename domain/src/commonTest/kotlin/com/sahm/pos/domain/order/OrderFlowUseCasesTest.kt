package com.sahm.pos.domain.order

import com.sahm.pos.domain.entity.CurrentUser
import com.sahm.pos.domain.entity.Discount
import com.sahm.pos.domain.entity.MenuItem
import com.sahm.pos.domain.entity.Order
import com.sahm.pos.domain.entity.OrderDetails
import com.sahm.pos.domain.entity.OrderItem
import com.sahm.pos.domain.entity.OrderStatus
import com.sahm.pos.domain.entity.Payment
import com.sahm.pos.domain.entity.PaymentStatus
import com.sahm.pos.domain.entity.PaymentType
import com.sahm.pos.domain.entity.PrintStatus
import com.sahm.pos.domain.entity.Refund
import com.sahm.pos.domain.entity.RefundDetails
import com.sahm.pos.domain.entity.RefundItem
import com.sahm.pos.domain.entity.RefundStatus
import com.sahm.pos.domain.entity.User
import com.sahm.pos.domain.repository.AuthRepo
import com.sahm.pos.domain.repository.OrderRepo
import com.sahm.pos.domain.CurrentEpochMillisProvider
import com.sahm.pos.domain.entity.CardPaymentRequest
import com.sahm.pos.domain.ClockProvider
import com.sahm.pos.domain.entity.CreateOrderRequest
import com.sahm.pos.domain.results.CreateOrderResult
import com.sahm.pos.domain.usecase.CreateOrderUseCase
import com.sahm.pos.domain.entity.CreateRefundRequest
import com.sahm.pos.domain.results.CreateRefundResult
import com.sahm.pos.domain.usecase.CreateRefundUseCase
import com.sahm.pos.domain.FakePaymentGateway
import com.sahm.pos.domain.usecase.GetRefundableItemsUseCase
import com.sahm.pos.domain.results.PaymentGatewayResult
import com.sahm.pos.domain.usecase.PayOrderByCardUseCase
import com.sahm.pos.domain.usecase.PayOrderByCashUseCase
import com.sahm.pos.domain.results.PrintResult
import com.sahm.pos.domain.ReceiptPrinter
import com.sahm.pos.domain.UUIDProvider
import com.sahm.pos.domain.usecase.RefundByCashUseCase
import com.sahm.pos.domain.entity.RefundSelection
import com.sahm.pos.domain.sync.SyncReason
import com.sahm.pos.domain.sync.SyncScheduler
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class OrderFlowUseCasesTest {

    @Test
    fun createOrderPersistsSnapshotsAndInitialStatuses() = runTest {
        val repository = FakeOrderRepo(menuItems = listOf(burger))
        val useCase = createOrderUseCase(repository)

        val result = useCase(
            CreateOrderRequest(
                items = listOf(CreateOrderRequest.Item(menuItem = burger, quantity = 2)),
                promoCode = "SAVE10",
            )
        )

        assertEquals(CreateOrderResult.Success("order-1"), result)
        val order = requireNotNull(repository.orderDetails("order-1"))
        assertEquals("order-1", order.order.id)
        assertEquals("cashier-1", order.order.cashierId)
        assertEquals("Mona", order.order.cashierName)
        assertEquals(OrderStatus.PendingPayment, order.order.orderStatus)
        assertEquals(PaymentStatus.NotStarted, order.order.paymentStatus)
        assertEquals(PrintStatus.NotPrinted, order.order.printStatus)
        assertEquals(11_404, order.order.subtotalAmount)
        assertEquals(1_140, order.order.discountAmount)
        assertEquals(1_437, order.order.taxAmount)
        assertEquals(11_701, order.order.totalAmount)
        assertEquals("discount-1", order.order.discountId)
        assertEquals("SAVE10", order.order.discountPromoCode)
        assertEquals(10.0, order.order.discountPercent)
        assertEquals(0.0, order.order.discountMinValue)
        assertEquals(50.0, order.order.discountMaxValue)
        assertEquals(1, order.items.size)
        assertEquals("Classic Burger", order.items.first().name)
        assertEquals(5_702, order.items.first().unitPrice)
        assertEquals(2, order.items.first().quantity)
        assertEquals(11_404, order.items.first().subtotalAmount)
        assertEquals(1_140, order.items.first().discountAmount)
        assertEquals(1_437, order.items.first().taxAmount)
        assertEquals(11_701, order.items.first().totalAmount)
    }

    @Test
    fun createOrderRejectsMissingCashierExpiredDiscountAndCaseMismatch() = runTest {
        val repository = FakeOrderRepo(menuItems = listOf(burger))

        assertEquals(
            CreateOrderResult.CashierMissing,
            createOrderUseCase(repository, currentUser = null)(
                CreateOrderRequest(listOf(CreateOrderRequest.Item(burger, 1)))
            )
        )
        assertEquals(
            CreateOrderResult.DiscountNotFound,
            createOrderUseCase(repository)(
                CreateOrderRequest(listOf(CreateOrderRequest.Item(burger, 1)), promoCode = "save10")
            )
        )
        assertEquals(
            CreateOrderResult.DiscountExpired,
            createOrderUseCase(repository, now = 4_000)(
                CreateOrderRequest(listOf(CreateOrderRequest.Item(burger, 1)), promoCode = "SAVE10")
            )
        )
    }

    @Test
    fun cashPaymentMarksPaidAndPrintFailureDoesNotRollbackPayment() = runTest {
        val repository = FakeOrderRepo(menuItems = listOf(burger))
        createOrderUseCase(repository)(CreateOrderRequest(listOf(CreateOrderRequest.Item(burger, 1))))
        val printer = FakePrinter(orderResult = PrintResult.Failed("paper out"))

        val result = PayOrderByCashUseCase(
            repo = repository,
            clockProvider = ClockProvider { 2_500 },
            uuidProvider = SequenceUuidProvider("payment-1"),
            receiptPrinter = printer,
            printScope = this,
        )("order-1")
        advanceUntilIdle()

        assertEquals(Unit, result.getOrThrow())
        val details = requireNotNull(repository.orderDetails("order-1"))
        assertEquals(OrderStatus.Paid, details.order.orderStatus)
        assertEquals(PaymentStatus.Paid, details.order.paymentStatus)
        assertEquals(PrintStatus.Failed, details.order.printStatus)
        assertEquals(1, printer.orderPrints)
        assertEquals(PaymentType.CASH, details.payments.single().type)
        assertEquals(details.order.totalAmount, details.payments.single().amount)
    }

    @Test
    fun cardPaymentFailureLeavesOrderPendingAndAllowsCashRetry() = runTest {
        val repository = FakeOrderRepo(menuItems = listOf(burger))
        createOrderUseCase(repository)(CreateOrderRequest(listOf(CreateOrderRequest.Item(burger, 1))))

        val declined = PayOrderByCardUseCase(
            repo = repository,
            clockProvider = ClockProvider { 2_500 },
            uuidProvider = SequenceUuidProvider("card-payment-1"),
            paymentGateway = FakePaymentGateway(),
            receiptPrinter = FakePrinter(),
        )(
            CardPaymentRequest("order-1", 6_500, "4000 0000 0000 0002", "12", "2099", "123", "Mona")
        )

        assertEquals("Card declined", declined.exceptionOrNull()?.message)
        assertEquals(OrderStatus.PendingPayment, repository.orderDetails("order-1")?.order?.orderStatus)
        assertEquals(PaymentStatus.Failed, repository.orderDetails("order-1")?.order?.paymentStatus)
        assertEquals("Card declined", repository.orderDetails("order-1")?.payments?.single()?.failureReason)

        PayOrderByCashUseCase(repository, ClockProvider { 2_600 }, SequenceUuidProvider("payment-2"), FakePrinter())("order-1")

        assertEquals(OrderStatus.Paid, repository.orderDetails("order-1")?.order?.orderStatus)
        assertEquals(1, repository.orderDetails("order-1")?.payments?.count { it.status == PaymentStatus.Paid })
    }

    @Test
    fun fakePaymentGatewayUsesExactTestCardsAndDoesNotReturnFullCard() = runTest {
        val gateway = FakePaymentGateway()

        val success = gateway.pay(card("4242 4242 4242 4242"))
        assertIs<PaymentGatewayResult.Success>(success)
        assertEquals("4242", success.cardLast4)
        assertEquals("Visa", success.cardBrand)
        assertIs<PaymentGatewayResult.Failed>(gateway.pay(card("4000 0000 0000 0002")))
        assertEquals("Insufficient funds", (gateway.pay(card("4000 0000 0000 9995")) as PaymentGatewayResult.Failed).reason)
        assertEquals("Unsupported test card", (gateway.pay(card("5555")) as PaymentGatewayResult.Failed).reason)
    }

    @Test
    fun cardPaymentAcceptsTwoDigitExpiryYear() = runTest {
        val repository = FakeOrderRepo(menuItems = listOf(burger))
        createOrderUseCase(repository)(CreateOrderRequest(listOf(CreateOrderRequest.Item(burger, 1))))

        val result = PayOrderByCardUseCase(
            repo = repository,
            clockProvider = ClockProvider { 2_500 },
            uuidProvider = SequenceUuidProvider("card-payment-1"),
            paymentGateway = FakePaymentGateway(),
            receiptPrinter = FakePrinter(),
        )(
            card("4242 4242 4242 4242").copy(expiryYear = "26")
        )

        assertTrue(result.isSuccess)
        assertEquals(OrderStatus.Paid, repository.orderDetails("order-1")?.order?.orderStatus)
        assertEquals(PaymentStatus.Paid, repository.orderDetails("order-1")?.order?.paymentStatus)
    }

    @Test
    fun refundableItemsAndCashRefundUseSavedLineAmounts() = runTest {
        val repository = FakeOrderRepo(menuItems = listOf(burger))
        createOrderUseCase(repository)(CreateOrderRequest(listOf(CreateOrderRequest.Item(burger, 3))))
        PayOrderByCashUseCase(repository, ClockProvider { 2_500 }, SequenceUuidProvider("payment-1"), FakePrinter())("order-1")
        val orderItemId = requireNotNull(repository.orderDetails("order-1")).items.single().id
        val createRefund = CreateRefundUseCase(
            repository,
            ClockProvider { 3_000 },
            SequenceUuidProvider("refund-item-1", "refund-1"),
        )

        val refundResult = createRefund(
            CreateRefundRequest(
                orderId = "order-1",
                selections = listOf(RefundSelection(orderItemId, quantity = 1)),
                type = PaymentType.CASH,
                reason = "Wrong item",
            )
        )

        assertIs<CreateRefundResult.Success>(refundResult)
        assertEquals(1, GetRefundableItemsUseCase(repository)("order-1").getOrThrow().single().refundedQuantity)
        RefundByCashUseCase(repository, ClockProvider { 3_500 }, FakePrinter())(refundResult.refundId).getOrThrow()

        val details = requireNotNull(repository.orderDetails("order-1"))
        assertEquals(OrderStatus.PartiallyRefunded, details.order.orderStatus)
        assertEquals(PaymentStatus.PartiallyRefunded, details.order.paymentStatus)
        assertEquals(RefundStatus.Completed, details.refunds.single().refund.refundStatus)
        assertEquals("Wrong item", details.refunds.single().refund.reason)
        assertEquals(details.items.single().totalAmount / 3, details.refunds.single().refund.amount)
    }

    private fun createOrderUseCase(
        repository: FakeOrderRepo,
        currentUser: CurrentUser? = CurrentUser("cashier-1", "Mona", "020000000000"),
        now: Long = 2_000,
    ) = CreateOrderUseCase(
        orderRepo = repository,
        authRepo = FakeAuthRepo(currentUser),
        uuidProvider = SequenceUuidProvider("order-1", "order-item-1", "order-item-2", "order-item-3"),
        timeProvider = CurrentEpochMillisProvider { now },
        syncScheduler = FakeSyncScheduler,
    )

    private fun card(number: String) = CardPaymentRequest(
        orderId = "order-1",
        amount = 1_000,
        cardNumber = number,
        expiryMonth = "12",
        expiryYear = "2099",
        cvv = "123",
        cardHolderName = "Mona",
    )

    private companion object {
        val burger = MenuItem(
            id = "classic-burger",
            category = "Burgers",
            name = "Classic Burger",
            description = "Beef patty",
            imageUrl = "https://example.com/burger.webp",
            localImageUrl = "file:///burger.webp",
            price = 5_702,
        )
    }
}

private class SequenceUuidProvider(
    private vararg val values: String,
) : UUIDProvider {
    private var index = 0

    override fun randomUuid(): String =
        values.getOrNull(index++) ?: "uuid-$index"
}

private object FakeSyncScheduler : SyncScheduler {
    override fun scheduleSync(reason: SyncReason) = Unit
}

private class FakeAuthRepo(
    private val currentUser: CurrentUser?,
) : AuthRepo {
    override suspend fun getUserByPhone(phone: String): User? = null
    override suspend fun saveCurrentUser(currentUser: CurrentUser) = Unit
    override suspend fun getCurrentUser(): CurrentUser? = currentUser
    override suspend fun updateUserLastLoginAt(userId: String, timestamp: String) = Unit
}

private class FakePrinter(
    private val orderResult: PrintResult = PrintResult.Success,
    private val refundResult: PrintResult = PrintResult.Success,
) : ReceiptPrinter {
    var orderPrints = 0
        private set
    var refundPrints = 0
        private set

    override suspend fun printOrderReceipt(orderId: String): PrintResult {
        orderPrints += 1
        return orderResult
    }

    override suspend fun printRefundReceipt(refundId: String): PrintResult {
        refundPrints += 1
        return refundResult
    }
}

private class FakeOrderRepo(
    private val menuItems: List<MenuItem>,
    private val discounts: List<Discount> = listOf(
        Discount("discount-1", "SAVE10", 10.0, 0.0, 50.0, 1_000, 3_000, 2_000)
    ),
) : OrderRepo {
    private val orders = mutableMapOf<String, Order>()
    private val orderItems = mutableMapOf<String, MutableList<OrderItem>>()
    private val payments = mutableMapOf<String, Payment>()
    private val refunds = mutableMapOf<String, Refund>()
    private val refundItems = mutableMapOf<String, MutableList<RefundItem>>()

    override suspend fun getMenuItemById(id: String): MenuItem? = menuItems.firstOrNull { it.id == id }

    override suspend fun getDiscountByPromoCode(promoCode: String): Discount? =
        discounts.firstOrNull { it.promoCode == promoCode }

    override suspend fun createOrder(order: Order, items: List<OrderItem>) {
        orders[order.id] = order
        orderItems[order.id] = items.toMutableList()
    }

    override suspend fun getOrders(): List<Order> =
        orders.values.sortedByDescending { it.createdAt }

    override suspend fun getOrderDetails(orderId: String): OrderDetails? = orderDetails(orderId)

    fun orderDetails(orderId: String): OrderDetails? {
        val order = orders[orderId] ?: return null
        val orderRefunds = refunds.values
            .filter { it.orderId == orderId }
            .map { RefundDetails(it, refundItems[it.id].orEmpty()) }
        return OrderDetails(
            order = order,
            items = orderItems[orderId].orEmpty(),
            payments = payments.values.filter { it.orderId == orderId },
            refunds = orderRefunds,
        )
    }

    override suspend fun updateOrderAfterPayment(
        orderId: String,
        orderStatus: OrderStatus,
        paymentStatus: PaymentStatus,
        paidAt: Long?,
    ) {
        orders[orderId] = requireNotNull(orders[orderId]).copy(
            orderStatus = orderStatus,
            paymentStatus = paymentStatus,
            paidAt = paidAt,
        )
    }

    override suspend fun updateOrderPaymentStatus(orderId: String, paymentStatus: PaymentStatus) {
        orders[orderId] = requireNotNull(orders[orderId]).copy(paymentStatus = paymentStatus)
    }

    override suspend fun updateOrderPrintStatus(orderId: String, printStatus: PrintStatus) {
        orders[orderId] = requireNotNull(orders[orderId]).copy(printStatus = printStatus)
    }

    override suspend fun upsertPayment(payment: Payment) {
        payments[payment.id] = payment
    }

    override suspend fun getCompletedPaymentForOrder(orderId: String): Payment? =
        payments.values.firstOrNull { it.orderId == orderId && it.status == PaymentStatus.Paid }

    override suspend fun createRefund(refund: Refund, items: List<RefundItem>) {
        refunds[refund.id] = refund
        refundItems[refund.id] = items.toMutableList()
    }

    override suspend fun getRefundDetails(refundId: String): RefundDetails? {
        val refund = refunds[refundId] ?: return null
        return RefundDetails(refund, refundItems[refundId].orEmpty())
    }

    override suspend fun updateRefundStatus(refundId: String, status: RefundStatus, completedAt: Long?) {
        refunds[refundId] = requireNotNull(refunds[refundId]).copy(
            refundStatus = status,
            completedAt = completedAt,
        )
    }

    override suspend fun updateOrderRefundStatus(
        orderId: String,
        orderStatus: OrderStatus,
        paymentStatus: PaymentStatus,
    ) {
        orders[orderId] = requireNotNull(orders[orderId]).copy(
            orderStatus = orderStatus,
            paymentStatus = paymentStatus,
        )
    }
}
