package com.sahm.pos.orders

import com.sahm.pos.domain.entity.Discount
import com.sahm.pos.domain.entity.MenuItem
import com.sahm.pos.domain.entity.Order
import com.sahm.pos.domain.entity.OrderDetails
import com.sahm.pos.domain.entity.OrderItem
import com.sahm.pos.domain.entity.OrderStatus
import com.sahm.pos.domain.entity.OrderType
import com.sahm.pos.domain.entity.Payment
import com.sahm.pos.domain.entity.PaymentStatus
import com.sahm.pos.domain.entity.PrintStatus
import com.sahm.pos.domain.entity.Refund
import com.sahm.pos.domain.entity.RefundDetails
import com.sahm.pos.domain.entity.RefundItem
import com.sahm.pos.domain.entity.RefundStatus
import com.sahm.pos.domain.repository.OrderRepo
import com.sahm.pos.domain.usecase.GetOrdersUseCase
import com.sahm.pos.screens.orders.OrdersIntent
import com.sahm.pos.screens.orders.OrdersViewModel
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

@OptIn(ExperimentalCoroutinesApi::class)
class OrdersViewModelTest {

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun searchChangedFiltersOrdersById() = runTest {
        val viewModel = viewModel(
            listOf(
                order(id = "ORDER-100", createdAt = 1_000),
                order(id = "TAKE-200", createdAt = 2_000),
            ),
        )

        viewModel.onIntent(OrdersIntent.ScreenOpened)
        advanceUntilIdle()
        viewModel.onIntent(OrdersIntent.SearchChanged("100"))

        assertEquals(listOf("ORDER-100"), viewModel.state.value.orders.map { it.id })
        assertEquals("100", viewModel.state.value.searchQuery)
    }

    @Test
    fun searchIsCaseInsensitiveAndCombinesWithOrderTypeFilter() = runTest {
        val viewModel = viewModel(
            listOf(
                order(id = "DELIVERY-10", orderType = OrderType.DELIVERY, createdAt = 1_000),
                order(id = "DINE-10", orderType = OrderType.DINE_IN),
                order(id = "DELIVERY-20", orderType = OrderType.DELIVERY, createdAt = 2_000),
            ),
        )

        viewModel.onIntent(OrdersIntent.ScreenOpened)
        advanceUntilIdle()
        viewModel.onIntent(OrdersIntent.SearchChanged("delivery"))
        viewModel.onIntent(OrdersIntent.OrderTypeSelected(OrderType.DELIVERY))

        assertEquals(listOf("DELIVERY-20", "DELIVERY-10"), viewModel.state.value.orders.map { it.id })
    }

    private fun TestScope.viewModel(orders: List<Order>): OrdersViewModel {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        return OrdersViewModel(GetOrdersUseCase(FakeOrderRepo(orders)))
    }

    private class FakeOrderRepo(
        private val orders: List<Order>,
    ) : OrderRepo {
        override suspend fun getOrders(): List<Order> = orders

        override suspend fun getMenuItemById(id: String): MenuItem? = null

        override suspend fun getDiscountByPromoCode(promoCode: String): Discount? = null

        override suspend fun createOrder(order: Order, items: List<OrderItem>) = Unit

        override suspend fun getOrderDetails(orderId: String): OrderDetails? = null

        override suspend fun updateOrderAfterPayment(
            orderId: String,
            orderStatus: OrderStatus,
            paymentStatus: PaymentStatus,
            paidAt: Long?,
        ) = Unit

        override suspend fun updateOrderPaymentStatus(orderId: String, paymentStatus: PaymentStatus) = Unit

        override suspend fun updateOrderPrintStatus(orderId: String, printStatus: PrintStatus) = Unit

        override suspend fun upsertPayment(payment: Payment) = Unit

        override suspend fun getCompletedPaymentForOrder(orderId: String): Payment? = null

        override suspend fun createRefund(refund: Refund, items: List<RefundItem>) = Unit

        override suspend fun getRefundDetails(refundId: String): RefundDetails? = null

        override suspend fun updateRefundStatus(refundId: String, status: RefundStatus, completedAt: Long?) = Unit

        override suspend fun updateOrderRefundStatus(
            orderId: String,
            orderStatus: OrderStatus,
            paymentStatus: PaymentStatus,
        ) = Unit
    }

    private companion object {
        fun order(
            id: String,
            orderType: OrderType = OrderType.TAKEAWAY,
            createdAt: Long = 1_000,
        ) = Order(
            id = id,
            cashierId = "cashier-1",
            cashierName = "Mona",
            subtotalAmount = 1_000,
            taxAmount = 140,
            discountAmount = 0,
            totalAmount = 1_140,
            discountId = null,
            discountPromoCode = null,
            discountPercent = null,
            discountMinValue = null,
            discountMaxValue = null,
            orderType = orderType,
            orderStatus = OrderStatus.PendingPayment,
            paymentStatus = PaymentStatus.NotStarted,
            printStatus = PrintStatus.NotPrinted,
            createdAt = createdAt,
            paidAt = null,
            cancelledAt = null,
            syncedAt = null,
        )
    }
}
