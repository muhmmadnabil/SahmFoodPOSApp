package com.sahm.pos.screens.orders.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sahm.pos.components.PosIcons
import com.sahm.pos.domain.entity.OrderType
import com.sahm.pos.screens.orders.OrdersSortDirection
import com.sahm.pos.theme.BorderDefault
import com.sahm.pos.theme.CardBackground
import com.sahm.pos.theme.TextPrimary
import com.sahm.pos.theme.TextSecondary
import org.jetbrains.compose.resources.stringResource
import sahmfoodposapp.composeapp.generated.resources.Res
import sahmfoodposapp.composeapp.generated.resources.home_order_type_delivery
import sahmfoodposapp.composeapp.generated.resources.home_order_type_dine_in
import sahmfoodposapp.composeapp.generated.resources.home_order_type_takeaway
import sahmfoodposapp.composeapp.generated.resources.orders_all_filter
import sahmfoodposapp.composeapp.generated.resources.orders_clear_search
import sahmfoodposapp.composeapp.generated.resources.orders_newest_first
import sahmfoodposapp.composeapp.generated.resources.orders_oldest_first
import sahmfoodposapp.composeapp.generated.resources.orders_search_placeholder

@Composable
fun OrdersControls(
    searchQuery: String,
    selectedOrderType: OrderType?,
    sortDirection: OrdersSortDirection,
    onSearchChanged: (String) -> Unit,
    onOrderTypeSelected: (OrderType?) -> Unit,
    onSortDirectionSelected: (OrdersSortDirection) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OrdersSearchField(
            searchQuery = searchQuery,
            onSearchChanged = onSearchChanged,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                text = stringResource(Res.string.orders_all_filter),
                isSelected = selectedOrderType == null,
                onClick = { onOrderTypeSelected(null) },
            )
            OrderType.entries.forEach { orderType ->
                FilterChip(
                    text = orderTypeLabel(orderType),
                    isSelected = selectedOrderType == orderType,
                    onClick = { onOrderTypeSelected(orderType) },
                )
            }
            Spacer(Modifier.width(8.dp))
            SortChip(
                sortDirection = OrdersSortDirection.NewestFirst,
                currentSortDirection = sortDirection,
                onClick = { onSortDirectionSelected(OrdersSortDirection.NewestFirst) },
            )
            SortChip(
                sortDirection = OrdersSortDirection.OldestFirst,
                currentSortDirection = sortDirection,
                onClick = { onSortDirectionSelected(OrdersSortDirection.OldestFirst) },
            )
        }
    }
}

@Composable
private fun OrdersSearchField(
    searchQuery: String,
    onSearchChanged: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = CardBackground,
        border = BorderStroke(1.dp, BorderDefault),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = PosIcons.Search,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(20.dp),
            )
            BasicTextField(
                value = searchQuery,
                onValueChange = onSearchChanged,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = TextStyle(
                    color = TextPrimary,
                    fontSize = 15.sp,
                ),
                decorationBox = { innerTextField ->
                    if (searchQuery.isBlank()) {
                        Text(
                            text = stringResource(Res.string.orders_search_placeholder),
                            color = TextSecondary,
                            fontSize = 15.sp,
                        )
                    }
                    innerTextField()
                },
            )
            if (searchQuery.isNotEmpty()) {
                IconButton(
                    onClick = { onSearchChanged("") },
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        imageVector = PosIcons.Close,
                        contentDescription = stringResource(Res.string.orders_clear_search),
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SortChip(
    sortDirection: OrdersSortDirection,
    currentSortDirection: OrdersSortDirection,
    onClick: () -> Unit,
) {
    val label = when (sortDirection) {
        OrdersSortDirection.NewestFirst -> stringResource(Res.string.orders_newest_first)
        OrdersSortDirection.OldestFirst -> stringResource(Res.string.orders_oldest_first)
    }
    FilterChip(
        text = label,
        isSelected = sortDirection == currentSortDirection,
        onClick = onClick,
    )
}

@Composable
private fun orderTypeLabel(orderType: OrderType): String =
    when (orderType) {
        OrderType.DINE_IN -> stringResource(Res.string.home_order_type_dine_in)
        OrderType.TAKEAWAY -> stringResource(Res.string.home_order_type_takeaway)
        OrderType.DELIVERY -> stringResource(Res.string.home_order_type_delivery)
    }
