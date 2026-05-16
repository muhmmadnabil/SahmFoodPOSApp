package com.sahm.pos.screens.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sahm.pos.screens.orders.components.OrderRow
import com.sahm.pos.screens.orders.components.OrdersControls
import com.sahm.pos.theme.PrimaryOrange
import com.sahm.pos.theme.ScreenBackground
import com.sahm.pos.theme.TextSecondary
import com.sahm.pos.utils.ScreenType
import org.jetbrains.compose.resources.stringResource
import sahmfoodposapp.composeapp.generated.resources.Res
import sahmfoodposapp.composeapp.generated.resources.orders_empty

@Composable
fun OrdersScreen(
    screenType: ScreenType,
    state: OrdersUiState,
    onIntent: (OrdersIntent) -> Unit,
) {
    val horizontalPadding = if (screenType == ScreenType.Phone) 20.dp else 40.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .padding(horizontal = horizontalPadding, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        OrdersControls(
            searchQuery = state.searchQuery,
            selectedOrderType = state.selectedOrderType,
            sortDirection = state.sortDirection,
            onSearchChanged = { onIntent(OrdersIntent.SearchChanged(it)) },
            onOrderTypeSelected = { onIntent(OrdersIntent.OrderTypeSelected(it)) },
            onSortDirectionSelected = { onIntent(OrdersIntent.SortDirectionSelected(it)) },
        )

        when {
            state.isLoading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = PrimaryOrange)
            }

            state.orders.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(Res.string.orders_empty),
                    color = TextSecondary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.orders, key = { it.id }) { order ->
                    OrderRow(order = order)
                }
            }
        }
    }
}
