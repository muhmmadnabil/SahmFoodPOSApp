package com.sahm.pos.screens.home.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sahm.pos.theme.PrimaryOrange
import com.sahm.pos.theme.TextPrimary
import org.jetbrains.compose.resources.stringResource
import sahmfoodposapp.composeapp.generated.resources.Res
import sahmfoodposapp.composeapp.generated.resources.home_current_order
import sahmfoodposapp.composeapp.generated.resources.home_order_items_count

@Composable
fun CurrentOrderHeader(
    itemCount: Int,
    showItemCount: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.home_current_order),
            modifier = Modifier.weight(1f),
            color = TextPrimary,
            fontSize = if (showItemCount) 21.sp else 18.sp,
            fontWeight = FontWeight.Bold,
        )
        if (showItemCount) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = PrimaryOrange.copy(alpha = 0.08f),
            ) {
                Text(
                    text = stringResource(Res.string.home_order_items_count, itemCount),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = PrimaryOrange,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}