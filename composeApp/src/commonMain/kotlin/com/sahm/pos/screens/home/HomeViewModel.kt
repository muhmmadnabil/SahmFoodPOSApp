package com.sahm.pos.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sahm.pos.domain.entity.CardPaymentRequest
import com.sahm.pos.domain.entity.CreateOrderRequest
import com.sahm.pos.domain.entity.Discount
import com.sahm.pos.domain.entity.MenuItem
import com.sahm.pos.domain.entity.OrderType
import com.sahm.pos.domain.entity.PaymentType
import com.sahm.pos.domain.results.ApplyDiscountResult
import com.sahm.pos.domain.results.CreateOrderResult
import com.sahm.pos.domain.usecase.ApplyDiscountUseCase
import com.sahm.pos.domain.usecase.CreateOrderUseCase
import com.sahm.pos.domain.usecase.GetMenuItemsUseCase
import com.sahm.pos.domain.usecase.PayOrderByCardUseCase
import com.sahm.pos.domain.usecase.PayOrderByCashUseCase
import com.sahm.pos.domain.usecase.RetryPrintOrderReceiptUseCase
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sahmfoodposapp.composeapp.generated.resources.Res
import sahmfoodposapp.composeapp.generated.resources.error_apply_discount_before_adding_items
import kotlin.math.roundToLong

class HomeViewModel(
    private val getMenuItemsUseCase: GetMenuItemsUseCase,
    private val applyDiscountUseCase: ApplyDiscountUseCase,
    private val createOrderUseCase: CreateOrderUseCase,
    private val payOrderByCashUseCase: PayOrderByCashUseCase,
    private val payOrderByCardUseCase: PayOrderByCardUseCase,
    private val retryPrintOrderReceiptUseCase: RetryPrintOrderReceiptUseCase,
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
            is HomeIntent.OrderTypeSelected -> selectOrderType(intent.orderType)
            is HomeIntent.DiscountChanged -> updateDiscount(intent.discount)
            HomeIntent.DiscountSubmitted -> applyDiscount()
            is HomeIntent.PaymentTypeSelected -> selectPaymentType(intent.paymentType)
            HomeIntent.MakeOrderClicked -> showPaymentPrompt()
            HomeIntent.ConfirmPaymentClicked -> confirmPayment()
            HomeIntent.PaymentPromptDismissed -> dismissPaymentPrompt()
            is HomeIntent.CardNumberChanged -> updateCardNumber(intent.value)
            is HomeIntent.ExpiryMonthChanged -> updateExpiryMonth(intent.value)
            is HomeIntent.ExpiryYearChanged -> updateExpiryYear(intent.value)
            is HomeIntent.CvvChanged -> updateCvv(intent.value)
            is HomeIntent.CardHolderNameChanged -> updateCardHolderName(intent.value)
            HomeIntent.PayByCardClicked -> payByCard()
            HomeIntent.CardPaymentDismissed -> dismissCardPayment()
            HomeIntent.RetryPrintClicked -> retryPrint()
            is HomeIntent.RefundItemQuantityChanged -> updateRefundItemQuantity(
                intent.orderItemId,
                intent.quantity
            )

            HomeIntent.ConfirmRefundClicked -> confirmRefund()
            HomeIntent.OnOrdersClicked -> navigateToOrders()
            HomeIntent.OnSettingsClicked -> navigateToSettings()
        }
    }

    private fun navigateToOrders() {
        viewModelScope.launch {
            _effect.emit(HomeEffect.NavigateToOrders)
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

            val updatedOrderItems = buildOrderItems(state.menuItems, orderItems)
            val isSameDraft = updatedOrderItems.sameQuantitiesAs(state.orderItems)
            state.copy(
                orderItems = updatedOrderItems.toImmutableList(),
                createdOrderId = if (isSameDraft) state.createdOrderId else null,
                selectedPaymentMethod = if (isSameDraft) state.selectedPaymentMethod else null,
            ).recalculate()
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

    private fun selectOrderType(orderType: OrderType) {
        _state.update { state ->
            if (orderType !in state.orderTypes || orderType == state.selectedOrderType) {
                state
            } else {
                state.copy(
                    selectedOrderType = orderType,
                    createdOrderId = null,
                    selectedPaymentMethod = null,
                ).recalculate()
            }
        }
    }

    private fun updateDiscount(discountText: String) {
        _state.update { state ->
            state.copy(discountText = discountText)
        }
    }

    private fun applyDiscount() {
        viewModelScope.launch {
            val state = _state.value

            if (state.discountText.isBlank()) {
                _state.update {
                    it.copy(
                        isApplyingDiscount = false,
                        appliedDiscount = null,
                        createdOrderId = if (it.appliedDiscount == null) it.createdOrderId else null,
                        selectedPaymentMethod = if (it.appliedDiscount == null) it.selectedPaymentMethod else null,
                    ).recalculate()
                }
                return@launch
            }

            if (state.orderItems.isEmpty()) {
                _state.update {
                    it.copy(
                        isApplyingDiscount = false,
                        appliedDiscount = null,
                    ).recalculate()
                }

                _effect.emit(HomeEffect.ShowMessage(Res.string.error_apply_discount_before_adding_items))

                return@launch
            }

            _state.update { it.copy(isApplyingDiscount = true) }

            val result = applyDiscountUseCase(
                promoCode = state.discountText,
                orderTotal = state.subtotal.toMajorAmount(),
            )

            _state.update { currentState ->
                if (currentState.discountText.trim() != state.discountText.trim()) {
                    return@update currentState.copy(isApplyingDiscount = false)
                }

                when (result) {
                    is ApplyDiscountResult.Success -> currentState
                        .copy(
                            discountText = "",
                            isApplyingDiscount = false,
                            appliedDiscount = result.discount,
                            createdOrderId = null,
                            selectedPaymentMethod = null,
                        )
                        .recalculate()

                    ApplyDiscountResult.InvalidDiscountConfiguration,
                    ApplyDiscountResult.PromoCodeExpired,
                    ApplyDiscountResult.PromoCodeNotFound,
                    ApplyDiscountResult.PromoCodeNotStartedYet,
                        -> currentState
                        .copy(
                            isApplyingDiscount = false,
                            appliedDiscount = null,
                            createdOrderId = if (currentState.appliedDiscount == null) {
                                currentState.createdOrderId
                            } else {
                                null
                            },
                            selectedPaymentMethod = if (currentState.appliedDiscount == null) {
                                currentState.selectedPaymentMethod
                            } else {
                                null
                            },
                        )
                        .recalculate()
                }
            }
        }
    }

    private fun showPaymentPrompt() {
        viewModelScope.launch {
            val state = _state.value
            if (state.orderItems.isEmpty() || state.isCreatingOrder) return@launch
            if (state.createdOrderId != null) {
                _state.update {
                    it.copy(
                        showPaymentPrompt = true,
                        errorMessage = null,
                    )
                }
                return@launch
            }

            _state.update { it.copy(isCreatingOrder = true, errorMessage = null) }
            val result = createOrderUseCase(
                CreateOrderRequest(
                    items = state.orderItems.map {
                        CreateOrderRequest.Item(it.item, it.quantity)
                    },
                    promoCode = state.appliedDiscount?.promoCode,
                    orderType = state.selectedOrderType,
                )
            )
            _state.update { current ->
                when (result) {
                    is CreateOrderResult.Success -> current.copy(
                        createdOrderId = result.orderId,
                        isCreatingOrder = false,
                        showPaymentPrompt = true,
                    )

                    else -> current.copy(
                        isCreatingOrder = false,
                        errorMessage = result.toMessage(),
                    )
                }
            }
        }
    }

    private fun dismissPaymentPrompt() {
        _state.update { it.copy(showPaymentPrompt = false) }
    }

    private fun confirmPayment() {
        val state = _state.value
        when (state.selectedPaymentType) {
            PaymentType.CASH -> payByCash()
            PaymentType.CARD -> _state.update {
                it.copy(
                    selectedPaymentMethod = PaymentType.CARD,
                    showPaymentPrompt = false,
                    isCardPaymentSheetVisible = true,
                    errorMessage = null,
                )
            }
        }
    }

    private fun payByCash() {
        viewModelScope.launch {
            val orderId = _state.value.createdOrderId ?: return@launch
            _state.update {
                it.copy(
                    selectedPaymentMethod = PaymentType.CASH,
                    isPaymentProcessing = true,
                    showPaymentPrompt = false,
                    errorMessage = null,
                )
            }

            val result=payOrderByCashUseCase(orderId)
            _state.update { current ->
                if (result.isSuccess) {
                    current.copy(
                        orderItems = emptyList<HomeOrderItemUiState>().toImmutableList(),
                        discountText = "",
                        isApplyingDiscount = false,
                        appliedDiscount = null,
                        createdOrderId = null,
                        selectedPaymentMethod = null,
                        isPaymentProcessing = false,
                    ).recalculate()
                } else {
                    current.copy(
                        isPaymentProcessing = false,
                        errorMessage = result.exceptionOrNull()?.message,
                    )
                }
            }
        }
    }

    private fun payByCard() {
        viewModelScope.launch {
            val state = _state.value
            val orderId = state.createdOrderId ?: return@launch
            validateCardFields(state)?.let { message ->
                _state.update { it.copy(errorMessage = message) }
                return@launch
            }
            val payCard = payOrderByCardUseCase ?: return@launch
            _state.update {
                it.copy(
                    isPaymentProcessing = true,
                    errorMessage = null
                )
            }
            val result = payCard(
                CardPaymentRequest(
                    orderId = orderId,
                    amount = state.total.toInt(),
                    cardNumber = state.cardNumber,
                    expiryMonth = state.expiryMonth,
                    expiryYear = state.expiryYear,
                    cvv = state.cvv,
                    cardHolderName = state.cardHolderName,
                )
            )
            _state.update { current ->
                if (result.isSuccess) {
                    current.copy(
                        orderItems = emptyList<HomeOrderItemUiState>().toImmutableList(),
                        discountText = "",
                        isApplyingDiscount = false,
                        appliedDiscount = null,
                        createdOrderId = null,
                        selectedPaymentMethod = null,
                        isPaymentProcessing = false,
                        isCardPaymentSheetVisible = false,
                        cardNumber = "",
                        expiryMonth = "",
                        expiryYear = "",
                        cvv = "",
                        cardHolderName = "",
                    ).recalculate()
                } else {
                    current.copy(
                        isPaymentProcessing = false,
                        isCardPaymentSheetVisible = true,
                        errorMessage = result.exceptionOrNull()?.message,
                    )
                }
            }
        }
    }

    private fun retryPrint() {
        viewModelScope.launch {
            val orderId = _state.value.createdOrderId ?: return@launch
            val retry = retryPrintOrderReceiptUseCase ?: return@launch
            _state.update { it.copy( errorMessage = null) }
            val result = retry(orderId)
            _state.update {
                it.copy(
                    errorMessage = result.exceptionOrNull()?.message,
                )
            }
        }
    }

    private fun dismissCardPayment() {
        _state.update { it.copy(isCardPaymentSheetVisible = false) }
    }

    private fun updateCardNumber(value: String) {
        _state.update { it.copy(cardNumber = value) }
    }

    private fun updateExpiryMonth(value: String) {
        _state.update { it.copy(expiryMonth = value) }
    }

    private fun updateExpiryYear(value: String) {
        _state.update { it.copy(expiryYear = value) }
    }

    private fun updateCvv(value: String) {
        _state.update { it.copy(cvv = value) }
    }

    private fun updateCardHolderName(value: String) {
        _state.update { it.copy(cardHolderName = value) }
    }

    private fun updateRefundItemQuantity(orderItemId: String, quantity: Int) {
        _state.update { state ->
            state.copy(
                selectedRefundItems = state.selectedRefundItems + (orderItemId to quantity.coerceAtLeast(
                    0
                ))
            )
        }
    }

    private fun confirmRefund() {
        _state.update { it.copy(isRefundProcessing = false) }
    }

    private fun clearDraftAfterPayment() {
        _state.update { state ->
            state.copy(
                orderItems = emptyList<HomeOrderItemUiState>().toImmutableList(),
                discountText = "",
                isApplyingDiscount = false,
                appliedDiscount = null,
                showPaymentPrompt = false,
                isCardPaymentSheetVisible = false,
                createdOrderId = null,
                selectedPaymentMethod = null,
            ).recalculate()
        }
    }

    private fun validateCardFields(state: HomeUiState): String? {
        val number = state.cardNumber.replace(" ", "")
        if (number.isBlank() || number.any { !it.isDigit() }) return "Invalid card number"
        if (state.cvv.length !in 3..4 || state.cvv.any { !it.isDigit() }) return "Invalid CVV"
        val month = state.expiryMonth.toIntOrNull() ?: return "Invalid expiry"
        if (month !in 1..12) return "Invalid expiry"
        val year = state.expiryYear.toIntOrNull() ?: return "Invalid expiry"
        if (year < 2026) return "Invalid expiry"
        if (state.cardHolderName.isBlank()) return "Card holder name is required"
        return null
    }

    private fun CreateOrderResult.toMessage(): String =
        when (this) {
            CreateOrderResult.CashierMissing -> "Cashier not found"
            CreateOrderResult.DiscountExpired -> "Discount expired"
            CreateOrderResult.DiscountMinValueNotReached -> "Discount minimum value not reached"
            CreateOrderResult.DiscountNotFound -> "Discount not found"
            CreateOrderResult.DiscountNotStartedYet -> "Discount has not started yet"
            CreateOrderResult.EmptyCart -> "Cart is empty"
            is CreateOrderResult.Failed -> message
            CreateOrderResult.InvalidQuantity -> "Invalid item quantity"
            CreateOrderResult.MenuItemInactive -> "Menu item is inactive"
            CreateOrderResult.MenuItemNotFound -> "Menu item not found"
            is CreateOrderResult.Success -> ""
        }

    private fun HomeUiState.recalculate(): HomeUiState {
        val filteredItems = menuItems.filter { item ->
            (selectedCategory == HomeConstants.AllCategory || item.category == selectedCategory) &&
                    item.name.contains(searchText, ignoreCase = true)
        }
        val orderItemsById = orderItems.associate { it.item.id to it.quantity }
        val recalculatedOrderItems = buildOrderItems(menuItems, orderItemsById)
        val subtotal = recalculatedOrderItems.sumOf { it.lineTotal }
        val discountAmount = appliedDiscount
            ?.let { calculateAppliedDiscountAmount(subtotal, it) }
            ?: 0
        val service = calculateService(subtotal - discountAmount, selectedOrderType)
        val taxableAmount = subtotal + service - discountAmount
        val tax = calculatePercent(taxableAmount, HomeConstants.TaxPercent)

        return copy(
            filteredMenuItems = filteredItems.toImmutableList(),
            orderItems = recalculatedOrderItems.toImmutableList(),
            subtotal = subtotal,
            discount = discountAmount,
            service = service,
            tax = tax,
            total = taxableAmount + tax,
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

    private fun List<HomeOrderItemUiState>.sameQuantitiesAs(other: List<HomeOrderItemUiState>): Boolean =
        size == other.size &&
                associate { it.item.id to it.quantity } == other.associate { it.item.id to it.quantity }

    private fun calculateService(amount: Long, orderType: OrderType): Long =
        if (orderType == OrderType.DINE_IN) {
            calculatePercent(amount, HomeConstants.ServicePercent)
        } else {
            0
        }

    private fun calculatePercent(amount: Long, percent: Int): Long =
        (amount * percent + 50) / 100

    private fun calculateAppliedDiscountAmount(
        subtotal: Long,
        discount: Discount,
    ): Long {
        val rawDiscount = (subtotal * discount.percent / 100.0).roundToLong()
        val minDiscount = discount.minValue.toCents()
        val maxDiscount = discount.maxValue.toCents()
        return minOf(maxOf(rawDiscount, minDiscount), maxDiscount, subtotal)
    }

    private fun Double.toCents(): Long = (this * 100.0).roundToLong()

    private fun Long.toMajorAmount(): Double = this / 100.0
}
