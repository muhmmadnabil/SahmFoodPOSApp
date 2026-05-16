package com.sahm.pos.di

import com.sahm.pos.MainViewModel
import com.sahm.pos.screens.home.HomeViewModel
import com.sahm.pos.screens.login.LoginViewModel
import com.sahm.pos.screens.syncDetails.SyncViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    viewModelOf(::MainViewModel)
    viewModelOf(::HomeViewModel)
    viewModelOf(::LoginViewModel)
    viewModelOf(::SyncViewModel)
}
