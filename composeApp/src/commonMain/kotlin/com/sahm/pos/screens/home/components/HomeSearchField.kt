package com.sahm.pos.screens.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sahm.pos.theme.BorderDefault
import com.sahm.pos.theme.CardBackground
import com.sahm.pos.theme.TextPrimary
import com.sahm.pos.theme.TextSecondary
import org.jetbrains.compose.resources.stringResource
import sahmfoodposapp.composeapp.generated.resources.Res
import sahmfoodposapp.composeapp.generated.resources.home_search_label

@Composable
fun HomeSearchField(
    searchText: String,
    placeholder: String,
    textFontSize: Int,
    modifier: Modifier = Modifier,
    onSearchChanged: (String) -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(if (textFontSize == 18) 14.dp else 10.dp),
        color = CardBackground,
        border = BorderStroke(1.dp, BorderDefault),
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = if (textFontSize == 18) 14.dp else 12.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(Res.string.home_search_label),
                color = TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            BasicTextField(
                value = searchText,
                onValueChange = onSearchChanged,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = TextStyle(
                    color = TextPrimary,
                    fontSize = textFontSize.sp,
                ),
                decorationBox = { innerTextField ->
                    if (searchText.isBlank()) {
                        Text(
                            text = placeholder,
                            color = TextSecondary,
                            fontSize = textFontSize.sp,
                        )
                    }
                    innerTextField()
                },
            )
        }
    }
}