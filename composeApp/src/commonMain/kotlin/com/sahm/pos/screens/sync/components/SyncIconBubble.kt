package com.sahm.pos.screens.sync.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import com.sahm.pos.theme.PrimaryOrange
import com.sahm.pos.theme.SyncIconBackground

@Composable
fun SyncIconBubble(
    icon: ImageVector,
    size: Dp,
    iconSize: Dp,
) {
    Box(
        modifier = Modifier
            .size(size)
            .background(SyncIconBackground, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PrimaryOrange,
            modifier = Modifier.size(iconSize),
        )
    }
}