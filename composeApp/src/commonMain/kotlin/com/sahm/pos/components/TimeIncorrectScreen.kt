package com.sahm.pos.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TimeIncorrectScreen(modifier: Modifier) {
    Box(modifier = modifier.fillMaxSize().background(color = Color.LightGray)) {
        Text(
            text = "Your device's time is incorrect. Please correct it to continue using the app.",
            modifier = Modifier.padding(20.dp).align(
                Alignment.Center
            ),
            textAlign = TextAlign.Center,
            fontSize = 20.sp
        )
    }
}