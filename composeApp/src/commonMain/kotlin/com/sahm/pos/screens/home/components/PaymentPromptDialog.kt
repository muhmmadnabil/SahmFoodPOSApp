package com.sahm.pos.screens.home.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sahm.pos.domain.entity.PaymentType
import com.sahm.pos.theme.CardBackground
import com.sahm.pos.theme.PrimaryOrange
import com.sahm.pos.theme.TextPrimary
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.stringResource
import sahmfoodposapp.composeapp.generated.resources.Res
import sahmfoodposapp.composeapp.generated.resources.home_cancel
import sahmfoodposapp.composeapp.generated.resources.home_confirm_payment
import sahmfoodposapp.composeapp.generated.resources.home_select_payment_method

@Composable
internal fun PaymentPromptDialog(
    paymentTypes: ImmutableList<PaymentType>,
    selectedPaymentType: PaymentType,
    onPaymentSelected: (PaymentType) -> Unit,
    onConfirmPayment: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        shape = RoundedCornerShape(18.dp),
        title = {
            Text(
                text = stringResource(Res.string.home_select_payment_method),
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(Modifier.fillMaxWidth()) {
                PaymentMethodSelector(
                    paymentTypes = paymentTypes,
                    selectedPaymentType = selectedPaymentType,
                    onPaymentSelected = onPaymentSelected,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmPayment,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
            ) {
                Text(
                    text = stringResource(Res.string.home_confirm_payment),
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(Res.string.home_cancel),
                    color = TextPrimary,
                )
            }
        },
    )
}
