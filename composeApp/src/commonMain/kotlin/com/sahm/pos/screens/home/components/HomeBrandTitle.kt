package com.sahm.pos.screens.home.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.sahm.pos.theme.PrimaryOrange
import com.sahm.pos.theme.TextPrimary
import org.jetbrains.compose.resources.stringResource
import sahmfoodposapp.composeapp.generated.resources.Res
import sahmfoodposapp.composeapp.generated.resources.home_pos_highlight
import sahmfoodposapp.composeapp.generated.resources.home_pos_title_suffix

@Composable
fun HomeBrandTitle(
    modifier: Modifier = Modifier,
    fontSize: Int,
) {
    val highlight = stringResource(Res.string.home_pos_highlight)
    val suffix = stringResource(Res.string.home_pos_title_suffix)

    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = PrimaryOrange)) { append(highlight) }
            append(suffix)
        },
        modifier = modifier,
        color = TextPrimary,
        fontSize = fontSize.sp,
        fontWeight = FontWeight.Bold,
    )
}

@Preview
@Composable
private fun HomeBrandTitlePreview() {
    HomeBrandTitle(fontSize = 20)
}