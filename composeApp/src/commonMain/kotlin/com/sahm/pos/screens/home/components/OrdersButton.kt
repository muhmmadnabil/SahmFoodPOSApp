package com.sahm.pos.screens.home.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.resources.stringResource
import sahmfoodposapp.composeapp.generated.resources.Res
import sahmfoodposapp.composeapp.generated.resources.home_orders

@Composable
fun OrdersButton(
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
    onOrdersClick: () -> Unit,
) {
    HomeHeaderActionButton(
        text = stringResource(Res.string.home_orders),
        modifier = modifier,
        textAlign = textAlign,
        onClick = onOrdersClick,
    )
}
