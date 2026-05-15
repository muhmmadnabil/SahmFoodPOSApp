package com.sahm.pos.screens.sync.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sahm.pos.components.PosIcons
import com.sahm.pos.screens.sync.SyncDetailType
import com.sahm.pos.theme.CardBackground
import com.sahm.pos.theme.ShadowColor
import com.sahm.pos.theme.TextPrimary
import com.sahm.pos.theme.TextSecondary
import org.jetbrains.compose.resources.stringResource

@Composable
fun SyncChoiceCard(
    modifier: Modifier = Modifier,
    type: SyncDetailType,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .shadow(
                elevation = 18.dp,
                shape = RoundedCornerShape(12.dp),
                ambientColor = ShadowColor,
                spotColor = ShadowColor,
            ),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)
        ) {
            SyncIconBubble(icon = type.icon, size = 64.dp, iconSize = 32.dp)

            Text(
                text = stringResource(type.title),
                color = TextPrimary,
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = stringResource(type.description),
                color = TextSecondary,
                fontSize = 16.sp,
                lineHeight = 22.sp,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Icon(
                    imageVector = PosIcons.ChevronRight,
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}