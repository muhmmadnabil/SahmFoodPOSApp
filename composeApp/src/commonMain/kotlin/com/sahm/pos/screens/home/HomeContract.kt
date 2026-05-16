package com.sahm.pos.screens.home

import androidx.compose.runtime.Stable
import com.sahm.pos.domain.entity.Discount
import com.sahm.pos.domain.entity.MenuItem
import com.sahm.pos.domain.entity.OrderType
import com.sahm.pos.domain.entity.PaymentType
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.StringResource

data class HomeUiState(
    val menuItems: ImmutableList<MenuItem> = persistentListOf(),
    val categories: ImmutableList<String> = persistentListOf(HomeConstants.AllCategory),
    val selectedCategory: String = HomeConstants.AllCategory,
    val searchText: String = "",
    val filteredMenuItems: ImmutableList<MenuItem> = persistentListOf(),
    val orderItems: ImmutableList<HomeOrderItemUiState> = persistentListOf(),
    val paymentTypes: ImmutableList<PaymentType> = persistentListOf(
        PaymentType.CASH,
        PaymentType.CARD,
    ),
    val selectedPaymentType: PaymentType = PaymentType.CASH,
    val orderTypes: ImmutableList<OrderType> = persistentListOf(
        OrderType.DINE_IN,
        OrderType.TAKEAWAY,
        OrderType.DELIVERY,
    ),
    val selectedOrderType: OrderType = OrderType.TAKEAWAY,
    val discountText: String = "",
    val isApplyingDiscount: Boolean = false,
    val appliedDiscount: Discount? = null,
    val subtotal: Long = 0,
    val discount: Long = 0,
    val service: Long = 0,
    val tax: Long = 0,
    val total: Long = 0,
    val showPaymentPrompt: Boolean = false,
)

@Stable
data class HomeOrderItemUiState(
    val item: MenuItem,
    val quantity: Int,
    val lineTotal: Long,
)

sealed interface HomeIntent {
    data object ScreenOpened : HomeIntent
    data class CategorySelected(val category: String) : HomeIntent
    data class SearchChanged(val query: String) : HomeIntent
    data class ItemAdded(val itemId: String) : HomeIntent
    data class ItemQuantityChanged(val itemId: String, val quantity: Int) : HomeIntent
    data class ItemRemoved(val itemId: String) : HomeIntent
    data class OrderTypeSelected(val orderType: OrderType) : HomeIntent
    data class DiscountChanged(val discount: String) : HomeIntent
    data object DiscountSubmitted : HomeIntent
    data class PaymentTypeSelected(val paymentType: PaymentType) : HomeIntent
    data object MakeOrderClicked : HomeIntent
    data object ConfirmPaymentClicked : HomeIntent
    data object PaymentPromptDismissed : HomeIntent
    data object OnSettingsClicked : HomeIntent
}

sealed interface HomeEffect {
    data object NavigateToSettings : HomeEffect
    data class ShowMessage(val message: StringResource) : HomeEffect
}

object HomeConstants {
    const val AllCategory = "__all__"
    const val TaxPercent = 14
    const val ServicePercent = 10
}
