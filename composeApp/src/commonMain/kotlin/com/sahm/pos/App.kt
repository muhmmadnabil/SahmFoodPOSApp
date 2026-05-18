package com.sahm.pos

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sahm.pos.components.AppTopBar
import com.sahm.pos.components.TimeIncorrectScreen
import com.sahm.pos.data.local.PlatformContext
import com.sahm.pos.di.appModules
import com.sahm.pos.domain.usecase.HasCurrentUserUseCase
import com.sahm.pos.navigation.AppNavHost
import com.sahm.pos.navigation.AppRoute
import com.sahm.pos.theme.ScreenBackground
import com.sahm.pos.utils.ScreenType
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.dsl.koinConfiguration
import sahmfoodposapp.composeapp.generated.resources.Res
import sahmfoodposapp.composeapp.generated.resources.orders_title
import sahmfoodposapp.composeapp.generated.resources.settings_title
import sahmfoodposapp.composeapp.generated.resources.sync_discounts_title
import sahmfoodposapp.composeapp.generated.resources.sync_items_title
import sahmfoodposapp.composeapp.generated.resources.sync_orders_title
import sahmfoodposapp.composeapp.generated.resources.sync_payments_title
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
        val mainViewModel = koinViewModel<MainViewModel>()
        val isTimeCorrect by mainViewModel.isTimeCorrect.collectAsStateWithLifecycle()
        CheckPhoneTimeOnLifecycle(mainViewModel)

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
        val snackbarHostState = remember { SnackbarHostState() }

        Scaffold(
            containerColor = ScreenBackground,
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            },
            topBar = {
                if (showTopBar) {
                    AppTopBar(
                        title = currentRoute.topBarTitle(),
                        onBackClick = { navController.popBackStack() },
                    )
                }
            },
        ) { innerPadding ->
            if (isTimeCorrect) {
                AppNavHost(
                    navController = navController,
                    screenType = screenType,
                    startDestination = startDestination,
                    showMessage = snackbarHostState::showMessage,
                    modifier = Modifier.padding(innerPadding),
                )
            } else {
                TimeIncorrectScreen(
                    modifier = Modifier.padding(innerPadding),
                )
            }

        }
    }
}

private suspend fun SnackbarHostState.showMessage(message: StringResource) {
    showSnackbar(message = getString(message))
}

@Composable
private fun CheckPhoneTimeOnLifecycle(viewModel: MainViewModel) {
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME,
                Lifecycle.Event.ON_PAUSE -> viewModel.checkPhoneTime()

                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

@Composable
private fun String.topBarTitle(): String =
    when (this) {
        AppRoute.Sync -> stringResource(Res.string.sync_title)
        AppRoute.Orders -> stringResource(Res.string.orders_title)
        AppRoute.Settings -> stringResource(Res.string.settings_title)
        AppRoute.SyncUsers -> stringResource(Res.string.sync_users_title)
        AppRoute.SyncItems -> stringResource(Res.string.sync_items_title)
        AppRoute.SyncDiscounts -> stringResource(Res.string.sync_discounts_title)
        AppRoute.SyncOrders -> stringResource(Res.string.sync_orders_title)
        AppRoute.SyncPayments -> stringResource(Res.string.sync_payments_title)
        else -> ""
    }
