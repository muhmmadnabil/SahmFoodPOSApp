package com.sahm.pos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.window.core.layout.WindowSizeClass
import com.sahm.pos.data.local.PlatformContext
import com.sahm.pos.utils.ScreenType

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            App(
                platformContext = PlatformContext(applicationContext),
                screenType = currentScreenType(),
            )
        }
    }
}

@Composable
private fun currentScreenType(): ScreenType {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass

    return if (windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)) {
        ScreenType.Tablet
    } else {
        ScreenType.Phone
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App(PlatformContext(LocalContext.current))
}