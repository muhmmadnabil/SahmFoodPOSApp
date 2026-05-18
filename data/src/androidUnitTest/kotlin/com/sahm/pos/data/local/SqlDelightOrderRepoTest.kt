package com.sahm.pos.data.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.sahm.pos.data.local.database.SahmPosDatabase
import com.sahm.pos.data.repo.OrderRepoImpl
import com.sahm.pos.domain.entity.MenuItem
import com.sahm.pos.domain.entity.Order
import com.sahm.pos.domain.entity.OrderItem
import com.sahm.pos.domain.entity.OrderStatus
import com.sahm.pos.domain.entity.PaymentStatus
import com.sahm.pos.domain.entity.PrintStatus
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SqlDelightOrderRepoTest {

    @Test
    fun insertsAndReadsOrderAggregateWithItemSnapshots() = runTest {
        val database = database()
        val repository = OrderRepoImpl(database)
        SqlDelightLocalDataSourceImpl(database).replaceMenuItemsSnapshot(listOf(menuItem))

        repository.createOrder(order, listOf(orderItem))

        val details = repository.getOrderDetails(order.id)
        assertEquals(order, details?.order)
        assertEquals(listOf(orderItem), details?.items)
        assertEquals(menuItem, repository.getMenuItemById(menuItem.id))
    }

    @Test
    fun updatesStatusesIndependently() = runTest {
        val database = database()
        val repository = OrderRepoImpl(database)
        repository.createOrder(order, listOf(orderItem))

        repository.updateOrderAfterPayment(order.id, OrderStatus.Paid, PaymentStatus.Paid, 2_000)
        repository.updateOrderPrintStatus(order.id, PrintStatus.Failed)

        val saved = repository.getOrderDetails(order.id)?.order
        assertEquals(OrderStatus.Paid, saved?.orderStatus)
        assertEquals(PaymentStatus.Paid, saved?.paymentStatus)
        assertEquals(PrintStatus.Failed, saved?.printStatus)
        assertEquals(2_000, saved?.paidAt)
    }

    @Test
    fun transactionRollsBackWhenOrderItemInsertFails() = runTest {
        val repository = OrderRepoImpl(database())

        runCatching {
            repository.createOrder(
                order,
                listOf(orderItem, orderItem.copy(id = orderItem.id, name = "Duplicate id")),
            )
        }

        assertNull(repository.getOrderDetails(order.id))
    }

    private fun database(): SahmPosDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        SahmPosDatabase.Schema.create(driver)
        return SahmPosDatabase(driver)
    }

    private companion object {
        val menuItem = MenuItem(
            id = "burger",
            category = "Burgers",
            name = "Burger",
            description = "Classic",
            imageUrl = "https://example.com/burger.webp",
            localImageUrl = "file:///burger.webp",
            price = 5_000,
        )

        val order = Order(
            id = "order-1",
            cashierId = "cashier-1",
            cashierName = "Mona",
            subtotalAmount = 5_000,
            taxAmount = 700,
            discountAmount = 0,
            totalAmount = 5_700,
            discountId = null,
            discountPromoCode = null,
            discountPercent = null,
            discountMinValue = null,
            discountMaxValue = null,
            orderStatus = OrderStatus.PendingPayment,
            paymentStatus = PaymentStatus.NotStarted,
            printStatus = PrintStatus.NotPrinted,
            createdAt = 1_000,
            paidAt = null,
            cancelledAt = null,
            syncedAt = null,
        )

        val orderItem = OrderItem(
            id = "order-item-1",
            orderId = order.id,
            menuItemId = menuItem.id,
            category = menuItem.category,
            name = menuItem.name,
            description = menuItem.description,
            imageUrl = menuItem.imageUrl,
            localImageUrl = menuItem.localImageUrl,
            quantity = 1,
            unitPrice = 5_000,
            subtotalAmount = 5_000,
            discountAmount = 0,
            taxAmount = 700,
            totalAmount = 5_700,
        )
    }
}
