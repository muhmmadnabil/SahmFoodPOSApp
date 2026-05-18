package com.sahm.pos.home

import com.sahm.pos.domain.ClockProvider
import com.sahm.pos.domain.CurrentEpochMillisProvider
import com.sahm.pos.domain.FakePaymentGateway
import com.sahm.pos.domain.ReceiptPrinter
import com.sahm.pos.domain.UUIDProvider
import com.sahm.pos.domain.entity.CurrentUser
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
import com.sahm.pos.domain.entity.TimeSyncInfo
import com.sahm.pos.domain.entity.User
import com.sahm.pos.domain.repository.AuthRepo
import com.sahm.pos.domain.repository.OrderRepo
import com.sahm.pos.domain.repository.SyncDataRepo
import com.sahm.pos.domain.usecase.ApplyDiscountUseCase
import com.sahm.pos.domain.usecase.CreateOrderUseCase
import com.sahm.pos.domain.usecase.GetAppTimeUseCase
import com.sahm.pos.domain.usecase.GetMenuItemsUseCase
import com.sahm.pos.domain.usecase.PayOrderByCardUseCase
import com.sahm.pos.domain.usecase.PayOrderByCashUseCase
import com.sahm.pos.domain.usecase.RetryPrintOrderReceiptUseCase
import com.sahm.pos.domain.results.PrintResult
import com.sahm.pos.domain.results.SyncResult
import com.sahm.pos.domain.sync.SyncReason
import com.sahm.pos.domain.sync.SyncScheduler
import com.sahm.pos.screens.home.HomeConstants
import com.sahm.pos.screens.home.HomeIntent
import com.sahm.pos.screens.home.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun screenOpenedLoadsCategoriesAndItems() = runTest {
        val viewModel = viewModel(items = menuItems)

        viewModel.onIntent(HomeIntent.ScreenOpened)
        advanceUntilIdle()

        assertEquals(listOf(HomeConstants.AllCategory, "Burgers", "Sides"), viewModel.state.value.categories)
        assertEquals(menuItems, viewModel.state.value.filteredMenuItems)
        assertEquals(HomeConstants.AllCategory, viewModel.state.value.selectedCategory)
    }

    @Test
    fun categorySelectedFiltersItems() = runTest {
        val viewModel = viewModel(items = menuItems)

        viewModel.onIntent(HomeIntent.ScreenOpened)
        advanceUntilIdle()
        viewModel.onIntent(HomeIntent.CategorySelected("Sides"))

        assertEquals(listOf(fries), viewModel.state.value.filteredMenuItems)
        assertEquals("Sides", viewModel.state.value.selectedCategory)
    }

    @Test
    fun searchChangedFiltersItemsWithinSelectedCategory() = runTest {
        val viewModel = viewModel(items = menuItems)

        viewModel.onIntent(HomeIntent.ScreenOpened)
        advanceUntilIdle()
        viewModel.onIntent(HomeIntent.CategorySelected("Burgers"))
        viewModel.onIntent(HomeIntent.SearchChanged("classic"))

        assertEquals(listOf(classicBurger), viewModel.state.value.filteredMenuItems)
    }

    @Test
    fun itemIntentsUpdateOrderAndTotals() = runTest {
        val viewModel = viewModel(items = menuItems)

        viewModel.onIntent(HomeIntent.ScreenOpened)
        advanceUntilIdle()
        viewModel.onIntent(HomeIntent.ItemAdded(classicBurger.id))
        viewModel.onIntent(HomeIntent.ItemQuantityChanged(classicBurger.id, 2))

        val state = viewModel.state.value
        assertEquals(1, state.orderItems.size)
        assertEquals(2, state.orderItems.first().quantity)
        assertEquals(11_404, state.subtotal)
        assertEquals(1_597, state.tax)
        assertEquals(13_001, state.total)
    }

    @Test
    fun itemRemovedClearsOrderTotals() = runTest {
        val viewModel = viewModel(items = menuItems)

        viewModel.onIntent(HomeIntent.ScreenOpened)
        advanceUntilIdle()
        viewModel.onIntent(HomeIntent.ItemAdded(fries.id))
        viewModel.onIntent(HomeIntent.ItemRemoved(fries.id))

        val state = viewModel.state.value
        assertTrue(state.orderItems.isEmpty())
        assertEquals(0, state.subtotal)
        assertEquals(0, state.tax)
        assertEquals(0, state.total)
    }

    @Test
    fun paymentTypeSelectedUpdatesState() = runTest {
        val viewModel = viewModel(items = menuItems)

        viewModel.onIntent(HomeIntent.PaymentTypeSelected(PaymentType.CARD))

        assertEquals(PaymentType.CARD, viewModel.state.value.selectedPaymentType)
    }

    @Test
    fun dineInOrderAddsServiceAndDiscountUpdatesTotals() = runTest {
        val viewModel = viewModel(
            items = menuItems,
            discounts = listOf(discount(promoCode = "SAVE10", percent = 10.0)),
        )

        viewModel.onIntent(HomeIntent.ScreenOpened)
        advanceUntilIdle()
        viewModel.onIntent(HomeIntent.ItemQuantityChanged(classicBurger.id, 2))
        viewModel.onIntent(HomeIntent.OrderTypeSelected(OrderType.DINE_IN))
        viewModel.onIntent(HomeIntent.DiscountChanged("SAVE10"))
        viewModel.onIntent(HomeIntent.DiscountSubmitted)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(11_404, state.subtotal)
        assertEquals(1_140, state.discount)
        assertEquals(1_026, state.service)
        assertEquals(1_581, state.tax)
        assertEquals(12_871, state.total)
        assertEquals(10.0, state.appliedDiscount?.percent)
        assertEquals("", state.discountText)
        assertEquals(false, state.isApplyingDiscount)
    }

    @Test
    fun discountChangedDoesNotSearchForPromoCodeUntilSubmitted() = runTest {
        val repo = FakeSyncDataRepo(menuItems, listOf(discount(promoCode = "SAVE10")))
        val viewModel = viewModel(repo)

        viewModel.onIntent(HomeIntent.DiscountChanged("SAVE10"))
        advanceUntilIdle()

        assertEquals(0, repo.discountLookupCount)
        assertEquals(null, viewModel.state.value.appliedDiscount)
    }

    @Test
    fun appliedPromoDiscountRecalculatesWhenItemsChange() = runTest {
        val viewModel = viewModel(
            items = menuItems,
            discounts = listOf(discount(promoCode = "SAVE10", percent = 10.0)),
        )

        viewModel.onIntent(HomeIntent.ScreenOpened)
        advanceUntilIdle()
        viewModel.onIntent(HomeIntent.ItemAdded(classicBurger.id))
        viewModel.onIntent(HomeIntent.DiscountChanged("SAVE10"))
        viewModel.onIntent(HomeIntent.DiscountSubmitted)
        advanceUntilIdle()

        assertEquals(570, viewModel.state.value.discount)

        viewModel.onIntent(HomeIntent.ItemQuantityChanged(classicBurger.id, 2))

        assertEquals(1_140, viewModel.state.value.discount)
    }

    @Test
    fun submittingNewValidPromoOverridesPreviousDiscount() = runTest {
        val viewModel = viewModel(
            items = menuItems,
            discounts = listOf(
                discount(promoCode = "SAVE10", percent = 10.0),
                discount(id = "discount-2", promoCode = "SAVE20", percent = 20.0),
            ),
        )

        viewModel.onIntent(HomeIntent.ScreenOpened)
        advanceUntilIdle()
        viewModel.onIntent(HomeIntent.ItemQuantityChanged(classicBurger.id, 2))
        viewModel.onIntent(HomeIntent.DiscountChanged("SAVE10"))
        viewModel.onIntent(HomeIntent.DiscountSubmitted)
        advanceUntilIdle()
        viewModel.onIntent(HomeIntent.DiscountChanged("SAVE20"))
        viewModel.onIntent(HomeIntent.DiscountSubmitted)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("", state.discountText)
        assertEquals(20.0, state.appliedDiscount?.percent)
        assertEquals(2_281, state.discount)
        assertEquals(false, state.isApplyingDiscount)
    }

    @Test
    fun makeOrderShowsPaymentPromptOnlyWhenOrderHasItems() = runTest {
        val viewModel = viewModel(items = menuItems)

        viewModel.onIntent(HomeIntent.MakeOrderClicked)
        assertEquals(false, viewModel.state.value.showPaymentPrompt)

        viewModel.onIntent(HomeIntent.ScreenOpened)
        advanceUntilIdle()
        viewModel.onIntent(HomeIntent.ItemAdded(fries.id))
        viewModel.onIntent(HomeIntent.MakeOrderClicked)

        assertEquals(true, viewModel.state.value.showPaymentPrompt)
    }

    @Test
    fun confirmPaymentClearsCurrentOrderAndClosesPrompt() = runTest {
        val viewModel = viewModel(items = menuItems)

        viewModel.onIntent(HomeIntent.ScreenOpened)
        advanceUntilIdle()
        viewModel.onIntent(HomeIntent.ItemAdded(fries.id))
        viewModel.onIntent(HomeIntent.DiscountChanged("1.50"))
        viewModel.onIntent(HomeIntent.MakeOrderClicked)
        viewModel.onIntent(HomeIntent.ConfirmPaymentClicked)

        val state = viewModel.state.value
        assertTrue(state.orderItems.isEmpty())
        assertEquals("", state.discountText)
        assertEquals(0, state.total)
        assertEquals(false, state.showPaymentPrompt)
    }

    @Test
    fun changingPaymentTypeBeforePaymentReusesCreatedOrder() = runTest {
        val repo = FakeOrderRepo(menuItems)
        val viewModel = orderFlowViewModel(repo)

        viewModel.onIntent(HomeIntent.ScreenOpened)
        advanceUntilIdle()
        viewModel.onIntent(HomeIntent.ItemAdded(fries.id))
        viewModel.onIntent(HomeIntent.PaymentTypeSelected(PaymentType.CARD))
        viewModel.onIntent(HomeIntent.MakeOrderClicked)
        advanceUntilIdle()
        viewModel.onIntent(HomeIntent.ConfirmPaymentClicked)
        viewModel.onIntent(HomeIntent.CardPaymentDismissed)

        viewModel.onIntent(HomeIntent.MakeOrderClicked)
        advanceUntilIdle()
        viewModel.onIntent(HomeIntent.PaymentTypeSelected(PaymentType.CASH))
        viewModel.onIntent(HomeIntent.ConfirmPaymentClicked)
        advanceUntilIdle()

        assertEquals(1, repo.createdOrderCount)
        assertEquals(listOf("order-1"), repo.paidOrderIds)
        assertTrue(viewModel.state.value.orderItems.isEmpty())
        assertEquals(null, viewModel.state.value.createdOrderId)
    }

    @Test
    fun cardPaymentAcceptsTwoDigitExpiryYearAndClearsOrder() = runTest {
        val repo = FakeOrderRepo(menuItems)
        val viewModel = orderFlowViewModel(repo)

        viewModel.onIntent(HomeIntent.ScreenOpened)
        advanceUntilIdle()
        viewModel.onIntent(HomeIntent.ItemAdded(fries.id))
        viewModel.onIntent(HomeIntent.PaymentTypeSelected(PaymentType.CARD))
        viewModel.onIntent(HomeIntent.MakeOrderClicked)
        advanceUntilIdle()
        viewModel.onIntent(HomeIntent.ConfirmPaymentClicked)
        viewModel.onIntent(HomeIntent.CardNumberChanged("4242 4242 4242 4242"))
        viewModel.onIntent(HomeIntent.ExpiryMonthChanged("12"))
        viewModel.onIntent(HomeIntent.ExpiryYearChanged("26"))
        viewModel.onIntent(HomeIntent.CvvChanged("123"))
        viewModel.onIntent(HomeIntent.CardHolderNameChanged("Mona"))
        viewModel.onIntent(HomeIntent.PayByCardClicked)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.orderItems.isEmpty())
        assertEquals(false, state.isCardPaymentSheetVisible)
        assertEquals(null, state.errorMessage)
        assertEquals(listOf("order-1"), repo.paidOrderIds)
    }

    @Test
    fun failedCardPaymentKeepsDialogOpenWithErrorMessage() = runTest {
        val viewModel = orderFlowViewModel(FakeOrderRepo(menuItems))

        viewModel.onIntent(HomeIntent.ScreenOpened)
        advanceUntilIdle()
        viewModel.onIntent(HomeIntent.ItemAdded(fries.id))
        viewModel.onIntent(HomeIntent.PaymentTypeSelected(PaymentType.CARD))
        viewModel.onIntent(HomeIntent.MakeOrderClicked)
        advanceUntilIdle()
        viewModel.onIntent(HomeIntent.ConfirmPaymentClicked)
        viewModel.onIntent(HomeIntent.CardNumberChanged("5555"))
        viewModel.onIntent(HomeIntent.ExpiryMonthChanged("12"))
        viewModel.onIntent(HomeIntent.ExpiryYearChanged("26"))
        viewModel.onIntent(HomeIntent.CvvChanged("123"))
        viewModel.onIntent(HomeIntent.CardHolderNameChanged("Mona"))
        viewModel.onIntent(HomeIntent.PayByCardClicked)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(true, state.isCardPaymentSheetVisible)
        assertEquals("Unsupported test card", state.errorMessage)
    }

    private fun TestScope.viewModel(
        items: List<MenuItem>,
        discounts: List<Discount> = emptyList(),
    ): HomeViewModel = viewModel(FakeSyncDataRepo(items, discounts))

    private fun TestScope.viewModel(repo: FakeSyncDataRepo): HomeViewModel {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val orderRepo = FakeOrderRepo(menuItems)
        val clockProvider = ClockProvider { 2_000 }
        val uuidProvider = SequenceUuidProvider("order-1", "order-item-1", "payment-1")
        return HomeViewModel(
            getMenuItemsUseCase = GetMenuItemsUseCase(repo),
            applyDiscountUseCase = ApplyDiscountUseCase(
                syncDataRepo = repo,
                getAppTimeUseCase = GetAppTimeUseCase(repo, clockProvider),
            ),
            createOrderUseCase = CreateOrderUseCase(
                orderRepo = orderRepo,
                authRepo = FakeAuthRepo,
                uuidProvider = uuidProvider,
                timeProvider = CurrentEpochMillisProvider { 2_000 },
                syncScheduler = FakeSyncScheduler,
            ),
            payOrderByCashUseCase = PayOrderByCashUseCase(
                repo = orderRepo,
                clockProvider = clockProvider,
                uuidProvider = uuidProvider,
                receiptPrinter = FakeReceiptPrinter,
            ),
            payOrderByCardUseCase = PayOrderByCardUseCase(
                repo = orderRepo,
                clockProvider = clockProvider,
                uuidProvider = uuidProvider,
                paymentGateway = FakePaymentGateway(),
                receiptPrinter = FakeReceiptPrinter,
            ),
            retryPrintOrderReceiptUseCase = RetryPrintOrderReceiptUseCase(
                repo = orderRepo,
                receiptPrinter = FakeReceiptPrinter,
            ),
        )
    }

    private fun TestScope.orderFlowViewModel(repo: FakeOrderRepo): HomeViewModel {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val clockProvider = ClockProvider { 2_000 }
        val uuidProvider = SequenceUuidProvider("order-1", "order-item-1", "payment-1")
        return HomeViewModel(
            getMenuItemsUseCase = GetMenuItemsUseCase(FakeSyncDataRepo(menuItems)),
            applyDiscountUseCase = ApplyDiscountUseCase(
                syncDataRepo = FakeSyncDataRepo(menuItems),
                getAppTimeUseCase = GetAppTimeUseCase(FakeSyncDataRepo(menuItems), clockProvider),
            ),
            createOrderUseCase = CreateOrderUseCase(
                orderRepo = repo,
                authRepo = FakeAuthRepo,
                uuidProvider = uuidProvider,
                timeProvider = CurrentEpochMillisProvider { 2_000 },
                syncScheduler = FakeSyncScheduler,
            ),
            payOrderByCashUseCase = PayOrderByCashUseCase(
                repo = repo,
                clockProvider = clockProvider,
                uuidProvider = uuidProvider,
                receiptPrinter = FakeReceiptPrinter,
            ),
            payOrderByCardUseCase = PayOrderByCardUseCase(
                repo = repo,
                clockProvider = clockProvider,
                uuidProvider = uuidProvider,
                paymentGateway = FakePaymentGateway(),
                receiptPrinter = FakeReceiptPrinter,
            ),
            retryPrintOrderReceiptUseCase = RetryPrintOrderReceiptUseCase(
                repo = repo,
                receiptPrinter = FakeReceiptPrinter,
            ),
        )
    }

    private class FakeSyncDataRepo(
        private val activeItems: List<MenuItem>,
        private val discounts: List<Discount> = emptyList(),
    ) : SyncDataRepo {
        var discountLookupCount = 0
            private set

        override suspend fun hasUsers(): Boolean = false

        override suspend fun syncUsers(): SyncResult = SyncResult.Success(0, 0)

        override suspend fun syncMenuItems(): SyncResult = SyncResult.Success(0, 0)

        override suspend fun syncDiscounts(): SyncResult = SyncResult.Success(0, 0)

        override suspend fun getActiveMenuItems(): List<MenuItem> = activeItems

        override suspend fun getDiscountByPromoCode(promoCode: String): Discount? {
            discountLookupCount += 1
            return discounts.firstOrNull { it.promoCode == promoCode }
        }

        override suspend fun getUserCount(): Long = 0

        override suspend fun getMenuItemCount(): Long = activeItems.size.toLong()

        override suspend fun getLastUsersSyncAt(): Long? = null

        override suspend fun getLastMenuItemsSyncAt(): Long? = null

        override suspend fun saveTimeSyncInfo(info: TimeSyncInfo) = Unit

        override suspend fun getTimeSyncInfo(): TimeSyncInfo? = null

        override suspend fun getServerTimeStamp(): Long? = null

        override suspend fun getLastDiscountsSyncAt(): Long? = null

        override suspend fun getDiscountsCount(): Int = discounts.size
    }

    private class FakeOrderRepo(
        private val activeItems: List<MenuItem>,
    ) : OrderRepo {
        private val orders = mutableMapOf<String, Order>()
        private val orderItems = mutableMapOf<String, List<OrderItem>>()
        private val payments = mutableMapOf<String, MutableList<Payment>>()
        val paidOrderIds = mutableListOf<String>()
        var createdOrderCount = 0
            private set

        override suspend fun getMenuItemById(id: String): MenuItem? = activeItems.firstOrNull { it.id == id }

        override suspend fun getDiscountByPromoCode(promoCode: String): Discount? = null

        override suspend fun createOrder(order: Order, items: List<OrderItem>) {
            createdOrderCount += 1
            orders[order.id] = order
            orderItems[order.id] = items
        }

        override suspend fun getOrders(): List<Order> = orders.values.toList()

        override suspend fun getOrderDetails(orderId: String): OrderDetails? {
            val order = orders[orderId] ?: return null
            return OrderDetails(
                order = order,
                items = orderItems[orderId].orEmpty(),
                payments = payments[orderId].orEmpty(),
                refunds = emptyList(),
            )
        }

        override suspend fun updateOrderAfterPayment(
            orderId: String,
            orderStatus: OrderStatus,
            paymentStatus: PaymentStatus,
            paidAt: Long?,
        ) {
            orders[orderId] = orders.getValue(orderId)
                .copy(orderStatus = orderStatus, paymentStatus = paymentStatus, paidAt = paidAt)
            paidOrderIds += orderId
        }

        override suspend fun updateOrderPaymentStatus(orderId: String, paymentStatus: PaymentStatus) {
            orders[orderId] = orders.getValue(orderId).copy(paymentStatus = paymentStatus)
        }

        override suspend fun updateOrderPrintStatus(orderId: String, printStatus: PrintStatus) {
            orders[orderId] = orders.getValue(orderId).copy(printStatus = printStatus)
        }

        override suspend fun upsertPayment(payment: Payment) {
            val orderPayments = payments.getOrPut(payment.orderId) { mutableListOf() }
            val index = orderPayments.indexOfFirst { it.id == payment.id }
            if (index >= 0) {
                orderPayments[index] = payment
            } else {
                orderPayments += payment
            }
        }

        override suspend fun getCompletedPaymentForOrder(orderId: String): Payment? =
            payments[orderId]?.firstOrNull { it.status == PaymentStatus.Paid }

        override suspend fun createRefund(refund: Refund, items: List<RefundItem>) = Unit

        override suspend fun getRefundDetails(refundId: String): RefundDetails? = null

        override suspend fun updateRefundStatus(refundId: String, status: RefundStatus, completedAt: Long?) = Unit

        override suspend fun updateOrderRefundStatus(
            orderId: String,
            orderStatus: OrderStatus,
            paymentStatus: PaymentStatus,
        ) = Unit
    }

    private object FakeAuthRepo : AuthRepo {
        override suspend fun getUserByPhone(phone: String): User? = null

        override suspend fun saveCurrentUser(currentUser: CurrentUser) = Unit

        override suspend fun getCurrentUser(): CurrentUser = CurrentUser(
            id = "cashier-1",
            username = "Mona",
            phone = "020000000000",
        )

        override suspend fun updateUserLastLoginAt(userId: String, timestamp: String) = Unit
    }

    private object FakeReceiptPrinter : ReceiptPrinter {
        override suspend fun printOrderReceipt(orderId: String): PrintResult = PrintResult.Success

        override suspend fun printRefundReceipt(refundId: String): PrintResult = PrintResult.Success
    }

    private object FakeSyncScheduler : SyncScheduler {
        override fun scheduleSync(reason: SyncReason) = Unit
    }

    private class SequenceUuidProvider(
        private vararg val ids: String,
    ) : UUIDProvider {
        private var index = 0

        override fun randomUuid(): String = ids.getOrElse(index++) { "id-$index" }
    }

    private companion object {
        val classicBurger = MenuItem(
            id = "classic_burger",
            category = "Burgers",
            name = "Classic Burger",
            description = "Beef patty with cheese",
            imageUrl = "https://example.com/classic.webp",
            price = 5_702,
        )
        val spicyBurger = MenuItem(
            id = "spicy_burger",
            category = "Burgers",
            name = "Spicy Burger",
            description = "Jalapeno and sauce",
            imageUrl = "https://example.com/spicy.webp",
            price = 6_000,
        )
        val fries = MenuItem(
            id = "fries",
            category = "Sides",
            name = "Fries",
            description = "Crispy fries",
            imageUrl = "https://example.com/fries.webp",
            price = 2_250,
        )
        val menuItems = listOf(classicBurger, spicyBurger, fries)

        fun discount(
            id: String = "discount-1",
            promoCode: String = "SAVE10",
            percent: Double = 10.0,
            minValue: Double = 0.0,
            maxValue: Double = 50.0,
            startAt: Long = 1_000,
            endAt: Long = 3_000,
        ) = Discount(id, promoCode, percent, minValue, maxValue, startAt, endAt, 2_000)
    }
}
