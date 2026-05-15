package com.sahm.pos.screens.home

import com.sahm.pos.domain.entity.MenuItem

data class HomeUiState(
    val menuItems: List<MenuItem> = emptyList(),
)

sealed interface HomeIntent {
    data object ScreenOpened : HomeIntent
}
