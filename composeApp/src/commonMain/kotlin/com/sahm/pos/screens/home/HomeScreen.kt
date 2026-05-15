package com.sahm.pos.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.stringResource
import sahmfoodposapp.composeapp.generated.resources.Res
import sahmfoodposapp.composeapp.generated.resources.login_pos_placeholder

@Composable
fun HomeScreen(
    state: HomeUiState = HomeUiState(),
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeContentPadding(),
        contentAlignment = Alignment.Center,
    ) {
        if (state.menuItems.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.menuItems, key = { it.id }) { item ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        AsyncImage(
                            model = item.localImageUrl ?: item.imageUrl,
                            contentDescription = item.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentScale = ContentScale.Crop,
                        )
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = item.category,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = item.price.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        } else {
        Text(
            text = stringResource(Res.string.login_pos_placeholder),
            style = MaterialTheme.typography.headlineMedium,
        )
        }
    }
}
