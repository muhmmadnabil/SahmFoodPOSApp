package com.sahm.pos.screens.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sahm.pos.components.PosIcons
import com.sahm.pos.theme.PrimaryOrange

@Composable
fun AddItemButton(
    isTablet: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .clip(if (isTablet) CircleShape else RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        shape = if (isTablet) CircleShape else RoundedCornerShape(10.dp),
        color = PrimaryOrange,
    ) {
        Icon(
            imageVector = PosIcons.Plus,
            contentDescription = null,
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 5.dp)
                .size(if (isTablet) 20.dp else 22.dp),
            tint = Color.White,
        )
    }
}

@Preview
@Composable
private fun AddItemButtonPreview() {
    AddItemButton(isTablet = false, onClick = {})
}
