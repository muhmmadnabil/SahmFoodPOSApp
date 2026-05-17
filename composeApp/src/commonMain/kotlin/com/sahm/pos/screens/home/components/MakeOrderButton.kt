package com.sahm.pos.screens.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sahm.pos.theme.PrimaryOrange
import org.jetbrains.compose.resources.stringResource
import sahmfoodposapp.composeapp.generated.resources.Res
import sahmfoodposapp.composeapp.generated.resources.home_make_order

@Composable
fun MakeOrderButton(
    isTablet: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(if (isTablet) 8.dp else 10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = PrimaryOrange,
            disabledContainerColor = PrimaryOrange.copy(alpha = 0.72f),
        ),
    ) {
        Text(
            text = stringResource(Res.string.home_make_order),
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
