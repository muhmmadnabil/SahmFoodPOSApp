package com.sahm.pos.screens.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sahm.pos.domain.entity.Order
import com.sahm.pos.domain.entity.OrderType
import com.sahm.pos.domain.usecase.GetOrdersUseCase
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OrdersViewModel(
    private val getOrdersUseCase: GetOrdersUseCase,
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
            totalAmount = totalAmount,
            createdAt = createdAt,
        )
}
