package com.sahm.pos.screens.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sahm.pos.domain.entity.Order
import com.sahm.pos.domain.entity.OrderDetails
import com.sahm.pos.domain.entity.OrderItem
import com.sahm.pos.domain.entity.OrderType
import com.sahm.pos.domain.entity.Payment
import com.sahm.pos.domain.entity.RefundDetails
import com.sahm.pos.domain.usecase.GetOrderDetailsUseCase
import com.sahm.pos.domain.usecase.GetOrdersUseCase
import com.sahm.pos.domain.usecase.RetryPrintOrderReceiptUseCase
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OrdersViewModel(
    private val getOrdersUseCase: GetOrdersUseCase,
    private val getOrderDetailsUseCase: GetOrderDetailsUseCase,
    private val retryPrintOrderReceiptUseCase: RetryPrintOrderReceiptUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(OrdersUiState())
    val state: StateFlow<OrdersUiState> = _state.asStateFlow()

    private var allOrders: List<Order> = emptyList()

    fun onIntent(intent: OrdersIntent) {
        when (intent) {
            OrdersIntent.ScreenOpened -> loadOrders()
            is OrdersIntent.SearchChanged -> updateSearch(intent.query)
            is OrdersIntent.OrderTypeSelected -> selectOrderType(intent.orderType)
            is OrdersIntent.SortDirectionSelected -> selectSortDirection(intent.sortDirection)
            is OrdersIntent.OrderSelected -> loadOrderDetails(intent.orderId)
            OrdersIntent.OrderDetailsDismissed -> dismissOrderDetails()
            OrdersIntent.PrintAgainClicked -> printAgain()
        }
    }

    private fun loadOrders() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            allOrders = getOrdersUseCase()
            _state.update {
                it.copy(
                    orders = filteredOrders(it.searchQuery, it.selectedOrderType, it.sortDirection).toImmutableList(),
                    isLoading = false,
                )
            }
        }
    }

    private fun loadOrderDetails(orderId: String) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isOrderDetailsLoading = true,
                    detailsErrorMessage = null,
                )
            }
            val details = getOrderDetailsUseCase(orderId)
            _state.update {
                it.copy(
                    selectedOrderDetails = details?.toUiState(),
                    isOrderDetailsLoading = false,
                    detailsErrorMessage = if (details == null) "Order not found" else null,
                )
            }
        }
    }

    private fun dismissOrderDetails() {
        _state.update {
            it.copy(
                selectedOrderDetails = null,
                isOrderDetailsLoading = false,
                isPrinting = false,
                detailsErrorMessage = null,
            )
        }
    }

    private fun printAgain() {
        viewModelScope.launch {
            val orderId = _state.value.selectedOrderDetails?.id ?: return@launch
            _state.update {
                it.copy(
                    isPrinting = true,
                    detailsErrorMessage = null,
                )
            }
            val result = retryPrintOrderReceiptUseCase(orderId)
            val details = getOrderDetailsUseCase(orderId)
            allOrders = getOrdersUseCase()
            _state.update {
                it.copy(
                    orders = filteredOrders(it.searchQuery, it.selectedOrderType, it.sortDirection).toImmutableList(),
                    selectedOrderDetails = details?.toUiState(),
                    isPrinting = false,
                    detailsErrorMessage = result.exceptionOrNull()?.message,
                )
            }
        }
    }

    private fun updateSearch(query: String) {
        _state.update {
            it.copy(
                searchQuery = query,
                orders = filteredOrders(query, it.selectedOrderType, it.sortDirection).toImmutableList(),
            )
        }
    }

    private fun selectOrderType(orderType: OrderType?) {
        _state.update {
            it.copy(
                selectedOrderType = orderType,
                orders = filteredOrders(it.searchQuery, orderType, it.sortDirection).toImmutableList(),
            )
        }
    }

    private fun selectSortDirection(sortDirection: OrdersSortDirection) {
        _state.update {
            it.copy(
                sortDirection = sortDirection,
                orders = filteredOrders(it.searchQuery, it.selectedOrderType, sortDirection).toImmutableList(),
            )
        }
    }

    private fun filteredOrders(
        searchQuery: String,
        selectedOrderType: OrderType?,
        sortDirection: OrdersSortDirection,
    ): List<OrderUiState> {
        val normalizedQuery = searchQuery.trim()
        val filtered = allOrders
            .asSequence()
            .filter { normalizedQuery.isBlank() || it.id.contains(normalizedQuery, ignoreCase = true) }
            .filter { selectedOrderType == null || it.orderType == selectedOrderType }
            .map { it.toUiState() }
            .toList()

        return when (sortDirection) {
            OrdersSortDirection.NewestFirst -> filtered.sortedByDescending { it.createdAt }
            OrdersSortDirection.OldestFirst -> filtered.sortedBy { it.createdAt }
        }
    }

    private fun Order.toUiState(): OrderUiState =
        OrderUiState(
            id = id,
            cashierName = cashierName,
            orderType = orderType,
            orderStatus = orderStatus,
            paymentStatus = paymentStatus,
            printStatus = printStatus,
            totalAmount = totalAmount,
            createdAt = createdAt,
        )

    private fun OrderDetails.toUiState(): OrderDetailsUiState =
        OrderDetailsUiState(
            id = order.id,
            cashierName = order.cashierName,
            orderType = order.orderType,
            orderStatus = order.orderStatus,
            paymentStatus = order.paymentStatus,
            printStatus = order.printStatus,
            subtotalAmount = order.subtotalAmount,
            serviceAmount = order.serviceAmount,
            discountAmount = order.discountAmount,
            taxAmount = order.taxAmount,
            totalAmount = order.totalAmount,
            discountPromoCode = order.discountPromoCode,
            createdAt = order.createdAt,
            paidAt = order.paidAt,
            items = items.map { it.toUiState() }.toImmutableList(),
            payments = payments.map { it.toUiState() }.toImmutableList(),
            refunds = refunds.map { it.toUiState() }.toImmutableList(),
        )

    private fun OrderItem.toUiState(): OrderItemUiState =
        OrderItemUiState(
            name = name,
            quantity = quantity,
            unitPrice = unitPrice,
            subtotalAmount = subtotalAmount,
            discountAmount = discountAmount,
            taxAmount = taxAmount,
            totalAmount = totalAmount,
        )

    private fun Payment.toUiState(): OrderPaymentUiState =
        OrderPaymentUiState(
            type = type,
            status = status,
            amount = amount,
            completedAt = completedAt,
        )

    private fun RefundDetails.toUiState(): OrderRefundUiState =
        OrderRefundUiState(
            status = refund.refundStatus,
            type = refund.refundType,
            amount = refund.amount,
            completedAt = refund.completedAt,
        )
}
