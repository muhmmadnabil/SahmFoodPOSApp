package com.sahm.pos.screens.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sahm.pos.screens.home.HomeOrderItemUiState
import com.sahm.pos.theme.TextPrimary

@Composable
fun OrderItemDetails(
    orderItem: HomeOrderItemUiState,
    isTablet: Boolean,
    modifier: Modifier,
    onQuantityChanged: (String, Int) -> Unit,
) {
    val item = orderItem.item

    if (isTablet) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OrderItemName(name = item.name, fontSize = 14)
            QuantityStepper(
                itemId = item.id,
                quantity = orderItem.quantity,
                isTablet = true,
                onQuantityChanged = onQuantityChanged,
            )
        }
    } else {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OrderItemName(
                name = item.name,
                fontSize = 15,
                modifier = Modifier.weight(0.58f),
            )
            QuantityStepper(
                itemId = item.id,
                quantity = orderItem.quantity,
                isTablet = false,
                modifier = Modifier.weight(0.42f),
                onQuantityChanged = onQuantityChanged,
            )
        }
    }
}

@Composable
private fun OrderItemName(
    name: String,
    fontSize: Int,
    modifier: Modifier = Modifier,
) {
    Text(
        text = name,
        modifier = modifier,
        color = TextPrimary,
        fontSize = fontSize.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}