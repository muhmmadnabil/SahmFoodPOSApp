package com.sahm.pos.screens.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sahm.pos.domain.entity.OrderType
import com.sahm.pos.theme.BorderDefault
import com.sahm.pos.theme.CardBackground
import com.sahm.pos.theme.PrimaryOrange
import com.sahm.pos.theme.TextPrimary
import com.sahm.pos.theme.TextSecondary
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.stringResource
import sahmfoodposapp.composeapp.generated.resources.Res
import sahmfoodposapp.composeapp.generated.resources.home_discount_label
import sahmfoodposapp.composeapp.generated.resources.home_discount_placeholder
import sahmfoodposapp.composeapp.generated.resources.home_order_type_delivery
import sahmfoodposapp.composeapp.generated.resources.home_order_type_dine_in
import sahmfoodposapp.composeapp.generated.resources.home_order_type_label
import sahmfoodposapp.composeapp.generated.resources.home_order_type_takeaway

@Composable
internal fun OrderDetailsControls(
    orderTypes: ImmutableList<OrderType>,
    selectedOrderType: OrderType,
    discountText: String,
    isApplyingDiscount: Boolean,
    isTablet: Boolean,
    onOrderTypeSelected: (OrderType) -> Unit,
    onDiscountChanged: (String) -> Unit,
    onDiscountSubmitted: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(if (isTablet) 10.dp else 8.dp)) {
        Text(
            text = stringResource(Res.string.home_order_type_label),
            color = TextPrimary,
            fontSize = if (isTablet) 13.sp else 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            orderTypes.forEach { orderType ->
                val isSelected = selectedOrderType == orderType
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onOrderTypeSelected(orderType) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) PrimaryOrange.copy(alpha = 0.08f) else CardBackground,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isSelected) PrimaryOrange else BorderDefault,
                    ),
                ) {
                    Text(
                        text = orderTypeLabel(orderType),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 9.dp),
                        color = if (isSelected) PrimaryOrange else TextPrimary,
                        fontSize = if (isTablet) 12.sp else 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }
        }
        OutlinedTextField(
            value = discountText,
            onValueChange = onDiscountChanged,
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text(
                    text = stringResource(Res.string.home_discount_label),
                    color = TextSecondary,
                )
            },
            placeholder = {
                Text(
                    text = stringResource(Res.string.home_discount_placeholder),
                    color = TextSecondary,
                )
            },
            textStyle = LocalTextStyle.current.copy(
                fontSize = if (isTablet) 13.sp else 14.sp,
                color = TextPrimary,
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onDiscountSubmitted() }),
            trailingIcon = if (isApplyingDiscount) {
                {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = PrimaryOrange,
                        strokeWidth = 2.dp,
                    )
                }
            } else {
                null
            },
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryOrange),
            shape = RoundedCornerShape(8.dp),
        )
    }
}

@Composable
internal fun orderTypeLabel(orderType: OrderType): String =
    when (orderType) {
        OrderType.DINE_IN -> stringResource(Res.string.home_order_type_dine_in)
        OrderType.TAKEAWAY -> stringResource(Res.string.home_order_type_takeaway)
        OrderType.DELIVERY -> stringResource(Res.string.home_order_type_delivery)
    }
