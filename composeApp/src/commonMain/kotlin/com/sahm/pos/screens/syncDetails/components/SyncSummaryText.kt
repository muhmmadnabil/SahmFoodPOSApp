package com.sahm.pos.screens.syncDetails.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sahm.pos.screens.sync.SyncDetailType
import com.sahm.pos.screens.sync.components.SyncIconBubble
import com.sahm.pos.theme.TextPrimary
import com.sahm.pos.theme.TextSecondary
import org.jetbrains.compose.resources.stringResource

@Composable
fun SyncSummaryText(
    type: SyncDetailType,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        SyncIconBubble(icon = type.icon, size = 56.dp, iconSize = 28.dp)
        Column {
            Text(
                text = stringResource(type.title),
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(type.description),
                color = TextSecondary,
                fontSize = 14.sp,
                lineHeight = 19.sp,
            )
        }
    }
}