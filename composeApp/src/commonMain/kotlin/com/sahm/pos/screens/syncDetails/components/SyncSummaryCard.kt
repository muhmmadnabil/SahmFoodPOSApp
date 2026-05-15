package com.sahm.pos.screens.syncDetails.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.sahm.pos.screens.sync.SyncDetailType
import com.sahm.pos.theme.CardBackground
import com.sahm.pos.theme.ShadowColor
import com.sahm.pos.utils.ScreenType

@Composable
fun SyncSummaryCard(
    type: SyncDetailType,
    screenType: ScreenType,
    isSyncing: Boolean = false,
    onStartSync: () -> Unit = {},
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(10.dp),
                ambientColor = ShadowColor,
                spotColor = ShadowColor,
            ),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(10.dp),
    ) {
        if (screenType == ScreenType.Phone) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                SyncSummaryText(type = type)
                StartSyncButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSyncing,
                    isLoading = isSyncing,
                    onStartSync = onStartSync,
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SyncSummaryText(
                    type = type,
                    modifier = Modifier.weight(1f),
                )
                StartSyncButton(
                    enabled = !isSyncing,
                    isLoading = isSyncing,
                    onStartSync = onStartSync,
                )
            }
        }
    }
}
