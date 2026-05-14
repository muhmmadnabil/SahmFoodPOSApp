package com.sahm.pos.screens.login.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sahm.pos.theme.TextPrimary
import com.sahm.pos.theme.TextSecondary
import com.sahm.pos.utils.ScreenType
import org.jetbrains.compose.resources.stringResource
import sahmfoodposapp.composeapp.generated.resources.Res
import sahmfoodposapp.composeapp.generated.resources.login_subtitle
import sahmfoodposapp.composeapp.generated.resources.login_title

@Composable
fun LoginHeader(screenType: ScreenType) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(Res.string.login_title),
            color = TextPrimary,
            fontSize = if (screenType == ScreenType.Phone) 35.sp else 50.sp,
            fontWeight = FontWeight.Bold,
            textAlign = if (screenType == ScreenType.Phone) TextAlign.Center else TextAlign.Start,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.login_subtitle),
            color = TextSecondary,
            fontSize = if (screenType == ScreenType.Phone) 18.sp else 30.sp,
            fontWeight = FontWeight.Normal,
            textAlign = if (screenType == ScreenType.Phone) TextAlign.Center else TextAlign.Start,
        )
    }
}