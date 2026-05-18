package com.sahm.pos.screens.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.sahm.pos.components.PosIcons
import com.sahm.pos.screens.home.HomeOrderItemUiState
import com.sahm.pos.theme.BorderDefault
import com.sahm.pos.theme.TextPrimary
import com.sahm.pos.theme.TextSecondary

@Composable
internal fun OrderItemRow(
    orderItem: HomeOrderItemUiState,
    isTablet: Boolean,
    onQuantityChanged: (String, Int) -> Unit,
    onItemRemoved: (String) -> Unit,
) {
    val item = orderItem.item

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (isTablet) 12.dp else 10.dp),
        ) {
            AsyncImage(
                model = item.localImageUrl ?: item.imageUrl,
                contentDescription = item.name,
                modifier = Modifier
                    .weight(if (isTablet) 0.18f else 0.13f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(if (isTablet) 9.dp else 8.dp)),
                contentScale = ContentScale.Crop,
            )
            OrderItemDetails(
                orderItem = orderItem,
                isTablet = isTablet,
                modifier = Modifier.weight(if (isTablet) 0.42f else 0.58f),
                onQuantityChanged = onQuantityChanged,
            )
            Text(
                text = homePriceText(orderItem.lineTotal),
                modifier = Modifier.weight(if (isTablet) 0.28f else 0.2f),
                color = if (isTablet) TextPrimary else TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.End,
                overflow = TextOverflow.StartEllipsis,
                maxLines = 1,
            )
            Box(
                modifier = Modifier
                    .weight(if (isTablet) 0.06f else 0.05f),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    imageVector = PosIcons.Close,
                    contentDescription = null,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { onItemRemoved(item.id) },
                    tint = TextSecondary,
                )
            }
        }
        HorizontalDivider(color = BorderDefault)
    }
}
