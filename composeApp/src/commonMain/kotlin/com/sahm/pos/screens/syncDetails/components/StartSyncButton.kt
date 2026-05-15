package com.sahm.pos.screens.syncDetails.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
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
import sahmfoodposapp.composeapp.generated.resources.sync_downloading
import sahmfoodposapp.composeapp.generated.resources.sync_start_button

@Composable
fun StartSyncButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    onStartSync: () -> Unit,
) {
    OutlinedButton(
        onClick = onStartSync,
        enabled = enabled && !isLoading,
        modifier = modifier,
        border = BorderStroke(1.dp, PrimaryOrange),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = PrimaryOrange,
        ),
        shape = RoundedCornerShape(6.dp),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = PrimaryOrange,
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = stringResource(
                    if (isLoading) {
                        Res.string.sync_downloading
                    } else {
                        Res.string.sync_start_button
                    }
                ),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
