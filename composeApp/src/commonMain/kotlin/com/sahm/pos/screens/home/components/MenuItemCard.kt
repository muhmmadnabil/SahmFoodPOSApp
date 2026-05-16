package com.sahm.pos.screens.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.sahm.pos.domain.entity.MenuItem
import com.sahm.pos.theme.BorderDefault
import com.sahm.pos.theme.CardBackground
import com.sahm.pos.theme.PrimaryOrange
import com.sahm.pos.theme.TextPrimary
import com.sahm.pos.theme.TextSecondary

@Composable
fun MenuItemCard(
    item: MenuItem,
    isTablet: Boolean,
    onAddItem: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(if (isTablet) 12.dp else 16.dp),
        color = CardBackground,
        border = BorderStroke(1.dp, BorderDefault),
    ) {
        Column(modifier = Modifier.padding(if (isTablet) 16.dp else 12.dp)) {
            AsyncImage(
                model = item.localImageUrl ?: item.imageUrl,
                contentDescription = item.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(if (isTablet) 1.35f else 1.45f)
                    .clip(RoundedCornerShape(if (isTablet) 10.dp else 12.dp)),
                contentScale = ContentScale.Crop,
            )
            Spacer(Modifier.height(if (isTablet) 12.dp else 10.dp))
            Text(
                text = item.name,
                color = TextPrimary,
                fontSize = if (isTablet) 16.sp else 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = if (isTablet) 1 else 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.description,
                modifier = Modifier.padding(top = if (isTablet) 6.dp else 4.dp),
                color = TextSecondary,
                fontSize = if (isTablet) 13.sp else 14.sp,
                lineHeight = if (isTablet) 18.sp else 19.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = if (isTablet) 18.dp else 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = homePriceText(item.price),
                    modifier = Modifier.weight(1f),
                    color = PrimaryOrange,
                    fontSize = if (isTablet) 15.sp else 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                AddItemButton(
                    isTablet = isTablet,
                    onClick = { onAddItem(item.id) },
                )
            }
        }
    }
}