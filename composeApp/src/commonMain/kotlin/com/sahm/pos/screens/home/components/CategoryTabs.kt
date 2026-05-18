package com.sahm.pos.screens.home.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun CategoryTabs(
    categories: ImmutableList<String>,
    selectedCategory: String,
    isTablet: Boolean,
    onCategorySelected: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(if (isTablet) 12.dp else 10.dp),
    ) {
        categories.forEach { category ->
            CategoryTab(
                category = category,
                isSelected = selectedCategory == category,
                isTablet = isTablet,
                onClick = { onCategorySelected(category) },
            )
        }
    }
}
