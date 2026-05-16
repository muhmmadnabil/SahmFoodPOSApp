package com.sahm.pos.screens.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sahm.pos.theme.CardBackground
import com.sahm.pos.theme.PrimaryOrange
import com.sahm.pos.theme.TextPrimary
import org.jetbrains.compose.resources.stringResource
import sahmfoodposapp.composeapp.generated.resources.Res
import sahmfoodposapp.composeapp.generated.resources.home_cancel
import sahmfoodposapp.composeapp.generated.resources.home_card_holder_name
import sahmfoodposapp.composeapp.generated.resources.home_card_number
import sahmfoodposapp.composeapp.generated.resources.home_card_payment_title
import sahmfoodposapp.composeapp.generated.resources.home_cvv
import sahmfoodposapp.composeapp.generated.resources.home_expiry_month
import sahmfoodposapp.composeapp.generated.resources.home_expiry_year
import sahmfoodposapp.composeapp.generated.resources.home_pay_amount

@Composable
internal fun CardPaymentDialog(
    cardNumber: String,
    expiryMonth: String,
    expiryYear: String,
    cvv: String,
    cardHolderName: String,
    total: Long,
    isProcessing: Boolean,
    onCardNumberChanged: (String) -> Unit,
    onExpiryMonthChanged: (String) -> Unit,
    onExpiryYearChanged: (String) -> Unit,
    onCvvChanged: (String) -> Unit,
    onCardHolderNameChanged: (String) -> Unit,
    onPay: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        shape = RoundedCornerShape(18.dp),
        title = {
            Text(
                text = stringResource(Res.string.home_card_payment_title),
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PaymentTextField(
                    value = cardNumber,
                    onValueChange = onCardNumberChanged,
                    label = stringResource(Res.string.home_card_number),
                    keyboardType = KeyboardType.Number,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PaymentTextField(
                        value = expiryMonth,
                        onValueChange = onExpiryMonthChanged,
                        label = stringResource(Res.string.home_expiry_month),
                        modifier = Modifier.weight(1f),
                        keyboardType = KeyboardType.Number,
                    )
                    PaymentTextField(
                        value = expiryYear,
                        onValueChange = onExpiryYearChanged,
                        label = stringResource(Res.string.home_expiry_year),
                        modifier = Modifier.weight(1f),
                        keyboardType = KeyboardType.Number,
                    )
                }
                PaymentTextField(
                    value = cvv,
                    onValueChange = onCvvChanged,
                    label = stringResource(Res.string.home_cvv),
                    keyboardType = KeyboardType.NumberPassword,
                )
                PaymentTextField(
                    value = cardHolderName,
                    onValueChange = onCardHolderNameChanged,
                    label = stringResource(Res.string.home_card_holder_name),
                    keyboardType = KeyboardType.Text,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onPay,
                enabled = !isProcessing,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
            ) {
                Text(
                    text = stringResource(Res.string.home_pay_amount, total / 100, (total % 100).toString().padStart(2, '0')),
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isProcessing) {
                Text(
                    text = stringResource(Res.string.home_cancel),
                    color = TextPrimary,
                )
            }
        },
    )
}

@Composable
private fun PaymentTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryOrange),
        shape = RoundedCornerShape(8.dp),
    )
}
