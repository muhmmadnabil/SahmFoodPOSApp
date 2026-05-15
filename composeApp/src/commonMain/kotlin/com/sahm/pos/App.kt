package com.sahm.pos

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sahm.pos.components.AppTopBar
import com.sahm.pos.data.local.PlatformContext
import com.sahm.pos.di.appModules
import com.sahm.pos.domain.usecase.HasCurrentUserUseCase
import com.sahm.pos.navigation.AppNavHost
import com.sahm.pos.navigation.AppRoute
import com.sahm.pos.theme.ScreenBackground
import com.sahm.pos.utils.ScreenType
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.dsl.koinConfiguration
import sahmfoodposapp.composeapp.generated.resources.Res
import sahmfoodposapp.composeapp.generated.resources.sync_items_title
import sahmfoodposapp.composeapp.generated.resources.sync_title
import sahmfoodposapp.composeapp.generated.resources.sync_users_title

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
        val hasCurrentUserUseCase: HasCurrentUserUseCase = koinInject()
        val hasCurrentUser by produceState<Boolean?>(
            initialValue = null,
            key1 = hasCurrentUserUseCase,
        ) {
            value = hasCurrentUserUseCase()
        }

        val startDestination = when (hasCurrentUser) {
            true -> AppRoute.Home
            false -> AppRoute.Login
            null -> return@KoinApplication
        }
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route ?: startDestination
        val showTopBar = currentRoute !in setOf(AppRoute.Login, AppRoute.Home)

        Scaffold(
            containerColor = ScreenBackground,
            topBar = {
                if (showTopBar) {
                    AppTopBar(
                        title = currentRoute.topBarTitle(),
                        onBackClick = { navController.popBackStack() },
                    )
                }
            },
        ) { innerPadding ->
            AppNavHost(
                navController = navController,
                screenType = screenType,
                startDestination = startDestination,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun String.topBarTitle(): String =
    when (this) {
        AppRoute.Sync -> stringResource(Res.string.sync_title)
        AppRoute.SyncUsers -> stringResource(Res.string.sync_users_title)
        AppRoute.SyncItems -> stringResource(Res.string.sync_items_title)
        else -> ""
    }
