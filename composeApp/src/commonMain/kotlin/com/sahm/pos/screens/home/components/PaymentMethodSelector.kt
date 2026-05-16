package com.sahm.pos.screens.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sahm.pos.domain.entity.PaymentType
import com.sahm.pos.theme.BorderDefault
import com.sahm.pos.theme.CardBackground
import com.sahm.pos.theme.PrimaryOrange
import com.sahm.pos.theme.TextPrimary
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.stringResource
import sahmfoodposapp.composeapp.generated.resources.Res
import sahmfoodposapp.composeapp.generated.resources.home_payment_card
import sahmfoodposapp.composeapp.generated.resources.home_payment_cash

@Composable
fun PaymentMethodSelector(
    paymentTypes: ImmutableList<PaymentType>,
    selectedPaymentType: PaymentType,
    modifier: Modifier = Modifier,
    onPaymentSelected: (PaymentType) -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        paymentTypes.forEach { paymentType ->
            val isSelected = selectedPaymentType == paymentType
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onPaymentSelected(paymentType) },
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) PrimaryOrange.copy(alpha = 0.08f) else CardBackground,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) PrimaryOrange else BorderDefault,
                ),
            ) {
                Text(
                    text = paymentTypeLabel(paymentType),
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = if (isSelected) PrimaryOrange else TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun paymentTypeLabel(paymentType: PaymentType): String =
    when (paymentType) {
        PaymentType.CARD -> stringResource(Res.string.home_payment_card)
        else -> stringResource(Res.string.home_payment_cash)
    }