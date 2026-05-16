package com.sahm.pos.domain.results

sealed interface CreateOrderResult {
    data class Success(val orderId: String) : CreateOrderResult
    data object EmptyCart : CreateOrderResult
    data object InvalidQuantity : CreateOrderResult
    data object CashierMissing : CreateOrderResult
    data object MenuItemNotFound : CreateOrderResult
    data object MenuItemInactive : CreateOrderResult
    data object DiscountNotFound : CreateOrderResult
    data object DiscountExpired : CreateOrderResult
    data object DiscountNotStartedYet : CreateOrderResult
    data object DiscountMinValueNotReached : CreateOrderResult
    data class Failed(val message: String) : CreateOrderResult
}