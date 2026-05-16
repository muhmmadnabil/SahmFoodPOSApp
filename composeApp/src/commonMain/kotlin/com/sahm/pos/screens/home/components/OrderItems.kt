package com.sahm.pos.screens.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sahm.pos.screens.home.HomeOrderItemUiState
import kotlinx.collections.immutable.ImmutableList

@Composable
fun OrderItems(
    orderItems: ImmutableList<HomeOrderItemUiState>,
    isTablet: Boolean,
    modifier: Modifier = Modifier,
    onQuantityChanged: (String, Int) -> Unit,
    onItemRemoved: (String) -> Unit,
) {
    if (isTablet) {
        LazyColumn(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(orderItems, key = { it.item.id }) { orderItem ->
                OrderItemRow(
                    orderItem = orderItem,
                    isTablet = true,
                    onQuantityChanged = onQuantityChanged,
                    onItemRemoved = onItemRemoved,
                )
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            orderItems.forEach { orderItem ->
                OrderItemRow(
                    orderItem = orderItem,
                    isTablet = false,
                    onQuantityChanged = onQuantityChanged,
                    onItemRemoved = onItemRemoved,
                )
            }
        }
    }
}