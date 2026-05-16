package com.sahm.pos.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.sahm.pos.screens.home.HomeEffect
import com.sahm.pos.screens.home.HomeIntent
import com.sahm.pos.screens.home.HomeScreen
import com.sahm.pos.screens.home.HomeViewModel
import com.sahm.pos.screens.login.LoginEffect
import com.sahm.pos.screens.login.LoginScreen
import com.sahm.pos.screens.login.LoginViewModel
import com.sahm.pos.screens.orders.OrdersIntent
import com.sahm.pos.screens.orders.OrdersScreen
import com.sahm.pos.screens.orders.OrdersViewModel
import com.sahm.pos.screens.sync.SyncDetailType
import com.sahm.pos.screens.sync.SyncScreen
import com.sahm.pos.screens.syncDetails.SyncDetailsScreen
import com.sahm.pos.screens.syncDetails.SyncEffect
import com.sahm.pos.screens.syncDetails.SyncIntent
import com.sahm.pos.screens.syncDetails.SyncViewModel
import com.sahm.pos.utils.ScreenType
import org.jetbrains.compose.resources.StringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AppNavHost(
    navController: NavHostController,
    screenType: ScreenType,
    startDestination: String,
    showMessage: suspend (StringResource) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentShowMessage by rememberUpdatedState(showMessage)

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable(AppRoute.Login) {
            val viewModel = koinViewModel<LoginViewModel>()
            val state by viewModel.state.collectAsStateWithLifecycle()

            LaunchedEffect(Unit) {
                viewModel.effect.collect { effect ->
                    when (effect) {
                        LoginEffect.NavigateToHome -> {
                            navController.navigate(AppRoute.Home) {
                                popUpTo(AppRoute.Login) { inclusive = true }
                            }
                        }

                        is LoginEffect.ShowMessage -> {
                            currentShowMessage(effect.message)
                        }

                        LoginEffect.NavigateToSync -> {
                            navController.navigate(AppRoute.Sync)
                        }
                    }
                }
            }

            LoginScreen(
                state = state,
                screenType = screenType,
                onIntent = viewModel::onIntent
            )
        }

        composable(AppRoute.Home) {
            val viewModel = koinViewModel<HomeViewModel>()
            val state by viewModel.state.collectAsStateWithLifecycle()

            LaunchedEffect(Unit) {
                viewModel.onIntent(HomeIntent.ScreenOpened)
            }

            LaunchedEffect(Unit) {
                viewModel.effect.collect { effect ->
                    when (effect) {
                        HomeEffect.NavigateToOrders -> {
                            navController.navigate(AppRoute.Orders)
                        }

                        HomeEffect.NavigateToSettings -> {}
                        is HomeEffect.ShowMessage -> {
                            currentShowMessage(effect.message)
                        }
                    }
                }
            }

            HomeScreen(
                screenType = screenType,
                state = state,
                onIntent = viewModel::onIntent,
            )
        }

        composable(AppRoute.Orders) {
            val viewModel = koinViewModel<OrdersViewModel>()
            val state by viewModel.state.collectAsStateWithLifecycle()

            LaunchedEffect(Unit) {
                viewModel.onIntent(OrdersIntent.ScreenOpened)
            }

            OrdersScreen(
                screenType = screenType,
                state = state,
                onIntent = viewModel::onIntent,
            )
        }

        composable(AppRoute.Sync) {
            SyncScreen(
                screenType = screenType,
                onUsersClick = { navController.navigate(AppRoute.SyncUsers) },
                onItemsClick = { navController.navigate(AppRoute.SyncItems) },
                onDiscountsClick = { navController.navigate(AppRoute.SyncDiscounts) },
            )
        }

        composable(AppRoute.SyncUsers) {
            val viewModel = koinViewModel<SyncViewModel>()
            val state by viewModel.state.collectAsStateWithLifecycle()

            LaunchedEffect(Unit) {
                viewModel.onIntent(SyncIntent.ScreenOpened(SyncDetailType.Users))
            }

            LaunchedEffect(Unit) {
                viewModel.effect.collect { effect ->
                    when (effect) {
                        is SyncEffect.ShowMessage -> {
                            currentShowMessage(effect.message)
                        }
                    }
                }
            }

            SyncDetailsScreen(
                screenType = screenType,
                type = SyncDetailType.Users,
                state = state,
                onIntent = viewModel::onIntent,
            )
        }

        composable(AppRoute.SyncItems) {
            val viewModel = koinViewModel<SyncViewModel>()
            val state by viewModel.state.collectAsStateWithLifecycle()

            LaunchedEffect(Unit) {
                viewModel.onIntent(SyncIntent.ScreenOpened(SyncDetailType.Items))
            }

            LaunchedEffect(Unit) {
                viewModel.effect.collect { effect ->
                    when (effect) {
                        is SyncEffect.ShowMessage -> {
                            currentShowMessage(effect.message)
                        }
                    }
                }
            }

            SyncDetailsScreen(
                screenType = screenType,
                type = SyncDetailType.Items,
                state = state,
                onIntent = viewModel::onIntent,
            )
        }

        composable(AppRoute.SyncDiscounts) {
            val viewModel = koinViewModel<SyncViewModel>()
            val state by viewModel.state.collectAsStateWithLifecycle()

            LaunchedEffect(Unit) {
                viewModel.onIntent(SyncIntent.ScreenOpened(SyncDetailType.Discounts))
            }

            LaunchedEffect(Unit) {
                viewModel.effect.collect { effect ->
                    when (effect) {
                        is SyncEffect.ShowMessage -> {
                            currentShowMessage(effect.message)
                        }
                    }
                }
            }

            SyncDetailsScreen(
                screenType = screenType,
                type = SyncDetailType.Discounts,
                state = state,
                onIntent = viewModel::onIntent,
            )
        }
    }
}
