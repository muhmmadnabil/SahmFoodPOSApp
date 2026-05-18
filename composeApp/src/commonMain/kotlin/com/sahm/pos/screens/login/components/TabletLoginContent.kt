package com.sahm.pos.screens.login.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sahm.pos.screens.login.LoginIntent
import com.sahm.pos.screens.login.LoginUiState
import com.sahm.pos.utils.ScreenType

@Composable
fun TabletLoginContent(
    state: LoginUiState,
    screenType: ScreenType,
    onIntent: (LoginIntent) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(56.dp),
    ) {
        Column(
            modifier = Modifier.weight(0.4f),
            verticalArrangement = Arrangement.Center,
        ) {
            LoginHeader(screenType = screenType)


        }

        Column(
            modifier = Modifier.weight(0.6f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LoginCard(
                state = state,
                screenType = screenType,
                onIntent = onIntent,
            )

            Spacer(modifier = Modifier.height(20.dp))

            SyncDataButton(onClick = { onIntent(LoginIntent.SyncClicked) })
        }
    }
}