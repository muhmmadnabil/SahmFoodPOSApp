package com.sahm.pos.data.di

import com.sahm.pos.data.UUIDProviderImpl
import com.sahm.pos.data.local.DataStoreLocalDataSource
import com.sahm.pos.data.local.DataStorePref
import com.sahm.pos.data.local.PlatformContext
import com.sahm.pos.data.local.SqlDelightLocalDataSource
import com.sahm.pos.data.local.SqlDelightLocalDataSourceImpl
import com.sahm.pos.data.local.createCurrentUserDataStore
import com.sahm.pos.data.local.createDatabaseDriver
import com.sahm.pos.data.local.database.SahmPosDatabase
import com.sahm.pos.data.printing.createReceiptPrinter
import com.sahm.pos.data.remote.RemoteDataSource
import com.sahm.pos.data.remote.TimeRemoteDataSource
import com.sahm.pos.data.remote.TimeRemoteDataSourceImpl
import com.sahm.pos.data.remote.createRemoteDataSource
import com.sahm.pos.data.remote.image.createMenuItemImageCache
import com.sahm.pos.data.repo.AuthRepoImpl
import com.sahm.pos.data.repo.OrderRepoImpl
import com.sahm.pos.data.repo.SyncDataRepoImpl
import com.sahm.pos.data.sync.SyncOutboxProcessorImpl
import com.sahm.pos.data.sync.createNetworkMonitor
import com.sahm.pos.data.sync.createSyncScheduler
import com.sahm.pos.domain.ClockProvider
import com.sahm.pos.domain.CurrentEpochMillisProvider
import com.sahm.pos.domain.CurrentTimestampProvider
import com.sahm.pos.domain.FakePaymentGateway
import com.sahm.pos.domain.PaymentGateway
import com.sahm.pos.domain.ReceiptPrinter
import com.sahm.pos.domain.SystemClockProvider
import com.sahm.pos.domain.SystemCurrentEpochMillisProvider
import com.sahm.pos.domain.SystemCurrentTimestampProvider
import com.sahm.pos.domain.UUIDProvider
import com.sahm.pos.domain.repository.AuthRepo
import com.sahm.pos.domain.repository.OrderRepo
import com.sahm.pos.domain.repository.SyncDataRepo
import com.sahm.pos.domain.sync.NetworkMonitor
import com.sahm.pos.domain.sync.SyncOutboxProcessor
import com.sahm.pos.domain.sync.SyncScheduler
import com.sahm.pos.domain.usecase.ApplyDiscountUseCase
import com.sahm.pos.domain.usecase.CheckPhoneTimeUseCase
import com.sahm.pos.domain.usecase.CreateOrderUseCase
import com.sahm.pos.domain.usecase.CreateRefundUseCase
import com.sahm.pos.domain.usecase.GetAppTimeUseCase
import com.sahm.pos.domain.usecase.GetCurrentUserUseCase
import com.sahm.pos.domain.usecase.GetDiscountsCountUseCase
import com.sahm.pos.domain.usecase.GetDiscountsLastSyncAtUseCase
import com.sahm.pos.domain.usecase.GetMenuItemsCountUseCase
import com.sahm.pos.domain.usecase.GetMenuItemsLastSyncUseCase
import com.sahm.pos.domain.usecase.GetMenuItemsUseCase
import com.sahm.pos.domain.usecase.GetOrderDetailsUseCase
import com.sahm.pos.domain.usecase.GetOrderSyncStatsUseCase
import com.sahm.pos.domain.usecase.GetOrdersUseCase
import com.sahm.pos.domain.usecase.GetPaymentSyncStatsUseCase
import com.sahm.pos.domain.usecase.GetRefundableItemsUseCase
import com.sahm.pos.domain.usecase.GetSyncOutboxCountsUseCase
import com.sahm.pos.domain.usecase.GetUsersCountUseCase
import com.sahm.pos.domain.usecase.GetUsersLastSyncAtUseCase
import com.sahm.pos.domain.usecase.HasCurrentUserUseCase
import com.sahm.pos.domain.usecase.HasUsersUseCase
import com.sahm.pos.domain.usecase.LoginUseCase
import com.sahm.pos.domain.usecase.LogoutUseCase
import com.sahm.pos.domain.usecase.ManualSyncOutboxUseCase
import com.sahm.pos.domain.usecase.ObserveSyncTriggersUseCase
import com.sahm.pos.domain.usecase.PayOrderByCardUseCase
import com.sahm.pos.domain.usecase.PayOrderByCashUseCase
import com.sahm.pos.domain.usecase.ProcessSyncOutboxUseCase
import com.sahm.pos.domain.usecase.RefundByCardUseCase
import com.sahm.pos.domain.usecase.RefundByCashUseCase
import com.sahm.pos.domain.usecase.RetryPrintOrderReceiptUseCase
import com.sahm.pos.domain.usecase.RetryPrintRefundReceiptUseCase
import com.sahm.pos.domain.usecase.ScheduleSyncIfPendingUseCase
import com.sahm.pos.domain.usecase.SyncDiscountsUseCase
import com.sahm.pos.domain.usecase.SyncMenuItemsUseCase
import com.sahm.pos.domain.usecase.SyncPendingOutboxUseCase
import com.sahm.pos.domain.usecase.SyncUsersUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.qualifier.named
import org.koin.dsl.module

private const val PAYMENT_PRINT_SCOPE = "paymentPrintScope"

fun dataModule(platformContext: PlatformContext) = module {
    single { SahmPosDatabase(createDatabaseDriver(platformContext)) }
    single { createCurrentUserDataStore(platformContext) }
    single<SqlDelightLocalDataSource> { SqlDelightLocalDataSourceImpl(get()) }
    single<DataStoreLocalDataSource> { DataStorePref(get()) }
    single<TimeRemoteDataSource> { TimeRemoteDataSourceImpl() }
    single<RemoteDataSource> { createRemoteDataSource() }
    single { createMenuItemImageCache(platformContext) }
    single<AuthRepo> { AuthRepoImpl(get(), get()) }
    single<OrderRepo> { OrderRepoImpl(get()) }
    single<SyncScheduler> { createSyncScheduler(platformContext) }
    single<NetworkMonitor> { createNetworkMonitor(platformContext) }
    single<SyncOutboxProcessor> { SyncOutboxProcessorImpl(get(), get()) }
    single { SyncDataRepoImpl(get(), get(), get(), get(), get(), get()) }
    single<SyncDataRepo> { get<SyncDataRepoImpl>() }
    single<CurrentTimestampProvider> { SystemCurrentTimestampProvider() }
    single<CurrentEpochMillisProvider> { SystemCurrentEpochMillisProvider() }
    single<ClockProvider> { SystemClockProvider() }
    single<UUIDProvider> { UUIDProviderImpl() }
    single<PaymentGateway> { FakePaymentGateway() }
    single<ReceiptPrinter> { createReceiptPrinter(platformContext, get()) }
    single(named(PAYMENT_PRINT_SCOPE)) { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    single { GetAppTimeUseCase(get(), get()) }
    factory { HasCurrentUserUseCase(get()) }
    factory { GetCurrentUserUseCase(get()) }
    factory { LogoutUseCase(get()) }
    factory { HasUsersUseCase(get()) }
    factory { GetMenuItemsUseCase(get()) }
    factory { CreateOrderUseCase(get(), get(), get(), get(), get()) }
    factory { PayOrderByCashUseCase(get(), get(), get(), get(), get(), get(named(PAYMENT_PRINT_SCOPE))) }
    factory { PayOrderByCardUseCase(get(), get(), get(), get(), get(), get(), get(named(PAYMENT_PRINT_SCOPE))) }
    factory { RetryPrintOrderReceiptUseCase(get(), get(), get()) }
    factory { GetOrderDetailsUseCase(get()) }
    factory { GetOrdersUseCase(get()) }
    factory { CreateRefundUseCase(get(), get(), get(), get()) }
    factory { RefundByCashUseCase(get(), get(), get(), get()) }
    factory { RefundByCardUseCase(get(), get(), get(), get(), get()) }
    factory { RetryPrintRefundReceiptUseCase(get(), get()) }
    factory { GetRefundableItemsUseCase(get()) }
    factory { LoginUseCase(get(), get()) }
    factory { ApplyDiscountUseCase(get(), get()) }
    factory { CheckPhoneTimeUseCase(get(), get()) }
    factory { SyncDiscountsUseCase(get()) }
    factory { SyncMenuItemsUseCase(get()) }
    factory { SyncUsersUseCase(get()) }
    factory { GetDiscountsCountUseCase(get()) }
    factory { GetDiscountsLastSyncAtUseCase(get()) }
    factory { GetUsersLastSyncAtUseCase(get()) }
    factory { GetUsersCountUseCase(get()) }
    factory { GetMenuItemsCountUseCase(get()) }
    factory { GetMenuItemsLastSyncUseCase(get()) }
    factory { GetSyncOutboxCountsUseCase(get()) }
    factory { GetOrderSyncStatsUseCase(get()) }
    factory { GetPaymentSyncStatsUseCase(get()) }
    factory { ScheduleSyncIfPendingUseCase(get(), get()) }
    factory { ManualSyncOutboxUseCase(get()) }
    factory { ProcessSyncOutboxUseCase(get()) }
    factory<SyncPendingOutboxUseCase> {
        val repo = get<SyncDataRepo>()
        SyncPendingOutboxUseCase(get()) { repo.getCountSyncItemsPending() }
    }
    single { ObserveSyncTriggersUseCase(get(), get()) }
}
