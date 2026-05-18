package com.sahm.pos.screens.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sahm.pos.theme.CardBackground
import com.sahm.pos.theme.TextPrimary
import com.sahm.pos.theme.TextSecondary
import org.jetbrains.compose.resources.stringResource
import sahmfoodposapp.composeapp.generated.resources.Res
import sahmfoodposapp.composeapp.generated.resources.settings_cashier_details
import sahmfoodposapp.composeapp.generated.resources.settings_cashier_id
import sahmfoodposapp.composeapp.generated.resources.settings_cashier_name
import sahmfoodposapp.composeapp.generated.resources.settings_cashier_phone
import sahmfoodposapp.composeapp.generated.resources.settings_loading_cashier
import sahmfoodposapp.composeapp.generated.resources.settings_unknown_cashier

@Composable
fun CashierDetailsCard(
    cashierName: String,
    cashierPhone: String,
    cashierId: String,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    val emptyText = stringResource(Res.string.settings_unknown_cashier)
    val name = cashierName.ifBlank { emptyText }
    val phone = cashierPhone.ifBlank { emptyText }
    val id = cashierId.ifBlank { emptyText }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                text = stringResource(Res.string.settings_cashier_details),
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )

            if (isLoading) {
                Text(
                    text = stringResource(Res.string.settings_loading_cashier),
                    color = TextSecondary,
                    fontSize = 16.sp,
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(Res.string.settings_cashier_name),
                        color = TextSecondary,
                        fontSize = 15.sp,
                    )
                    Text(
                        text = name,
                        modifier = Modifier.weight(1f),
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.End,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(Res.string.settings_cashier_phone),
                        color = TextSecondary,
                        fontSize = 15.sp,
                    )
                    Text(
                        text = phone,
                        modifier = Modifier.weight(1f),
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.End,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(Res.string.settings_cashier_id),
                        color = TextSecondary,
                        fontSize = 15.sp,
                    )
                    Text(
                        text = id,
                        modifier = Modifier.weight(1f),
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
    }
}
