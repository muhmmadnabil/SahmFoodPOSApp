package com.sahm.pos.screens.syncDetails

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sahm.pos.screens.sync.SyncDetailType
import com.sahm.pos.screens.sync.SyncDetailRow
import com.sahm.pos.screens.syncDetails.components.LastSyncCard
import com.sahm.pos.screens.syncDetails.components.SectionTitle
import com.sahm.pos.screens.syncDetails.components.SyncDetailsTable
import com.sahm.pos.screens.syncDetails.components.SyncSummaryCard
import com.sahm.pos.utils.ScreenType
import com.sahm.pos.utils.toUtcDateTimeText
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.compose.resources.stringResource
import sahmfoodposapp.composeapp.generated.resources.Res
import sahmfoodposapp.composeapp.generated.resources.sync_details_title
import sahmfoodposapp.composeapp.generated.resources.sync_failed
import sahmfoodposapp.composeapp.generated.resources.sync_last_sync
import sahmfoodposapp.composeapp.generated.resources.sync_never_synced
import sahmfoodposapp.composeapp.generated.resources.sync_new_discounts
import sahmfoodposapp.composeapp.generated.resources.sync_new_items
import sahmfoodposapp.composeapp.generated.resources.sync_new_users
import sahmfoodposapp.composeapp.generated.resources.sync_total_discounts
import sahmfoodposapp.composeapp.generated.resources.sync_total_items
import sahmfoodposapp.composeapp.generated.resources.sync_total_users
import sahmfoodposapp.composeapp.generated.resources.sync_updated_discounts
import sahmfoodposapp.composeapp.generated.resources.sync_updated_items
import sahmfoodposapp.composeapp.generated.resources.sync_updated_users

@Composable
fun SyncDetailsScreen(
    screenType: ScreenType,
    type: SyncDetailType,
    state: SyncUiState = SyncUiState(),
    onIntent: (SyncIntent) -> Unit = {},
) {
    val rows = when (type) {
        SyncDetailType.Users -> listOf(
            SyncDetailRow(Res.string.sync_total_users, state.count.toString()),
            SyncDetailRow(Res.string.sync_new_users, state.lastSyncedCount.toString()),
            SyncDetailRow(Res.string.sync_updated_users, "0"),
            SyncDetailRow(Res.string.sync_failed, state.skippedCount.toString()),
        )

        SyncDetailType.Items -> listOf(
            SyncDetailRow(Res.string.sync_total_items, state.count.toString()),
            SyncDetailRow(Res.string.sync_new_items, state.lastSyncedCount.toString()),
            SyncDetailRow(Res.string.sync_updated_items, "0"),
            SyncDetailRow(Res.string.sync_failed, state.skippedCount.toString()),
        )

        SyncDetailType.Discounts -> listOf(
            SyncDetailRow(Res.string.sync_total_discounts, state.count.toString()),
            SyncDetailRow(Res.string.sync_new_discounts, state.lastSyncedCount.toString()),
            SyncDetailRow(Res.string.sync_updated_discounts, "0"),
            SyncDetailRow(Res.string.sync_failed, state.skippedCount.toString()),
        )
    }.toImmutableList()
    val lastSyncAt =
        state.lastSyncAt?.toUtcDateTimeText() ?: stringResource(Res.string.sync_never_synced)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (screenType == ScreenType.Phone) 20.dp else 40.dp),
    ) {
        SyncSummaryCard(
            type = type,
            screenType = screenType,
            isSyncing = state.isSyncing,
            onStartSync = {
                when (type) {
                    SyncDetailType.Users -> onIntent(SyncIntent.SyncUsersClicked)
                    SyncDetailType.Items -> onIntent(SyncIntent.SyncItemsClicked)
                    SyncDetailType.Discounts -> onIntent(SyncIntent.SyncDiscountsClicked)
                }
            },
        )
        Spacer(Modifier.height(22.dp))
        SectionTitle(text = stringResource(Res.string.sync_last_sync))
        Spacer(Modifier.height(10.dp))
        LastSyncCard(lastSyncAt = lastSyncAt)
        Spacer(Modifier.height(24.dp))
        SectionTitle(text = stringResource(Res.string.sync_details_title))
        Spacer(Modifier.height(10.dp))
        SyncDetailsTable(rows = rows)
    }
}