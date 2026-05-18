package com.sahm.pos.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sahm.pos.components.PosIcons
import com.sahm.pos.screens.settings.components.CashierDetailsCard
import com.sahm.pos.screens.settings.components.SettingsActionButton
import com.sahm.pos.theme.ErrorRed
import com.sahm.pos.theme.PrimaryOrange
import com.sahm.pos.theme.ScreenBackground
import com.sahm.pos.utils.ScreenType
import org.jetbrains.compose.resources.stringResource
import sahmfoodposapp.composeapp.generated.resources.Res
import sahmfoodposapp.composeapp.generated.resources.settings_logout
import sahmfoodposapp.composeapp.generated.resources.settings_logging_out
import sahmfoodposapp.composeapp.generated.resources.settings_sync

@Composable
fun SettingsScreen(
    screenType: ScreenType,
    state: SettingsUiState,
    onIntent: (SettingsIntent) -> Unit,
) {
    val contentWidth = if (screenType == ScreenType.Tablet) 560.dp else 420.dp
    val currentUser = state.currentUser

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .padding(horizontal = if (screenType == ScreenType.Tablet) 40.dp else 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = contentWidth),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CashierDetailsCard(
                cashierName = currentUser?.username.orEmpty(),
                cashierPhone = currentUser?.phone.orEmpty(),
                cashierId = currentUser?.id.orEmpty(),
                isLoading = state.isLoading,
                modifier = Modifier.fillMaxWidth(),
            )

            SettingsActionButton(
                text = stringResource(Res.string.settings_sync),
                icon = PosIcons.Refresh,
                containerColor = PrimaryOrange,
                enabled = !state.isLoading,
                onClick = { onIntent(SettingsIntent.SyncClicked) },
            )

            SettingsActionButton(
                text = stringResource(Res.string.settings_logout),
                icon = PosIcons.LogOut,
                containerColor = ErrorRed,
                enabled = !state.isLoading,
                onClick = { onIntent(SettingsIntent.LogoutClicked) },
            )
        }
    }
}
