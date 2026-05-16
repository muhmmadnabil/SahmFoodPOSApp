package com.sahm.pos.screens.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sahm.pos.components.PosIcons
import com.sahm.pos.theme.BorderDefault
import com.sahm.pos.theme.CardBackground
import com.sahm.pos.theme.PrimaryOrange
import com.sahm.pos.theme.TextPrimary

@Composable
fun QuantityStepper(
    itemId: String,
    quantity: Int,
    isTablet: Boolean,
    modifier: Modifier = Modifier,
    onQuantityChanged: (String, Int) -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(if (isTablet) 8.dp else 9.dp),
        color = CardBackground,
        border = BorderStroke(1.dp, BorderDefault),
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (isTablet) 9.dp else 8.dp,
                vertical = 5.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isTablet) {
                Arrangement.spacedBy(13.dp)
            } else {
                Arrangement.SpaceBetween
            },
        ) {
            QuantityButton(
                icon = PosIcons.Minus,
                onClick = { onQuantityChanged(itemId, quantity - 1) },
            )
            Text(
                text = quantity.toString(),
                color = TextPrimary,
                fontSize = if (isTablet) 14.sp else 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            QuantityButton(
                icon = PosIcons.Plus,
                onClick = { onQuantityChanged(itemId, quantity + 1) },
            )
        }
    }
}

@Composable
private fun QuantityButton(
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier
            .clickable(onClick = onClick)
            .size(16.dp),
        tint = PrimaryOrange,
    )
}
