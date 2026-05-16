package com.sahm.pos.domain.usecase

import com.sahm.pos.domain.SyncResult
import com.sahm.pos.domain.entity.Discount
import com.sahm.pos.domain.entity.MenuItem
import com.sahm.pos.domain.repository.SyncDataRepo
import com.sahm.pos.domain.entity.TimeSyncInfo
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ApplyDiscountUseCaseTest {
    @Test
    fun validDiscountUsesRawPercentage() = assertSuccess(200.0, discount(percent = 10.0, minValue = 5.0, maxValue = 50.0), 20.0, 180.0)

    @Test
    fun usesMinValueWhenRawDiscountIsLower() = assertSuccess(100.0, discount(percent = 10.0, minValue = 15.0, maxValue = 50.0), 15.0, 85.0)

    @Test
    fun usesMaxValueWhenRawDiscountIsHigher() = assertSuccess(1000.0, discount(percent = 10.0, minValue = 5.0, maxValue = 10.0), 10.0, 990.0)

    @Test
    fun discountCannotExceedOrderTotal() = assertSuccess(10.0, discount(percent = 10.0, minValue = 15.0, maxValue = 50.0), 10.0, 0.0)

    @Test
    fun promoCodeIsCaseSensitive() = runTest {
        val result = useCase(discount()).invoke("hello22", 100.0)

        assertEquals(ApplyDiscountResult.PromoCodeNotFound, result)
    }

    @Test
    fun exactPromoCodeWorks() = runTest {
        val result = useCase(discount()).invoke("Hello22", 100.0)

        assertIs<ApplyDiscountResult.Success>(result)
    }

    @Test
    fun expiredDiscountRejected() = runTest {
        val result = useCase(discount(endAt = 1_500), now = 2_000).invoke("Hello22", 100.0)

        assertEquals(ApplyDiscountResult.PromoCodeExpired, result)
    }

    @Test
    fun futureDiscountRejected() = runTest {
        val result = useCase(discount(startAt = 3_000, endAt = 4_000), now = 2_000).invoke("Hello22", 100.0)

        assertEquals(ApplyDiscountResult.PromoCodeNotStartedYet, result)
    }

    @Test
    fun activeDiscountAccepted() = runTest {
        val result = useCase(discount(startAt = 1_000, endAt = 3_000), now = 2_000).invoke("Hello22", 100.0)

        assertIs<ApplyDiscountResult.Success>(result)
    }

    @Test
    fun invalidConfigRejected() = runTest {
        val result = useCase(discount(minValue = 50.0, maxValue = 10.0)).invoke("Hello22", 100.0)

        assertEquals(ApplyDiscountResult.InvalidDiscountConfiguration, result)
    }

    private fun assertSuccess(orderTotal: Double, discount: Discount, amount: Double, total: Double) = runTest {
        val result = useCase(discount).invoke("Hello22", orderTotal)

        assertIs<ApplyDiscountResult.Success>(result)
        assertEquals(amount, result.discountAmount)
        assertEquals(total, result.totalAfterDiscount)
    }

    private fun useCase(discount: Discount?, now: Long = 2_000) =
        ApplyDiscountUseCase(
            FakeSyncDataRepo(discount),
            AppTimeProvider(FakeTimeLocalDataSource(), ClockProvider { now }),
        )

    private class FakeSyncDataRepo(private val discount: Discount?) : SyncDataRepo {
        override suspend fun hasUsers(): Boolean = false
        override suspend fun syncUsers(): SyncResult = SyncResult.EmptyRemoteData
        override suspend fun syncMenuItems(): SyncResult = SyncResult.EmptyRemoteData
        override suspend fun syncDiscounts(): SyncResult = SyncResult.EmptyRemoteData
        override suspend fun getActiveMenuItems(): List<MenuItem> = emptyList()
        override suspend fun getDiscountByPromoCode(promoCode: String): Discount? =
            discount?.takeIf { it.promoCode == promoCode }
        override suspend fun getUserCount(): Long = 0
        override suspend fun getMenuItemCount(): Long = 0
        override suspend fun getLastUsersSyncAt(): Long? = null
        override suspend fun getLastMenuItemsSyncAt(): Long? = null
    }

    private class FakeTimeLocalDataSource : TimeLocalDataSource {
        override suspend fun saveTimeSyncInfo(info: TimeSyncInfo) = Unit
        override suspend fun getTimeSyncInfo(): TimeSyncInfo? = null
    }

    private fun discount(
        promoCode: String = "Hello22",
        percent: Double = 10.0,
        minValue: Double = 5.0,
        maxValue: Double = 50.0,
        startAt: Long = 1_000,
        endAt: Long = 3_000,
    ) = Discount("discount-1", promoCode, percent, minValue, maxValue, startAt, endAt, 100)
}
