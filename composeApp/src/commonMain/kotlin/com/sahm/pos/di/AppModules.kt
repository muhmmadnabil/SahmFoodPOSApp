package com.sahm.pos.di

import com.sahm.pos.data.di.dataModule
import com.sahm.pos.data.local.PlatformContext

fun appModules(platformContext: PlatformContext) = listOf(
    dataModule(platformContext),
    viewModelModule,
)
