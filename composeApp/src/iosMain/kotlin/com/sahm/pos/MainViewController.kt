package com.sahm.pos

import androidx.compose.ui.window.ComposeUIViewController
import com.sahm.pos.data.local.PlatformContext

fun MainViewController() = ComposeUIViewController { App(PlatformContext()) }
