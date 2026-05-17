package com.sahm.pos.di

import com.sahm.pos.MainViewModel
import com.sahm.pos.screens.home.HomeViewModel
import com.sahm.pos.screens.login.LoginViewModel
import com.sahm.pos.screens.orders.OrdersViewModel
import com.sahm.pos.screens.syncDetails.SyncViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { MainViewModel(get(), get()) }
    viewModel {
        HomeViewModel(
            getMenuItemsUseCase = get(),
            applyDiscountUseCase = get(),
            createOrderUseCase = get(),
            payOrderByCashUseCase = get(),
            payOrderByCardUseCase = get(),
            retryPrintOrderReceiptUseCase = get(),
        )
    }
    viewModel { LoginViewModel(get(), get()) }
    viewModelOf(::OrdersViewModel)
    viewModel {
        SyncViewModel(
            syncMenuItemsUseCase = get(),
            syncUsersUseCase = get(),
            syncDiscountsUseCase = get(),
            getUsersCountUseCase = get(),
            getMenuItemsCountUseCase = get(),
            getMenuItemsLastSyncUseCase = get(),
            getUsersLastSyncAtUseCase = get(),
            getDiscountsLastSyncAtUseCase = get(),
            getDiscountsCountUseCase = get(),
            getSyncOutboxCountsUseCase = get(),
            manualSyncOutboxUseCase = get(),
        )
    }
}
