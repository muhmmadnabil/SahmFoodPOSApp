package com.sahm.pos.screens.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sahm.pos.domain.entity.PaymentType
import com.sahm.pos.theme.BorderDefault
import com.sahm.pos.theme.CardBackground
import kotlinx.collections.immutable.ImmutableList

@Composable
fun PaymentMethodCard(
    paymentTypes: ImmutableList<PaymentType>,
    selectedPaymentType: PaymentType,
    showTitle: Boolean,
    modifier: Modifier = Modifier,
    onPaymentSelected: (PaymentType) -> Unit,
    onConfirmPayment: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = CardBackground,
        border = BorderStroke(1.dp, BorderDefault),
    ) {
        PaymentMethodContent(
            paymentTypes = paymentTypes,
            selectedPaymentType = selectedPaymentType,
            showTitle = showTitle,
            onPaymentSelected = onPaymentSelected,
            onConfirmPayment = onConfirmPayment,
        )
    }
}