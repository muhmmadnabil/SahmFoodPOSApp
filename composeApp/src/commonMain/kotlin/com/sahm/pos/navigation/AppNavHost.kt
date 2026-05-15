package com.sahm.pos.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sahm.pos.domain.usecase.HasCurrentUserUseCase
import com.sahm.pos.screens.home.HomeScreen
import com.sahm.pos.screens.login.LoginEffect
import com.sahm.pos.screens.login.LoginScreen
import com.sahm.pos.screens.login.LoginViewModel
import com.sahm.pos.utils.ScreenType
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AppNavHost(
    screenType: ScreenType,
    modifier: Modifier = Modifier,
    hasCurrentUserUseCase: HasCurrentUserUseCase = koinInject(),
) {
    val navController = rememberNavController()
    val hasCurrentUser by produceState<Boolean?>(
        initialValue = null,
        key1 = hasCurrentUserUseCase,
    ) {
        value = hasCurrentUserUseCase()
    }
    val startDestination = when (hasCurrentUser) {
        true -> AppRoute.Home
        false -> AppRoute.Login
        null -> return
    }

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
                        LoginEffect.NavigateToPos -> {
                            navController.navigate(AppRoute.Home) {
                                popUpTo(AppRoute.Login) { inclusive = true }
                            }
                        }

                        is LoginEffect.ShowMessage -> {

                        }
                    }
                }
            }

            LoginScreen(
                state = state,
                screenType = screenType,
                onIntent = viewModel::onIntent,
            )
        }

        composable(AppRoute.Home) {
            HomeScreen(screenType = screenType)
        }
    }
}
