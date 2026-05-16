package com.sahm.pos.screens.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sahm.pos.domain.entity.MenuItem
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun MenuItemsGrid(
    items: ImmutableList<MenuItem>,
    isTablet: Boolean,
    modifier: Modifier = Modifier,
    onAddItem: (String) -> Unit,
) {
    LazyVerticalGrid(
        columns = if (isTablet) GridCells.Adaptive(minSize = 180.dp) else GridCells.Fixed(2),
        modifier = modifier.fillMaxWidth(),
        contentPadding = if (isTablet) PaddingValues(bottom = 2.dp) else PaddingValues(),
        horizontalArrangement = Arrangement.spacedBy(if (isTablet) 16.dp else 10.dp),
        verticalArrangement = Arrangement.spacedBy(if (isTablet) 16.dp else 10.dp),
    ) {
        items(items, key = { it.id }) { item ->
            MenuItemCard(
                item = item,
                isTablet = isTablet,
                onAddItem = onAddItem,
            )
        }
    }
}
