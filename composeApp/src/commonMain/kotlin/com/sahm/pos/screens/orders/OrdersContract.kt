package com.sahm.pos.screens.orders

import com.sahm.pos.domain.entity.OrderStatus
import com.sahm.pos.domain.entity.OrderType
import com.sahm.pos.domain.entity.PaymentStatus
import com.sahm.pos.domain.entity.PaymentType
import com.sahm.pos.domain.entity.PrintStatus
import com.sahm.pos.domain.entity.RefundStatus
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class OrdersUiState(
    val orders: ImmutableList<OrderUiState> = persistentListOf(),
    val searchQuery: String = "",
    val selectedOrderType: OrderType? = null,
    val sortDirection: OrdersSortDirection = OrdersSortDirection.NewestFirst,
    val isLoading: Boolean = false,
    val selectedOrderDetails: OrderDetailsUiState? = null,
    val isOrderDetailsLoading: Boolean = false,
    val isPrinting: Boolean = false,
    val detailsErrorMessage: String? = null,
)

data class OrderUiState(
    val id: String,
    val cashierName: String,
    val orderType: OrderType,
    val orderStatus: OrderStatus,
    val paymentStatus: PaymentStatus,
    val printStatus: PrintStatus,
    val totalAmount: Long,
    val createdAt: Long,
)

data class OrderDetailsUiState(
    val id: String,
    val cashierName: String,
    val orderType: OrderType,
    val orderStatus: OrderStatus,
    val paymentStatus: PaymentStatus,
    val printStatus: PrintStatus,
    val subtotalAmount: Long,
    val serviceAmount: Long,
    val discountAmount: Long,
    val taxAmount: Long,
    val totalAmount: Long,
    val discountPromoCode: String?,
    val createdAt: Long,
    val paidAt: Long?,
    val items: ImmutableList<OrderItemUiState> = persistentListOf(),
    val payments: ImmutableList<OrderPaymentUiState> = persistentListOf(),
    val refunds: ImmutableList<OrderRefundUiState> = persistentListOf(),
) {
    val canPrintAgain: Boolean =
        orderStatus in setOf(OrderStatus.Paid, OrderStatus.PartiallyRefunded, OrderStatus.Refunded)
}

data class OrderItemUiState(
    val name: String,
    val quantity: Int,
    val unitPrice: Long,
    val subtotalAmount: Long,
    val discountAmount: Long,
    val taxAmount: Long,
    val totalAmount: Long,
)

data class OrderPaymentUiState(
    val type: PaymentType,
    val status: PaymentStatus,
    val amount: Long,
    val completedAt: Long?,
)

data class OrderRefundUiState(
    val status: RefundStatus,
    val type: PaymentType,
    val amount: Long,
    val completedAt: Long?,
)

enum class OrdersSortDirection {
    NewestFirst,
    OldestFirst,
}

sealed interface OrdersIntent {
    data object ScreenOpened : OrdersIntent
    data class SearchChanged(val query: String) : OrdersIntent
    data class OrderTypeSelected(val orderType: OrderType?) : OrdersIntent
    data class SortDirectionSelected(val sortDirection: OrdersSortDirection) : OrdersIntent
    data class OrderSelected(val orderId: String) : OrdersIntent
    data object OrderDetailsDismissed : OrdersIntent
    data object PrintAgainClicked : OrdersIntent
}
