package com.sahm.pos.home

import com.sahm.pos.domain.SyncResult
import com.sahm.pos.domain.entity.Discount
import com.sahm.pos.domain.entity.MenuItem
import com.sahm.pos.domain.entity.OrderType
import com.sahm.pos.domain.entity.PaymentType
import com.sahm.pos.domain.entity.TimeSyncInfo
import com.sahm.pos.domain.repository.SyncDataRepo
import com.sahm.pos.domain.usecase.AppTimeProvider
import com.sahm.pos.domain.usecase.ApplyDiscountUseCase
import com.sahm.pos.domain.usecase.ClockProvider
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
        val viewModel = viewModel(
            items = menuItems,
            discounts = listOf(discount(promoCode = "SAVE10", percent = 10.0)),
        )

        viewModel.onIntent(HomeIntent.ScreenOpened)
        advanceUntilIdle()
        viewModel.onIntent(HomeIntent.ItemQuantityChanged(classicBurger.id, 2))
        viewModel.onIntent(HomeIntent.OrderTypeSelected(OrderType.DINE_IN))
        viewModel.onIntent(HomeIntent.DiscountChanged("SAVE10"))
        viewModel.onIntent(HomeIntent.DiscountSubmitted)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(11_404, state.subtotal)
        assertEquals(1_140, state.discount)
        assertEquals(1_026, state.service)
        assertEquals(1_581, state.tax)
        assertEquals(12_871, state.total)
        assertEquals(10.0, state.appliedDiscount?.percent)
        assertEquals("", state.discountText)
        assertEquals(false, state.isApplyingDiscount)
    }

    @Test
    fun discountChangedDoesNotSearchForPromoCodeUntilSubmitted() = runTest {
        val repo = FakeSyncDataRepo(menuItems, listOf(discount(promoCode = "SAVE10")))
        val viewModel = viewModel(repo)

        viewModel.onIntent(HomeIntent.DiscountChanged("SAVE10"))
        advanceUntilIdle()

        assertEquals(0, repo.discountLookupCount)
        assertEquals(null, viewModel.state.value.appliedDiscount)
    }

    @Test
    fun appliedPromoDiscountRecalculatesWhenItemsChange() = runTest {
        val viewModel = viewModel(
            items = menuItems,
            discounts = listOf(discount(promoCode = "SAVE10", percent = 10.0)),
        )

        viewModel.onIntent(HomeIntent.ScreenOpened)
        advanceUntilIdle()
        viewModel.onIntent(HomeIntent.ItemAdded(classicBurger.id))
        viewModel.onIntent(HomeIntent.DiscountChanged("SAVE10"))
        viewModel.onIntent(HomeIntent.DiscountSubmitted)
        advanceUntilIdle()

        assertEquals(570, viewModel.state.value.discount)

        viewModel.onIntent(HomeIntent.ItemQuantityChanged(classicBurger.id, 2))

        assertEquals(1_140, viewModel.state.value.discount)
    }

    @Test
    fun submittingNewValidPromoOverridesPreviousDiscount() = runTest {
        val viewModel = viewModel(
            items = menuItems,
            discounts = listOf(
                discount(promoCode = "SAVE10", percent = 10.0),
                discount(id = "discount-2", promoCode = "SAVE20", percent = 20.0),
            ),
        )

        viewModel.onIntent(HomeIntent.ScreenOpened)
        advanceUntilIdle()
        viewModel.onIntent(HomeIntent.ItemQuantityChanged(classicBurger.id, 2))
        viewModel.onIntent(HomeIntent.DiscountChanged("SAVE10"))
        viewModel.onIntent(HomeIntent.DiscountSubmitted)
        advanceUntilIdle()
        viewModel.onIntent(HomeIntent.DiscountChanged("SAVE20"))
        viewModel.onIntent(HomeIntent.DiscountSubmitted)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("", state.discountText)
        assertEquals(20.0, state.appliedDiscount?.percent)
        assertEquals(2_281, state.discount)
        assertEquals(false, state.isApplyingDiscount)
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

    private fun TestScope.viewModel(
        items: List<MenuItem>,
        discounts: List<Discount> = emptyList(),
    ): HomeViewModel = viewModel(FakeSyncDataRepo(items, discounts))

    private fun TestScope.viewModel(repo: FakeSyncDataRepo): HomeViewModel {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        return HomeViewModel(
            getMenuItemsUseCase = GetMenuItemsUseCase(repo),
            applyDiscountUseCase = ApplyDiscountUseCase(
                syncDataRepo = repo,
                appTimeProvider = AppTimeProvider(repo, ClockProvider { 2_000 }),
            ),
        )
    }

    private class FakeSyncDataRepo(
        private val activeItems: List<MenuItem>,
        private val discounts: List<Discount> = emptyList(),
    ) : SyncDataRepo {
        var discountLookupCount = 0
            private set

        override suspend fun hasUsers(): Boolean = false

        override suspend fun syncUsers(): SyncResult = SyncResult.Success(0, 0)

        override suspend fun syncMenuItems(): SyncResult = SyncResult.Success(0, 0)

        override suspend fun syncDiscounts(): SyncResult = SyncResult.Success(0, 0)

        override suspend fun getActiveMenuItems(): List<MenuItem> = activeItems

        override suspend fun getDiscountByPromoCode(promoCode: String): Discount? {
            discountLookupCount += 1
            return discounts.firstOrNull { it.promoCode == promoCode }
        }

        override suspend fun getUserCount(): Long = 0

        override suspend fun getMenuItemCount(): Long = activeItems.size.toLong()

        override suspend fun getLastUsersSyncAt(): Long? = null

        override suspend fun getLastMenuItemsSyncAt(): Long? = null

        override suspend fun saveTimeSyncInfo(info: TimeSyncInfo) = Unit

        override suspend fun getTimeSyncInfo(): TimeSyncInfo? = null

        override suspend fun getServerTimeStamp(): Long? = null

        override suspend fun getLastDiscountsSyncAt(): Long? = null

        override suspend fun getDiscountsCount(): Int = discounts.size
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

        fun discount(
            id: String = "discount-1",
            promoCode: String = "SAVE10",
            percent: Double = 10.0,
            minValue: Double = 0.0,
            maxValue: Double = 50.0,
            startAt: Long = 1_000,
            endAt: Long = 3_000,
        ) = Discount(id, promoCode, percent, minValue, maxValue, startAt, endAt, 2_000)
    }
}
