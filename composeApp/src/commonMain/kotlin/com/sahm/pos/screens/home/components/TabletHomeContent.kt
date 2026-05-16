package com.sahm.pos.screens.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sahm.pos.domain.entity.MenuItem
import com.sahm.pos.domain.entity.PaymentType
import com.sahm.pos.screens.home.HomeIntent
import com.sahm.pos.screens.home.HomeOrderItemUiState
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun TabletHomeContent(
    filteredItems: ImmutableList<MenuItem>,
    orderItems: ImmutableList<HomeOrderItemUiState>,
    paymentTypes: ImmutableList<PaymentType>,
    selectedPaymentType: PaymentType,
    subtotal: Long,
    tax: Long,
    total: Long,
    spacing: Dp,
    modifier: Modifier = Modifier,
    onIntent: (HomeIntent) -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        horizontalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(spacing),
        ) {
            MenuItemsGrid(
                items = filteredItems,
                isTablet = true,
                modifier = Modifier.weight(1f),
                onAddItem = { onIntent(HomeIntent.ItemAdded(it)) },
            )
            PaymentMethodCard(
                paymentTypes = paymentTypes,
                selectedPaymentType = selectedPaymentType,
                showTitle = true,
                onPaymentSelected = { onIntent(HomeIntent.PaymentTypeSelected(it)) },
                onConfirmPayment = { onIntent(HomeIntent.ConfirmPaymentClicked) },
            )
        }
        CurrentOrderPanel(
            isTablet = true,
            orderItems = orderItems,
            subtotal = subtotal,
            tax = tax,
            total = total,
            modifier = Modifier
                .weight(0.34f)
                .fillMaxHeight(),
            onQuantityChanged = { itemId, quantity ->
                onIntent(HomeIntent.ItemQuantityChanged(itemId, quantity))
            },
            onItemRemoved = { onIntent(HomeIntent.ItemRemoved(it)) },
            onMakeOrder = { onIntent(HomeIntent.MakeOrderClicked) },
        )
    }
}