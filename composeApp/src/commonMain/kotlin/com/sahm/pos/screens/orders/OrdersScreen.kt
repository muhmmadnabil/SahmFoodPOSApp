package com.sahm.pos.screens.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.sahm.pos.screens.orders.components.OrderDetailsPanel
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
    val selectedDetails = state.selectedOrderDetails

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .padding(horizontal = horizontalPadding, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (screenType == ScreenType.Phone && selectedDetails != null) {
            OrderDetailsContent(
                state = state,
                modifier = Modifier.fillMaxSize(),
                onIntent = onIntent,
            )
        } else {
            OrdersControls(
                searchQuery = state.searchQuery,
                selectedOrderType = state.selectedOrderType,
                sortDirection = state.sortDirection,
                onSearchChanged = { onIntent(OrdersIntent.SearchChanged(it)) },
                onOrderTypeSelected = { onIntent(OrdersIntent.OrderTypeSelected(it)) },
                onSortDirectionSelected = { onIntent(OrdersIntent.SortDirectionSelected(it)) },
            )

            if (screenType == ScreenType.Phone) {
                OrderListContent(
                    state = state,
                    modifier = Modifier.fillMaxSize(),
                    onIntent = onIntent,
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    OrderListContent(
                        state = state,
                        modifier = Modifier.weight(1f),
                        onIntent = onIntent,
                    )
                    if (state.selectedOrderDetails != null || state.isOrderDetailsLoading) {
                        OrderDetailsContent(
                            state = state,
                            modifier = Modifier.width(420.dp),
                            onIntent = onIntent,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderListContent(
    state: OrdersUiState,
    modifier: Modifier = Modifier,
    onIntent: (OrdersIntent) -> Unit,
) {
    when {
        state.isLoading -> Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = PrimaryOrange)
        }

        state.orders.isEmpty() -> Box(
            modifier = modifier.fillMaxSize(),
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
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(state.orders, key = { it.id }) { order ->
                OrderRow(
                    order = order,
                    isSelected = state.selectedOrderDetails?.id == order.id,
                    onClick = { onIntent(OrdersIntent.OrderSelected(order.id)) },
                )
            }
        }
    }
}

@Composable
private fun OrderDetailsContent(
    state: OrdersUiState,
    modifier: Modifier = Modifier,
    onIntent: (OrdersIntent) -> Unit,
) {
    val details = state.selectedOrderDetails
    when {
        state.isOrderDetailsLoading && details == null -> Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = PrimaryOrange)
        }

        details != null -> LazyColumn(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                OrderDetailsPanel(
                    details = details,
                    isPrinting = state.isPrinting,
                    errorMessage = state.detailsErrorMessage,
                    modifier = Modifier.fillMaxWidth(),
                    onPrintAgain = { onIntent(OrdersIntent.PrintAgainClicked) },
                    onClose = { onIntent(OrdersIntent.OrderDetailsDismissed) },
                )
            }
        }

        else -> Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            state.detailsErrorMessage?.let { error ->
                Text(
                    text = error,
                    color = TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
