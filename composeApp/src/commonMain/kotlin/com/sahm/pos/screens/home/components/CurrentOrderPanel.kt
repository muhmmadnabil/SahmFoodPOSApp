package com.sahm.pos.screens.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sahm.pos.domain.entity.OrderType
import com.sahm.pos.screens.home.HomeOrderItemUiState
import com.sahm.pos.theme.BorderDefault
import com.sahm.pos.theme.CardBackground
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun CurrentOrderPanel(
    isTablet: Boolean,
    orderItems: ImmutableList<HomeOrderItemUiState>,
    orderTypes: ImmutableList<OrderType>,
    selectedOrderType: OrderType,
    discountText: String,
    isApplyingDiscount: Boolean,
    isCreatingOrder: Boolean,
    subtotal: Long,
    discount: Long,
    appliedDiscountPercent: Double?,
    service: Long,
    tax: Long,
    total: Long,
    modifier: Modifier = Modifier,
    onQuantityChanged: (String, Int) -> Unit,
    onItemRemoved: (String) -> Unit,
    onOrderTypeSelected: (OrderType) -> Unit,
    onDiscountChanged: (String) -> Unit,
    onDiscountSubmitted: () -> Unit,
    onMakeOrder: () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = if (isTablet) {
            RoundedCornerShape(14.dp)
        } else {
            RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        },
        color = CardBackground,
        border = BorderStroke(1.dp, BorderDefault),
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = if (isTablet) 18.dp else 14.dp,
                vertical = if (isTablet) 18.dp else 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(if (isTablet) 16.dp else 12.dp),
        ) {
            CurrentOrderHeader(
                itemCount = orderItems.size,
                showItemCount = !isTablet && orderItems.isNotEmpty(),
            )
            OrderItems(
                orderItems = orderItems,
                isTablet = isTablet,
                modifier = if (isTablet) Modifier.weight(1f) else Modifier,
                onQuantityChanged = onQuantityChanged,
                onItemRemoved = onItemRemoved,
            )
            OrderDetailsControls(
                orderTypes = orderTypes,
                selectedOrderType = selectedOrderType,
                discountText = discountText,
                isApplyingDiscount = isApplyingDiscount,
                isTablet = isTablet,
                onOrderTypeSelected = onOrderTypeSelected,
                onDiscountChanged = onDiscountChanged,
                onDiscountSubmitted = onDiscountSubmitted,
            )
            OrderTotals(
                subtotal = subtotal,
                discount = discount,
                appliedDiscountPercent = appliedDiscountPercent,
                service = service,
                showService = selectedOrderType == OrderType.DINE_IN,
                tax = tax,
                total = total,
                isTablet = isTablet,
            )
            MakeOrderButton(
                isTablet = isTablet,
                enabled = orderItems.isNotEmpty() && !isCreatingOrder,
                onClick = onMakeOrder,
            )
        }
    }
}
