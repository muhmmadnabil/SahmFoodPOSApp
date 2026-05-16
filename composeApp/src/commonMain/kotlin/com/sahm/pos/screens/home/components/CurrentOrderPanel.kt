package com.sahm.pos.screens.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sahm.pos.screens.home.HomeOrderItemUiState
import com.sahm.pos.theme.BorderDefault
import com.sahm.pos.theme.CardBackground
import com.sahm.pos.theme.PrimaryOrange
import com.sahm.pos.theme.TextPrimary
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.stringResource
import sahmfoodposapp.composeapp.generated.resources.Res
import sahmfoodposapp.composeapp.generated.resources.home_current_order
import sahmfoodposapp.composeapp.generated.resources.home_make_order
import sahmfoodposapp.composeapp.generated.resources.home_order_items_count

@Composable
internal fun CurrentOrderPanel(
    isTablet: Boolean,
    orderItems: ImmutableList<HomeOrderItemUiState>,
    subtotal: Long,
    tax: Long,
    total: Long,
    modifier: Modifier = Modifier,
    paymentContent: (@Composable () -> Unit)? = null,
    onQuantityChanged: (String, Int) -> Unit,
    onItemRemoved: (String) -> Unit,
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
            paymentContent?.invoke()
            OrderTotals(
                subtotal = subtotal,
                tax = tax,
                total = total,
                isTablet = isTablet,
            )
            MakeOrderButton(
                isTablet = isTablet,
                onClick = onMakeOrder,
            )
        }
    }
}