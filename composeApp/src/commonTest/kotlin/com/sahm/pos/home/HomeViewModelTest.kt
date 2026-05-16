package com.sahm.pos.home

import com.sahm.pos.domain.SyncResult
import com.sahm.pos.domain.entity.MenuItem
import com.sahm.pos.domain.entity.OrderType
import com.sahm.pos.domain.entity.PaymentType
import com.sahm.pos.domain.repository.SyncDataRepo
import com.sahm.pos.domain.usecase.GetMenuItemsUseCase
import com.sahm.pos.screens.home.HomeConstants
import com.sahm.pos.screens.home.HomeIntent
import com.sahm.pos.screens.home.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun screenOpenedLoadsCategoriesAndItems() = runTest {
        val viewModel = viewModel(items = menuItems)

        viewModel.onIntent(HomeIntent.ScreenOpened)
        advanceUntilIdle()

        assertEquals(listOf(HomeConstants.AllCategory, "Burgers", "Sides"), viewModel.state.value.categories)
        assertEquals(menuItems, viewModel.state.value.filteredMenuItems)
        assertEquals(HomeConstants.AllCategory, viewModel.state.value.selectedCategory)
    }

    @Test
    fun categorySelectedFiltersItems() = runTest {
        val viewModel = viewModel(items = menuItems)

        viewModel.onIntent(HomeIntent.ScreenOpened)
        advanceUntilIdle()
        viewModel.onIntent(HomeIntent.CategorySelected("Sides"))

        assertEquals(listOf(fries), viewModel.state.value.filteredMenuItems)
        assertEquals("Sides", viewModel.state.value.selectedCategory)
    }

    @Test
    fun searchChangedFiltersItemsWithinSelectedCategory() = runTest {
        val viewModel = viewModel(items = menuItems)

        viewModel.onIntent(HomeIntent.ScreenOpened)
        advanceUntilIdle()
        viewModel.onIntent(HomeIntent.CategorySelected("Burgers"))
        viewModel.onIntent(HomeIntent.SearchChanged("classic"))

        assertEquals(listOf(classicBurger), viewModel.state.value.filteredMenuItems)
    }

    @Test
    fun itemIntentsUpdateOrderAndTotals() = runTest {
        val viewModel = viewModel(items = menuItems)

        viewModel.onIntent(HomeIntent.ScreenOpened)
        advanceUntilIdle()
        viewModel.onIntent(HomeIntent.ItemAdded(classicBurger.id))
        viewModel.onIntent(HomeIntent.ItemQuantityChanged(classicBurger.id, 2))

        val state = viewModel.state.value
        assertEquals(1, state.orderItems.size)
        assertEquals(2, state.orderItems.first().quantity)
        assertEquals(11_404, state.subtotal)
        assertEquals(1_597, state.tax)
        assertEquals(13_001, state.total)
    }

    @Test
    fun itemRemovedClearsOrderTotals() = runTest {
        val viewModel = viewModel(items = menuItems)

        viewModel.onIntent(HomeIntent.ScreenOpened)
        advanceUntilIdle()
        viewModel.onIntent(HomeIntent.ItemAdded(fries.id))
        viewModel.onIntent(HomeIntent.ItemRemoved(fries.id))

        val state = viewModel.state.value
        assertTrue(state.orderItems.isEmpty())
        assertEquals(0, state.subtotal)
        assertEquals(0, state.tax)
        assertEquals(0, state.total)
    }

    @Test
    fun paymentTypeSelectedUpdatesState() = runTest {
        val viewModel = viewModel(items = menuItems)

        viewModel.onIntent(HomeIntent.PaymentTypeSelected(PaymentType.CARD))

        assertEquals(PaymentType.CARD, viewModel.state.value.selectedPaymentType)
    }

    @Test
    fun dineInOrderAddsServiceAndDiscountUpdatesTotals() = runTest {
        val viewModel = viewModel(items = menuItems)

        viewModel.onIntent(HomeIntent.ScreenOpened)
        advanceUntilIdle()
        viewModel.onIntent(HomeIntent.ItemQuantityChanged(classicBurger.id, 2))
        viewModel.onIntent(HomeIntent.OrderTypeSelected(OrderType.DINE_IN))
        viewModel.onIntent(HomeIntent.DiscountChanged("10.00"))

        val state = viewModel.state.value
        assertEquals(11_404, state.subtotal)
        assertEquals(1_000, state.discount)
        assertEquals(1_040, state.service)
        assertEquals(1_602, state.tax)
        assertEquals(13_046, state.total)
    }

    @Test
    fun makeOrderShowsPaymentPromptOnlyWhenOrderHasItems() = runTest {
        val viewModel = viewModel(items = menuItems)

        viewModel.onIntent(HomeIntent.MakeOrderClicked)
        assertEquals(false, viewModel.state.value.showPaymentPrompt)

        viewModel.onIntent(HomeIntent.ScreenOpened)
        advanceUntilIdle()
        viewModel.onIntent(HomeIntent.ItemAdded(fries.id))
        viewModel.onIntent(HomeIntent.MakeOrderClicked)

        assertEquals(true, viewModel.state.value.showPaymentPrompt)
    }

    @Test
    fun confirmPaymentClearsCurrentOrderAndClosesPrompt() = runTest {
        val viewModel = viewModel(items = menuItems)

        viewModel.onIntent(HomeIntent.ScreenOpened)
        advanceUntilIdle()
        viewModel.onIntent(HomeIntent.ItemAdded(fries.id))
        viewModel.onIntent(HomeIntent.DiscountChanged("1.50"))
        viewModel.onIntent(HomeIntent.MakeOrderClicked)
        viewModel.onIntent(HomeIntent.ConfirmPaymentClicked)

        val state = viewModel.state.value
        assertTrue(state.orderItems.isEmpty())
        assertEquals("", state.discountText)
        assertEquals(0, state.total)
        assertEquals(false, state.showPaymentPrompt)
    }

    private fun TestScope.viewModel(items: List<MenuItem>): HomeViewModel {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        return HomeViewModel(GetMenuItemsUseCase(FakeSyncDataRepo(items)))
    }

    private class FakeSyncDataRepo(
        private val activeItems: List<MenuItem>,
    ) : SyncDataRepo {
        override suspend fun hasUsers(): Boolean = false

        override suspend fun syncUsers(): SyncResult = SyncResult.Success(0, 0)

        override suspend fun syncMenuItems(): SyncResult = SyncResult.Success(0, 0)

        override suspend fun getActiveMenuItems(): List<MenuItem> = activeItems

        override suspend fun getUserCount(): Long = 0

        override suspend fun getMenuItemCount(): Long = activeItems.size.toLong()

        override suspend fun getLastUsersSyncAt(): Long? = null

        override suspend fun getLastMenuItemsSyncAt(): Long? = null
    }

    private companion object {
        val classicBurger = MenuItem(
            id = "classic_burger",
            category = "Burgers",
            name = "Classic Burger",
            description = "Beef patty with cheese",
            imageUrl = "https://example.com/classic.webp",
            price = 5_702,
        )
        val spicyBurger = MenuItem(
            id = "spicy_burger",
            category = "Burgers",
            name = "Spicy Burger",
            description = "Jalapeno and sauce",
            imageUrl = "https://example.com/spicy.webp",
            price = 6_000,
        )
        val fries = MenuItem(
            id = "fries",
            category = "Sides",
            name = "Fries",
            description = "Crispy fries",
            imageUrl = "https://example.com/fries.webp",
            price = 2_250,
        )
        val menuItems = listOf(classicBurger, spicyBurger, fries)
    }
}
