package com.sahm.pos.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sahm.pos.domain.usecase.GetMenuItemsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val getMenuItemsUseCase: GetMenuItemsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.ScreenOpened -> loadMenuItems()
        }
    }

    private fun loadMenuItems() {
        viewModelScope.launch {
            _state.update { it.copy(menuItems = getMenuItemsUseCase()) }
        }
    }
}
