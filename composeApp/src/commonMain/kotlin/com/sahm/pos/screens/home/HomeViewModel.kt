package com.sahm.pos.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sahm.pos.domain.entity.MenuItem
import com.sahm.pos.domain.entity.PaymentType
import com.sahm.pos.domain.usecase.GetMenuItemsUseCase
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val getMenuItemsUseCase: GetMenuItemsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<HomeEffect>()
    val effect: SharedFlow<HomeEffect> = _effect.asSharedFlow()

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.ScreenOpened -> loadMenuItems()
            is HomeIntent.CategorySelected -> selectCategory(intent.category)
            is HomeIntent.SearchChanged -> updateSearch(intent.query)
            is HomeIntent.ItemAdded -> addItem(intent.itemId)
            is HomeIntent.ItemQuantityChanged -> updateQuantity(intent.itemId, intent.quantity)
            is HomeIntent.ItemRemoved -> updateQuantity(intent.itemId, 0)
            is HomeIntent.PaymentTypeSelected -> selectPaymentType(intent.paymentType)
            HomeIntent.MakeOrderClicked -> Unit
            HomeIntent.ConfirmPaymentClicked -> Unit
            HomeIntent.OnSettingsClicked -> navigateToSettings()
        }
    }

    private fun navigateToSettings() {
        viewModelScope.launch {
            _effect.emit(HomeEffect.NavigateToSettings)
        }
    }

    private fun loadMenuItems() {
        viewModelScope.launch {
            val menuItems = getMenuItemsUseCase()
            _state.update { currentState ->
                val categories = buildCategories(menuItems)
                val selectedCategory = currentState.selectedCategory
                    .takeIf { it in categories }
                    ?: HomeConstants.AllCategory

                currentState
                    .copy(
                        menuItems = menuItems.toImmutableList(),
                        categories = categories.toImmutableList(),
                        selectedCategory = selectedCategory,
                    )
                    .recalculate()
            }
        }
    }

    private fun selectCategory(category: String) {
        _state.update { state ->
            val selectedCategory = category.takeIf { it in state.categories }
                ?: HomeConstants.AllCategory

            state.copy(selectedCategory = selectedCategory).recalculate()
        }
    }

    private fun updateSearch(query: String) {
        _state.update { it.copy(searchText = query).recalculate() }
    }

    private fun addItem(itemId: String) {
        val currentQuantity = _state.value.orderItems
            .firstOrNull { it.item.id == itemId }
            ?.quantity
            ?: 0

        updateQuantity(itemId = itemId, quantity = currentQuantity + 1)
    }

    private fun updateQuantity(itemId: String, quantity: Int) {
        _state.update { state ->
            if (state.menuItems.none { it.id == itemId }) return@update state

            val orderItems = state.orderItems
                .associate { it.item.id to it.quantity }
                .toMutableMap()

            if (quantity <= 0) {
                orderItems.remove(itemId)
            } else {
                orderItems[itemId] = quantity
            }

            state.copy(orderItems = buildOrderItems(state.menuItems, orderItems).toImmutableList())
                .recalculate()
        }
    }

    private fun selectPaymentType(paymentType: PaymentType) {
        _state.update { state ->
            if (paymentType !in state.paymentTypes) {
                state
            } else {
                state.copy(selectedPaymentType = paymentType)
            }
        }
    }

    private fun HomeUiState.recalculate(): HomeUiState {
        val filteredItems = menuItems.filter { item ->
            (selectedCategory == HomeConstants.AllCategory || item.category == selectedCategory) &&
                    item.name.contains(searchText, ignoreCase = true)
        }
        val orderItemsById = orderItems.associate { it.item.id to it.quantity }
        val recalculatedOrderItems = buildOrderItems(menuItems, orderItemsById)
        val subtotal = recalculatedOrderItems.sumOf { it.lineTotal }
        val tax = calculateTax(subtotal)

        return copy(
            filteredMenuItems = filteredItems.toImmutableList(),
            orderItems = recalculatedOrderItems.toImmutableList(),
            subtotal = subtotal,
            tax = tax,
            total = subtotal + tax,
        )
    }

    private fun buildCategories(menuItems: List<MenuItem>): List<String> =
        listOf(HomeConstants.AllCategory) + menuItems
            .map { it.category }
            .distinct()

    private fun buildOrderItems(
        menuItems: List<MenuItem>,
        quantitiesById: Map<String, Int>,
    ): List<HomeOrderItemUiState> =
        menuItems.mapNotNull { item ->
            val quantity = quantitiesById[item.id]?.takeIf { it > 0 } ?: return@mapNotNull null
            HomeOrderItemUiState(
                item = item,
                quantity = quantity,
                lineTotal = item.price * quantity,
            )
        }

    private fun calculateTax(subtotal: Long): Long =
        (subtotal * HomeConstants.TaxPercent + 50) / 100
}