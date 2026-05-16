package com.sahm.pos.screens.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sahm.pos.screens.home.HomeConstants
import com.sahm.pos.theme.BorderDefault
import com.sahm.pos.theme.PrimaryOrange
import com.sahm.pos.theme.TextPrimary
import com.sahm.pos.theme.TextSecondary
import org.jetbrains.compose.resources.stringResource
import sahmfoodposapp.composeapp.generated.resources.Res
import sahmfoodposapp.composeapp.generated.resources.home_discount
import sahmfoodposapp.composeapp.generated.resources.home_service
import sahmfoodposapp.composeapp.generated.resources.home_subtotal
import sahmfoodposapp.composeapp.generated.resources.home_tax
import sahmfoodposapp.composeapp.generated.resources.home_total

@Composable
internal fun OrderTotals(
    subtotal: Long,
    discount: Long,
    service: Long,
    showService: Boolean,
    tax: Long,
    total: Long,
    isTablet: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(if (isTablet) 10.dp else 5.dp)) {
        TotalLine(
            label = stringResource(Res.string.home_subtotal),
            value = subtotal,
            fontSize = if (isTablet) 14 else 15,
        )
        if (discount > 0) {
            TotalLine(
                label = stringResource(Res.string.home_discount),
                value = -discount,
                fontSize = if (isTablet) 14 else 15,
            )
        }
        if (showService) {
            TotalLine(
                label = stringResource(Res.string.home_service, HomeConstants.ServicePercent),
                value = service,
                fontSize = if (isTablet) 14 else 15,
            )
        }
        TotalLine(
            label = stringResource(Res.string.home_tax, HomeConstants.TaxPercent),
            value = tax,
            fontSize = if (isTablet) 14 else 15,
        )
    }
    HorizontalDivider(color = BorderDefault)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = stringResource(Res.string.home_total),
            modifier = Modifier.weight(1f),
            color = TextPrimary,
            fontSize = if (isTablet) 18.sp else 19.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = homePriceText(total),
            color = PrimaryOrange,
            fontSize = if (isTablet) 24.sp else 25.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun TotalLine(
    label: String,
    value: Long,
    fontSize: Int,
) {
    Row(Modifier.fillMaxWidth()) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = TextSecondary,
            fontSize = fontSize.sp,
        )
        Text(
            text = if (value < 0) "-${homePriceText(-value)}" else homePriceText(value),
            color = TextSecondary,
            fontSize = fontSize.sp,
        )
    }
}
