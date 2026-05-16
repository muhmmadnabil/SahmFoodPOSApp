package com.sahm.pos.screens.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import sahmfoodposapp.composeapp.generated.resources.Res
import sahmfoodposapp.composeapp.generated.resources.home_search_items_placeholder
import sahmfoodposapp.composeapp.generated.resources.home_search_menu_items_placeholder

@Composable
internal fun HomeHeader(
    isTablet: Boolean,
    searchText: String,
    onSearchChanged: (String) -> Unit,
    onSettingsClick: () -> Unit,
) {
    if (isTablet) {
        TabletHomeHeader(
            searchText = searchText,
            onSearchChanged = onSearchChanged,
            onSettingsClick = onSettingsClick
        )
    } else {
        PhoneHomeHeader(
            searchText = searchText,
            onSearchChanged = onSearchChanged,
            onSettingsClick = onSettingsClick
        )
    }
}

@Composable
private fun TabletHomeHeader(
    searchText: String,
    onSearchChanged: (String) -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        HomeBrandTitle(
            modifier = Modifier.weight(0.22f),
            fontSize = 20,
        )
        HomeSearchField(
            searchText = searchText,
            placeholder = stringResource(Res.string.home_search_menu_items_placeholder),
            textFontSize = 14,
            modifier = Modifier.weight(0.56f),
            onSearchChanged = onSearchChanged,
        )
        SettingsButton(
            modifier = Modifier.weight(0.16f),
            textAlign = TextAlign.Center,
            onSettingsClick = onSettingsClick
        )
    }
}

@Composable
private fun PhoneHomeHeader(
    searchText: String,
    onSearchChanged: (String) -> Unit,
    onSettingsClick:()-> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HomeBrandTitle(
            modifier = Modifier.weight(1f),
            fontSize = 26,
        )
        SettingsButton(onSettingsClick = onSettingsClick)
    }
    HomeSearchField(
        searchText = searchText,
        placeholder = stringResource(Res.string.home_search_items_placeholder),
        textFontSize = 18,
        modifier = Modifier.fillMaxWidth(),
        onSearchChanged = onSearchChanged,
    )
}