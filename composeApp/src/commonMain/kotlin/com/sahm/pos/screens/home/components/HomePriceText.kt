package com.sahm.pos.screens.home.components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import sahmfoodposapp.composeapp.generated.resources.Res
import sahmfoodposapp.composeapp.generated.resources.home_price_format

@Composable
internal fun homePriceText(value: Long): String {
    val pounds = value / 100
    val piasters = (value % 100).toString().padStart(2, '0')
    return stringResource(Res.string.home_price_format, pounds, piasters)
}
