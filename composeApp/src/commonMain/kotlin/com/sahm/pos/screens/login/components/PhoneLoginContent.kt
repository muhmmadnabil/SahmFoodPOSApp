package com.sahm.pos.screens.login.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sahm.pos.screens.login.LoginIntent
import com.sahm.pos.screens.login.LoginUiState
import com.sahm.pos.utils.ScreenType

@Composable
fun PhoneLoginContent(
    state: LoginUiState,
    screenType: ScreenType,
    onIntent: (LoginIntent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(40.dp, alignment = Alignment.CenterVertically)
    ) {
        LoginHeader(screenType = screenType)

        LoginCard(
            state = state,
            screenType = screenType,
            onIntent = onIntent,
        )

        SyncDataButton(
            onClick = { onIntent(LoginIntent.SyncClicked) },
        )
    }
}