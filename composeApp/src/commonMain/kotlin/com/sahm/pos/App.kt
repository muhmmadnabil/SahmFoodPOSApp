package com.sahm.pos

import androidx.compose.runtime.Composable
import com.sahm.pos.data.local.PlatformContext
import com.sahm.pos.di.appModules
import com.sahm.pos.navigation.AppNavHost
import com.sahm.pos.utils.ScreenType
import org.koin.compose.KoinApplication
import org.koin.dsl.koinConfiguration

@Composable
fun App(
    platformContext: PlatformContext,
    screenType: ScreenType = ScreenType.Phone,
) {
    KoinApplication(
        configuration = koinConfiguration {
            modules(appModules(platformContext))
        },
    ) {
        AppNavHost(screenType = screenType)
    }
}
