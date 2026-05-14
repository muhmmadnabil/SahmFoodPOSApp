package com.sahm.pos.screens.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sahm.pos.screens.login.components.PhoneLoginContent
import com.sahm.pos.screens.login.components.TabletLoginContent
import com.sahm.pos.theme.ScreenBackground
import com.sahm.pos.utils.ScreenType

@Composable
fun LoginScreen(
    state: LoginUiState,
    screenType: ScreenType,
    onIntent: (LoginIntent) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .safeContentPadding()
            .imePadding()
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center,
    ) {
        when (screenType) {
            ScreenType.Phone -> {
                PhoneLoginContent(
                    state = state,
                    screenType = screenType,
                    onIntent = onIntent,
                )
            }

            ScreenType.Tablet -> {
                TabletLoginContent(
                    state = state,
                    screenType = screenType,
                    onIntent = onIntent,
                )
            }
        }
    }
}