package com.sahm.pos.screens.syncDetails.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sahm.pos.components.PosIcons
import com.sahm.pos.theme.CardBackground
import com.sahm.pos.theme.ShadowColor
import com.sahm.pos.theme.SuccessBackground
import com.sahm.pos.theme.SuccessGreen
import com.sahm.pos.theme.TextPrimary
import com.sahm.pos.theme.TextSecondary
import org.jetbrains.compose.resources.stringResource
import sahmfoodposapp.composeapp.generated.resources.Res
import sahmfoodposapp.composeapp.generated.resources.sync_completed_at
import sahmfoodposapp.composeapp.generated.resources.sync_completed_successfully

@Composable
fun LastSyncCard(
    lastSyncAt: String = stringResource(Res.string.sync_completed_at),
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(10.dp),
                ambientColor = ShadowColor,
                spotColor = ShadowColor,
            ),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(SuccessBackground, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = PosIcons.CheckCircle,
                    contentDescription = null,
                    tint = SuccessGreen,
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(Modifier.width(18.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = lastSyncAt,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = stringResource(Res.string.sync_completed_successfully),
                    color = TextSecondary,
                    fontSize = 14.sp,
                )
            }
            Icon(
                imageVector = PosIcons.ChevronRight,
                contentDescription = null,
                tint = TextPrimary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
