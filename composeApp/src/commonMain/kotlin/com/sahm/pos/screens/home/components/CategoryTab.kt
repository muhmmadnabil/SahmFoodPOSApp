package com.sahm.pos.screens.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sahm.pos.screens.home.HomeConstants
import com.sahm.pos.theme.BorderDefault
import com.sahm.pos.theme.CardBackground
import com.sahm.pos.theme.PrimaryOrange
import com.sahm.pos.theme.TextPrimary
import org.jetbrains.compose.resources.stringResource
import sahmfoodposapp.composeapp.generated.resources.Res
import sahmfoodposapp.composeapp.generated.resources.home_all_category

@Composable
fun CategoryTab(
    category: String,
    isSelected: Boolean,
    isTablet: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(if (isTablet) 8.dp else 12.dp),
        color = if (isSelected) PrimaryOrange else CardBackground,
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) PrimaryOrange else BorderDefault,
        ),
    ) {
        Text(
            text = categoryLabel(category),
            modifier = Modifier.padding(
                horizontal = if (isTablet) 28.dp else 18.dp,
                vertical = if (isTablet) 12.dp else 11.dp,
            ),
            color = if (isSelected) Color.White else TextPrimary,
            fontSize = if (isTablet) 14.sp else 16.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun categoryLabel(category: String): String =
    if (category == HomeConstants.AllCategory) {
        stringResource(Res.string.home_all_category)
    } else {
        category
    }