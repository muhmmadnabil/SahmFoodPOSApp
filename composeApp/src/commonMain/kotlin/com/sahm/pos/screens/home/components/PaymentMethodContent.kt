package com.sahm.pos.screens.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sahm.pos.domain.entity.PaymentType
import com.sahm.pos.theme.PrimaryOrange
import com.sahm.pos.theme.TextPrimary
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.stringResource
import sahmfoodposapp.composeapp.generated.resources.Res
import sahmfoodposapp.composeapp.generated.resources.home_confirm_payment
import sahmfoodposapp.composeapp.generated.resources.home_select_payment_method

@Composable
fun PaymentMethodContent(
    paymentTypes: ImmutableList<PaymentType>,
    selectedPaymentType: PaymentType,
    showTitle: Boolean,
    onPaymentSelected: (PaymentType) -> Unit,
    onConfirmPayment: (() -> Unit)?,
) {
    Column(
        modifier = Modifier.padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (showTitle) {
            Text(
                text = stringResource(Res.string.home_select_payment_method),
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        PaymentMethodSelector(
            paymentTypes = paymentTypes,
            selectedPaymentType = selectedPaymentType,
            onPaymentSelected = onPaymentSelected,
        )
        if (onConfirmPayment != null) {
            Button(
                onClick = onConfirmPayment,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
            ) {
                Text(
                    text = stringResource(Res.string.home_confirm_payment),
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}