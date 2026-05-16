package com.sahm.pos.screens.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sahm.pos.theme.BorderDefault
import com.sahm.pos.theme.CardBackground
import com.sahm.pos.theme.TextPrimary
import org.jetbrains.compose.resources.stringResource
import sahmfoodposapp.composeapp.generated.resources.Res
import sahmfoodposapp.composeapp.generated.resources.home_settings

@Composable
fun SettingsButton(
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
    onSettingsClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(enabled = true, onClick = onSettingsClick),
        shape = RoundedCornerShape(14.dp),
        color = CardBackground,
        border = BorderStroke(1.dp, BorderDefault),
    ) {
        Text(
            text = stringResource(Res.string.home_settings),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = textAlign,
        )
    }
}

@Preview
@Composable
private fun SettingsButtonPreview() {
    SettingsButton(onSettingsClick = {})
}