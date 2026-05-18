package com.sahm.pos.screens.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sahm.pos.screens.sync.components.SyncChoiceCard
import com.sahm.pos.utils.ScreenType

@Composable
fun SyncScreen(
    screenType: ScreenType,
    onUsersClick: () -> Unit,
    onItemsClick: () -> Unit,
    onDiscountsClick: () -> Unit,
    onOrdersClick: () -> Unit,
    onPaymentsClick: () -> Unit,
) {
    val choices = listOf(
        SyncDetailType.Users to onUsersClick,
        SyncDetailType.Items to onItemsClick,
        SyncDetailType.Discounts to onDiscountsClick,
        SyncDetailType.Orders to onOrdersClick,
        SyncDetailType.Payments to onPaymentsClick,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (screenType == ScreenType.Phone) {
            Column(
                verticalArrangement = Arrangement.spacedBy(30.dp, Alignment.CenterVertically),
                modifier = Modifier
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
            ) {
                choices.forEach { (type, onClick) ->
                    SyncChoiceCard(type = type, onClick = onClick)
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                choices.chunked(3).forEach { rowChoices ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        rowChoices.forEach { (type, onClick) ->
                            SyncChoiceCard(
                                type = type,
                                onClick = onClick,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}
