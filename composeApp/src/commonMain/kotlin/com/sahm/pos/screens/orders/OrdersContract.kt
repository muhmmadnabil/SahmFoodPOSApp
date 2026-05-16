package com.sahm.pos.screens.orders

import com.sahm.pos.domain.entity.OrderStatus
import com.sahm.pos.domain.entity.OrderType
import com.sahm.pos.domain.entity.PaymentStatus
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class OrdersUiState(
    val orders: ImmutableList<OrderUiState> = persistentListOf(),
    val searchQuery: String = "",
    val selectedOrderType: OrderType? = null,
    val sortDirection: OrdersSortDirection = OrdersSortDirection.NewestFirst,
    val isLoading: Boolean = false,
)

data class OrderUiState(
    val id: String,
    val cashierName: String,
    val orderType: OrderType,
    val orderStatus: OrderStatus,
    val paymentStatus: PaymentStatus,
    val totalAmount: Long,
    val createdAt: Long,
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
}
