package com.sahm.pos.screens.orders.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sahm.pos.components.PosIcons
import com.sahm.pos.screens.home.components.orderTypeLabel
import com.sahm.pos.screens.orders.OrderDetailsUiState
import com.sahm.pos.theme.BorderDefault
import com.sahm.pos.theme.CardBackground
import com.sahm.pos.theme.ErrorRed
import com.sahm.pos.theme.PrimaryOrange
import com.sahm.pos.theme.TextPrimary
import com.sahm.pos.theme.TextSecondary
import com.sahm.pos.utils.toUtcDateTimeText
import org.jetbrains.compose.resources.stringResource
import sahmfoodposapp.composeapp.generated.resources.Res
import sahmfoodposapp.composeapp.generated.resources.home_discount
import sahmfoodposapp.composeapp.generated.resources.home_price_format
import sahmfoodposapp.composeapp.generated.resources.home_service
import sahmfoodposapp.composeapp.generated.resources.home_subtotal
import sahmfoodposapp.composeapp.generated.resources.home_tax
import sahmfoodposapp.composeapp.generated.resources.home_total
import sahmfoodposapp.composeapp.generated.resources.orders_cashier
import sahmfoodposapp.composeapp.generated.resources.orders_close_details
import sahmfoodposapp.composeapp.generated.resources.orders_created_at
import sahmfoodposapp.composeapp.generated.resources.orders_details_title
import sahmfoodposapp.composeapp.generated.resources.orders_discount_code
import sahmfoodposapp.composeapp.generated.resources.orders_items_title
import sahmfoodposapp.composeapp.generated.resources.orders_paid_at
import sahmfoodposapp.composeapp.generated.resources.orders_payment_status
import sahmfoodposapp.composeapp.generated.resources.orders_payments_title
import sahmfoodposapp.composeapp.generated.resources.orders_print_again
import sahmfoodposapp.composeapp.generated.resources.orders_print_status
import sahmfoodposapp.composeapp.generated.resources.orders_printing
import sahmfoodposapp.composeapp.generated.resources.orders_refunds_title
import sahmfoodposapp.composeapp.generated.resources.orders_status
import sahmfoodposapp.composeapp.generated.resources.orders_type

@Composable
fun OrderDetailsPanel(
    details: OrderDetailsUiState,
    isPrinting: Boolean,
    errorMessage: String?,
    modifier: Modifier = Modifier,
    onPrintAgain: () -> Unit,
    onClose: () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = CardBackground,
        border = BorderStroke(1.dp, BorderDefault),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.orders_details_title),
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "#${details.id}",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                OutlinedButton(
                    onClick = onClose,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, BorderDefault),
                ) {
                    Text(
                        text = stringResource(Res.string.orders_close_details),
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Button(
                onClick = onPrintAgain,
                enabled = details.canPrintAgain && !isPrinting,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryOrange,
                    disabledContainerColor = PrimaryOrange.copy(alpha = 0.56f),
                ),
            ) {
                if (isPrinting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White,
                    )
                    Spacer(Modifier.width(8.dp))
                } else {
                    Icon(
                        imageVector = PosIcons.Receipt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.White,
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = stringResource(if (isPrinting) Res.string.orders_printing else Res.string.orders_print_again),
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            errorMessage?.let {
                Text(
                    text = it,
                    color = ErrorRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoRow(stringResource(Res.string.orders_type), orderTypeLabel(details.orderType))
                InfoRow(stringResource(Res.string.orders_status), details.orderStatus.name)
                InfoRow(stringResource(Res.string.orders_payment_status), details.paymentStatus.name)
                InfoRow(stringResource(Res.string.orders_print_status), details.printStatus.name)
                InfoRow(stringResource(Res.string.orders_cashier), details.cashierName)
                InfoRow(stringResource(Res.string.orders_created_at), details.createdAt.toUtcDateTimeText())
                details.paidAt?.let { InfoRow(stringResource(Res.string.orders_paid_at), it.toUtcDateTimeText()) }
                details.discountPromoCode?.let { InfoRow(stringResource(Res.string.orders_discount_code), it) }
            }

            DetailsSection(title = stringResource(Res.string.orders_items_title)) {
                details.items.forEach { item ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top,
                        ) {
                            Text(
                                text = item.name,
                                modifier = Modifier.weight(1f),
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = priceText(item.totalAmount),
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Text(
                            text = "${item.quantity} x ${priceText(item.unitPrice)}",
                            color = TextSecondary,
                            fontSize = 12.sp,
                        )
                    }
                }
            }

            if (details.payments.isNotEmpty()) {
                DetailsSection(title = stringResource(Res.string.orders_payments_title)) {
                    details.payments.forEach { payment ->
                        InfoRow("${payment.type.name} / ${payment.status.name}", priceText(payment.amount))
                    }
                }
            }

            if (details.refunds.isNotEmpty()) {
                DetailsSection(title = stringResource(Res.string.orders_refunds_title)) {
                    details.refunds.forEach { refund ->
                        InfoRow("${refund.type.name} / ${refund.status.name}", priceText(refund.amount))
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MoneyRow(stringResource(Res.string.home_subtotal), details.subtotalAmount)
                MoneyRow(stringResource(Res.string.home_service), details.serviceAmount)
                MoneyRow(stringResource(Res.string.home_discount), -details.discountAmount)
                MoneyRow(stringResource(Res.string.home_tax), details.taxAmount)
                Spacer(Modifier.height(2.dp))
                MoneyRow(
                    label = stringResource(Res.string.home_total),
                    amount = details.totalAmount,
                    isTotal = true,
                )
            }
        }
    }
}

@Composable
private fun DetailsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
        )
        content()
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MoneyRow(
    label: String,
    amount: Long,
    isTotal: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = if (isTotal) TextPrimary else TextSecondary,
            fontSize = if (isTotal) 15.sp else 13.sp,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Medium,
        )
        Text(
            text = priceText(amount),
            color = TextPrimary,
            fontSize = if (isTotal) 16.sp else 13.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun priceText(value: Long): String {
    val prefix = if (value < 0) "-" else ""
    val absoluteValue = kotlin.math.abs(value)
    val pounds = absoluteValue / 100
    val piasters = (absoluteValue % 100).toString().padStart(2, '0')
    return prefix + stringResource(Res.string.home_price_format, pounds, piasters)
}
