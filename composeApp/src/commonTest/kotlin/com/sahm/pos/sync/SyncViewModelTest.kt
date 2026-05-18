package com.sahm.pos.sync

import com.sahm.pos.domain.results.SyncResult
import com.sahm.pos.domain.entity.Discount
import com.sahm.pos.domain.entity.MenuItem
import com.sahm.pos.domain.repository.SyncDataRepo
import com.sahm.pos.domain.usecase.GetDiscountsCountUseCase
import com.sahm.pos.domain.usecase.GetDiscountsLastSyncAtUseCase
import com.sahm.pos.domain.usecase.GetMenuItemsCountUseCase
import com.sahm.pos.domain.usecase.GetMenuItemsLastSyncUseCase
import com.sahm.pos.domain.usecase.GetUsersCountUseCase
import com.sahm.pos.domain.usecase.GetUsersLastSyncAtUseCase
import com.sahm.pos.domain.usecase.SyncDiscountsUseCase
import com.sahm.pos.domain.usecase.SyncMenuItemsUseCase
import com.sahm.pos.domain.usecase.SyncUsersUseCase
import com.sahm.pos.screens.sync.SyncDetailType
import com.sahm.pos.screens.syncDetails.SyncEffect
import com.sahm.pos.screens.syncDetails.SyncIntent
import com.sahm.pos.screens.syncDetails.SyncUiState
import com.sahm.pos.screens.syncDetails.SyncViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import sahmfoodposapp.composeapp.generated.resources.Res
import sahmfoodposapp.composeapp.generated.resources.sync_empty_remote_data
import sahmfoodposapp.composeapp.generated.resources.sync_items_permission_denied
import sahmfoodposapp.composeapp.generated.resources.sync_items_success
import sahmfoodposapp.composeapp.generated.resources.sync_items_success_with_warnings
import sahmfoodposapp.composeapp.generated.resources.sync_no_internet
import sahmfoodposapp.composeapp.generated.resources.sync_unknown_error
import sahmfoodposapp.composeapp.generated.resources.sync_users_success

@OptIn(ExperimentalCoroutinesApi::class)
class SyncViewModelTest {

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialStateHasIsSyncingItemsFalse() = runTest {
        val viewModel = viewModel()

        assertEquals(SyncUiState(), viewModel.state.value)
    }

    @Test
    fun screenOpenedLoadsLocalItemCountIfSupported() = runTest {
        val viewModel = viewModel(
            syncDataRepo = FakeSyncDataRepo(activeItems = listOf(item, item.copy(id = "fries"))),
        )

        viewModel.onIntent(SyncIntent.ScreenOpened(SyncDetailType.Items))
        advanceUntilIdle()

        assertEquals(2, viewModel.state.value.count)
    }

    @Test
    fun screenOpenedLoadsLocalUserCountAndLastSync() = runTest {
        val viewModel = viewModel(
            syncDataRepo = FakeSyncDataRepo(userCount = 3, lastUsersSyncAt = 1234),
        )

        viewModel.onIntent(SyncIntent.ScreenOpened(SyncDetailType.Users))
        advanceUntilIdle()

        assertEquals(3, viewModel.state.value.count)
        assertEquals(1234, viewModel.state.value.lastSyncAt)
    }

    @Test
    fun syncItemsClickedSetsIsSyncingItemsTrue() = runTest {
        val repo = FakeSyncDataRepo(waitForSync = CompletableDeferred())
        val viewModel = viewModel(syncDataRepo = repo)

        viewModel.onIntent(SyncIntent.SyncItemsClicked)
        runCurrent()

        assertTrue(viewModel.state.value.isSyncing)
        repo.waitForSync?.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun successfulSyncSetsIsSyncingItemsFalse() = runTest {
        val viewModel = viewModel()

        viewModel.onIntent(SyncIntent.SyncItemsClicked)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isSyncing)
    }

    @Test
    fun successfulSyncEmitsItemsSyncedSuccessfullyEffect() = runTest {
        val effects = mutableListOf<SyncEffect>()
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effect.toList(effects)
        }

        viewModel.onIntent(SyncIntent.SyncItemsClicked)
        advanceUntilIdle()

        assertTrue(effects.contains(SyncEffect.ShowMessage(Res.string.sync_items_success)))
    }

    @Test
    fun successfulUsersSyncEmitsUsersSyncedSuccessfullyEffect() = runTest {
        val effects = mutableListOf<SyncEffect>()
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effect.toList(effects)
        }

        viewModel.onIntent(SyncIntent.SyncUsersClicked)
        advanceUntilIdle()

        assertTrue(effects.contains(SyncEffect.ShowMessage(Res.string.sync_users_success)))
        assertFalse(viewModel.state.value.isSyncing)
    }

    @Test
    fun successfulSyncWithSkippedInvalidDocumentsEmitsWarningEffect() = runTest {
        val effects = mutableListOf<SyncEffect>()
        val viewModel = viewModel(
            syncDataRepo = FakeSyncDataRepo(SyncResult.Success(2, skippedInvalidCount = 1))
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effect.toList(effects)
        }

        viewModel.onIntent(SyncIntent.SyncItemsClicked)
        advanceUntilIdle()

        assertTrue(effects.contains(SyncEffect.ShowMessage(Res.string.sync_items_success_with_warnings)))
    }

    @Test
    fun noInternetResultShowsNoInternetEffect() = runTest {
        assertEffectFor(
            result = SyncResult.NoInternet,
            expected = SyncEffect.ShowMessage(Res.string.sync_no_internet),
        )
    }

    @Test
    fun emptyRemoteDataResultShowsSafeEmptyRemoteEffect() = runTest {
        assertEffectFor(
            result = SyncResult.EmptyRemoteData,
            expected = SyncEffect.ShowMessage(Res.string.sync_empty_remote_data),
        )
    }

    @Test
    fun permissionDeniedResultShowsPermissionDeniedEffect() = runTest {
        assertEffectFor(
            result = SyncResult.PermissionDenied,
            expected = SyncEffect.ShowMessage(Res.string.sync_items_permission_denied),
        )
    }

    @Test
    fun unknownErrorResultShowsGenericErrorEffect() = runTest {
        assertEffectFor(
            result = SyncResult.UnknownError,
            expected = SyncEffect.ShowMessage(Res.string.sync_unknown_error),
        )
    }

    @Test
    fun clickingSyncWhileAlreadySyncingDoesNotStartSecondSync() = runTest {
        val repo = FakeSyncDataRepo(waitForSync = CompletableDeferred())
        val viewModel = viewModel(syncDataRepo = repo)

        viewModel.onIntent(SyncIntent.SyncItemsClicked)
        runCurrent()
        viewModel.onIntent(SyncIntent.SyncItemsClicked)

        assertEquals(1, repo.syncMenuItemsCalls)
        repo.waitForSync?.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun retryAfterFailureStartsSyncAgain() = runTest {
        val repo = FakeSyncDataRepo(result = SyncResult.NoInternet)
        val viewModel = viewModel(syncDataRepo = repo)

        viewModel.onIntent(SyncIntent.SyncItemsClicked)
        advanceUntilIdle()
        viewModel.onIntent(SyncIntent.SyncItemsClicked)
        advanceUntilIdle()

        assertEquals(2, repo.syncMenuItemsCalls)
    }

    private suspend fun TestScope.assertEffectFor(
        result: SyncResult,
        expected: SyncEffect,
    ) {
        val effects = mutableListOf<SyncEffect>()
        val viewModel = viewModel(syncDataRepo = FakeSyncDataRepo(result))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effect.toList(effects)
        }

        viewModel.onIntent(SyncIntent.SyncItemsClicked)
        advanceUntilIdle()

        assertTrue(effects.contains(expected))
    }

    private fun TestScope.viewModel(
        syncDataRepo: FakeSyncDataRepo = FakeSyncDataRepo(),
    ): SyncViewModel {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        return SyncViewModel(
            syncMenuItemsUseCase = SyncMenuItemsUseCase(syncDataRepo),
            syncUsersUseCase = SyncUsersUseCase(syncDataRepo),
            syncDiscountsUseCase = SyncDiscountsUseCase(syncDataRepo),
            getUsersCountUseCase = GetUsersCountUseCase(syncDataRepo),
            getMenuItemsCountUseCase = GetMenuItemsCountUseCase(syncDataRepo),
            getMenuItemsLastSyncUseCase = GetMenuItemsLastSyncUseCase(syncDataRepo),
            getUsersLastSyncAtUseCase = GetUsersLastSyncAtUseCase(syncDataRepo),
            getDiscountsLastSyncAtUseCase = GetDiscountsLastSyncAtUseCase(syncDataRepo),
            getDiscountsCountUseCase = GetDiscountsCountUseCase(syncDataRepo),
        )
    }

    private class FakeSyncDataRepo(
        private val result: SyncResult = SyncResult.Success(1, 0),
        val waitForSync: CompletableDeferred<Unit>? = null,
        private val userCount: Long = 0,
        private val itemCount: Long = 0,
        private val lastUsersSyncAt: Long? = null,
        private val lastItemsSyncAt: Long? = null,
        private val activeItems: List<MenuItem> = emptyList(),
    ) : SyncDataRepo {
        var syncMenuItemsCalls = 0
        var syncUsersCalls = 0

        override suspend fun hasUsers(): Boolean = false

        override suspend fun syncUsers(): SyncResult {
            syncUsersCalls += 1
            return result
        }

        override suspend fun syncMenuItems(): SyncResult {
            syncMenuItemsCalls += 1
            waitForSync?.await()
            return result
        }

        override suspend fun syncDiscounts(): SyncResult = result

        override suspend fun getActiveMenuItems(): List<MenuItem> = activeItems

        override suspend fun getDiscountByPromoCode(promoCode: String): Discount? = null

        override suspend fun getUserCount(): Long = userCount

        override suspend fun getMenuItemCount(): Long =
            itemCount.takeIf { it > 0 } ?: activeItems.size.toLong()

        override suspend fun getLastUsersSyncAt(): Long? = lastUsersSyncAt

        override suspend fun getLastMenuItemsSyncAt(): Long? = lastItemsSyncAt
    }

    private companion object {
        val item = MenuItem(
            id = "burger_animal_style",
            category = "Beef Burgers",
            name = "Animal Style",
            description = "Crispy onion rings",
            imageUrl = "https://example.com/burger.webp",
            price = 5702,
        )
    }
}
