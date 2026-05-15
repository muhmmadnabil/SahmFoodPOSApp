package com.sahm.pos.screens.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sahm.pos.screens.sync.components.SyncChoiceCard
import com.sahm.pos.utils.ScreenType

@Composable
fun SyncScreen(
    screenType: ScreenType,
    onUsersClick: () -> Unit,
    onItemsClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (screenType == ScreenType.Phone) {
            Column(
                verticalArrangement = Arrangement.spacedBy(30.dp, Alignment.CenterVertically),
                modifier = Modifier.fillMaxHeight()
            ) {
                SyncChoiceCard(type = SyncDetailType.Users, onClick = onUsersClick)
                SyncChoiceCard(type = SyncDetailType.Items, onClick = onItemsClick)
            }
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SyncChoiceCard(
                    type = SyncDetailType.Users,
                    onClick = onUsersClick,
                    modifier = Modifier.weight(1f),
                )

                SyncChoiceCard(
                    type = SyncDetailType.Items,
                    onClick = onItemsClick,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}