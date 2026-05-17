package com.sahm.pos.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sahm.pos.screens.home.components.CardPaymentDialog
import com.sahm.pos.screens.home.components.CategoryTabs
import com.sahm.pos.screens.home.components.CurrentOrderPanel
import com.sahm.pos.screens.home.components.HomeHeader
import com.sahm.pos.screens.home.components.MenuItemsGrid
import com.sahm.pos.screens.home.components.PaymentPromptDialog
import com.sahm.pos.screens.home.components.TabletHomeContent
import com.sahm.pos.theme.ScreenBackground
import com.sahm.pos.utils.ScreenType

@Composable
fun HomeScreen(
    screenType: ScreenType,
    state: HomeUiState,
    onIntent: (HomeIntent) -> Unit,
) {
    val isTablet = screenType == ScreenType.Tablet
    val spacing = if (isTablet) 20.dp else 12.dp

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ScreenBackground)
                .safeContentPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(spacing),
        ) {
            HomeHeader(
                isTablet = isTablet,
                searchText = state.searchText,
                onSearchChanged = { onIntent(HomeIntent.SearchChanged(it)) },
                onOrdersClick = { onIntent(HomeIntent.OnOrdersClicked) },
                onSettingsClick = { onIntent(HomeIntent.OnSettingsClicked) }
            )

            CategoryTabs(
                categories = state.categories,
                selectedCategory = state.selectedCategory,
                isTablet = isTablet,
                onCategorySelected = { onIntent(HomeIntent.CategorySelected(it)) },
            )

            if (isTablet) {
                TabletHomeContent(
                    filteredItems = state.filteredMenuItems,
                    orderItems = state.orderItems,
                    orderTypes = state.orderTypes,
                    selectedOrderType = state.selectedOrderType,
                    discountText = state.discountText,
                    isApplyingDiscount = state.isApplyingDiscount,
                    isCreatingOrder = state.isCreatingOrder,
                    subtotal = state.subtotal,
                    discount = state.discount,
                    appliedDiscountPercent = state.appliedDiscount?.percent,
                    service = state.service,
                    tax = state.tax,
                    total = state.total,
                    spacing = spacing,
                    modifier = Modifier.weight(1f),
                    onIntent = onIntent,
                )
            } else {
                MenuItemsGrid(
                    items = state.filteredMenuItems,
                    isTablet = false,
                    modifier = Modifier.weight(1f),
                    onAddItem = { onIntent(HomeIntent.ItemAdded(it)) },
                )

                CurrentOrderPanel(
                    isTablet = false,
                    orderItems = state.orderItems,
                    orderTypes = state.orderTypes,
                    selectedOrderType = state.selectedOrderType,
                    discountText = state.discountText,
                    isApplyingDiscount = state.isApplyingDiscount,
                    isCreatingOrder = state.isCreatingOrder,
                    subtotal = state.subtotal,
                    discount = state.discount,
                    appliedDiscountPercent = state.appliedDiscount?.percent,
                    service = state.service,
                    tax = state.tax,
                    total = state.total,
                    onQuantityChanged = { itemId, quantity ->
                        onIntent(HomeIntent.ItemQuantityChanged(itemId, quantity))
                    },
                    onItemRemoved = { onIntent(HomeIntent.ItemRemoved(it)) },
                    onOrderTypeSelected = { onIntent(HomeIntent.OrderTypeSelected(it)) },
                    onDiscountChanged = { onIntent(HomeIntent.DiscountChanged(it)) },
                    onDiscountSubmitted = { onIntent(HomeIntent.DiscountSubmitted) },
                    onMakeOrder = { onIntent(HomeIntent.MakeOrderClicked) },
                )
            }
        }

        if (state.showPaymentPrompt) {
            PaymentPromptDialog(
                modifier = Modifier.align(Alignment.Center),
                paymentTypes = state.paymentTypes,
                selectedPaymentType = state.selectedPaymentType,
                onPaymentSelected = { onIntent(HomeIntent.PaymentTypeSelected(it)) },
                onConfirmPayment = { onIntent(HomeIntent.ConfirmPaymentClicked) },
                onDismiss = { onIntent(HomeIntent.PaymentPromptDismissed) },
            )
        }

        if (state.isCardPaymentSheetVisible) {
            CardPaymentDialog(
                cardNumber = state.cardNumber,
                expiryMonth = state.expiryMonth,
                expiryYear = state.expiryYear,
                cvv = state.cvv,
                cardHolderName = state.cardHolderName,
                total = state.total,
                isProcessing = state.isPaymentProcessing,
                onCardNumberChanged = { onIntent(HomeIntent.CardNumberChanged(it)) },
                onExpiryMonthChanged = { onIntent(HomeIntent.ExpiryMonthChanged(it)) },
                onExpiryYearChanged = { onIntent(HomeIntent.ExpiryYearChanged(it)) },
                onCvvChanged = { onIntent(HomeIntent.CvvChanged(it)) },
                onCardHolderNameChanged = { onIntent(HomeIntent.CardHolderNameChanged(it)) },
                onPay = { onIntent(HomeIntent.PayByCardClicked) },
                onDismiss = { onIntent(HomeIntent.CardPaymentDismissed) },
            )
        }
    }
}