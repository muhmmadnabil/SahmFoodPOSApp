package com.sahm.pos.data.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.sahm.pos.data.local.database.SahmPosDatabase
import com.sahm.pos.domain.entity.Discount
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNull

class SqlDelightLocalDataSourceDiscountsTest {
    @Test
    fun createTableAndInsertDiscounts() = runTest {
        val local = localDataSource()

        local.replaceAllDiscounts(listOf(discount()))

        assertEquals(listOf(discount()), local.getAllDiscounts())
    }

    @Test
    fun replaceAllRemovesOldData() = runTest {
        val local = localDataSource()

        local.replaceAllDiscounts(listOf(discount(id = "old", promoCode = "Old22")))
        local.replaceAllDiscounts(listOf(discount()))

        assertEquals(listOf(discount()), local.getAllDiscounts())
    }

    @Test
    fun emptyReplaceClearsDiscounts() = runTest {
        val local = localDataSource()

        local.replaceAllDiscounts(listOf(discount()))
        local.replaceAllDiscounts(emptyList())

        assertEquals(emptyList(), local.getAllDiscounts())
    }

    @Test
    fun transactionPreventsPartialSave() = runTest {
        val local = localDataSource()
        local.replaceAllDiscounts(listOf(discount()))

        runCatching {
            local.replaceAllDiscounts(listOf(discount(id = "new", promoCode = "New22"), discount(id = "", promoCode = "Bad22")))
        }

        assertEquals(listOf(discount()), local.getAllDiscounts())
    }

    @Test
    fun caseSensitivePromoSearch() = runTest {
        val local = localDataSource()

        local.replaceAllDiscounts(listOf(discount(promoCode = "Hello22")))

        assertNull(local.getDiscountByPromoCode("hello22"))
    }

    @Test
    fun exactPromoSearch() = runTest {
        val local = localDataSource()

        local.replaceAllDiscounts(listOf(discount(promoCode = "Hello22")))

        assertEquals(discount(promoCode = "Hello22"), local.getDiscountByPromoCode("Hello22"))
    }

    @Test
    fun differentCasePromoCodesCanCoexist() = runTest {
        val local = localDataSource()

        local.replaceAllDiscounts(listOf(discount(id = "a", promoCode = "Hello22"), discount(id = "b", promoCode = "hello22")))

        assertEquals(2, local.getDiscountCount())
    }

    @Test
    fun sameCaseDuplicatePromoCodeFails() = runTest {
        val local = localDataSource()

        assertFails {
            local.replaceAllDiscounts(listOf(discount(id = "a", promoCode = "Hello22"), discount(id = "b", promoCode = "Hello22")))
        }
    }

    private fun localDataSource(): SqlDelightLocalDataSource {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        SahmPosDatabase.Schema.create(driver)
        return SqlDelightLocalDataSourceImpl(SahmPosDatabase(driver))
    }

    private fun discount(
        id: String = "discount-1",
        promoCode: String = "Hello22",
    ) = Discount(id, promoCode, 10.0, 5.0, 50.0, 1_000, 3_000, 1234)
}
