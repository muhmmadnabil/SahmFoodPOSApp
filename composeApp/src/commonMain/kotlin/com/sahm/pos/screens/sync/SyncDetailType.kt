package com.sahm.pos.screens.sync

import androidx.compose.ui.graphics.vector.ImageVector
import com.sahm.pos.components.PosIcons
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.compose.resources.StringResource
import sahmfoodposapp.composeapp.generated.resources.Res
import sahmfoodposapp.composeapp.generated.resources.sync_discounts_description
import sahmfoodposapp.composeapp.generated.resources.sync_discounts_title
import sahmfoodposapp.composeapp.generated.resources.sync_failed
import sahmfoodposapp.composeapp.generated.resources.sync_items_description
import sahmfoodposapp.composeapp.generated.resources.sync_items_title
import sahmfoodposapp.composeapp.generated.resources.sync_new_discounts
import sahmfoodposapp.composeapp.generated.resources.sync_new_items
import sahmfoodposapp.composeapp.generated.resources.sync_new_users
import sahmfoodposapp.composeapp.generated.resources.sync_total_discounts
import sahmfoodposapp.composeapp.generated.resources.sync_total_items
import sahmfoodposapp.composeapp.generated.resources.sync_total_users
import sahmfoodposapp.composeapp.generated.resources.sync_updated_discounts
import sahmfoodposapp.composeapp.generated.resources.sync_updated_items
import sahmfoodposapp.composeapp.generated.resources.sync_updated_users
import sahmfoodposapp.composeapp.generated.resources.sync_users_description
import sahmfoodposapp.composeapp.generated.resources.sync_users_title

enum class SyncDetailType(
    val title: StringResource,
    val description: StringResource,
    val icon: ImageVector,
    val rows: ImmutableList<SyncDetailRow>,
) {
    Users(
        title = Res.string.sync_users_title,
        description = Res.string.sync_users_description,
        icon = PosIcons.Users,
        rows = listOf(
            SyncDetailRow(Res.string.sync_total_users, "0"),
            SyncDetailRow(Res.string.sync_new_users, "0"),
            SyncDetailRow(Res.string.sync_updated_users, "0"),
            SyncDetailRow(Res.string.sync_failed, "0"),
        ).toImmutableList(),
    ),
    Items(
        title = Res.string.sync_items_title,
        description = Res.string.sync_items_description,
        icon = PosIcons.Bag,
        rows = listOf(
            SyncDetailRow(Res.string.sync_total_items, "0"),
            SyncDetailRow(Res.string.sync_new_items, "0"),
            SyncDetailRow(Res.string.sync_updated_items, "0"),
            SyncDetailRow(Res.string.sync_failed, "0"),
        ).toImmutableList(),
    ),
    Discounts(
        title = Res.string.sync_discounts_title,
        description = Res.string.sync_discounts_description,
        icon = PosIcons.Discount,
        rows = listOf(
            SyncDetailRow(Res.string.sync_total_discounts, "0"),
            SyncDetailRow(Res.string.sync_new_discounts, "0"),
            SyncDetailRow(Res.string.sync_updated_discounts, "0"),
            SyncDetailRow(Res.string.sync_failed, "0"),
        ).toImmutableList(),
    )
}