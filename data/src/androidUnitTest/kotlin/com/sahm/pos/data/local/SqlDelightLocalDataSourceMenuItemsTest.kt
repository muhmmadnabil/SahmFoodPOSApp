package com.sahm.pos.data.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.sahm.pos.data.local.database.SahmPosDatabase
import com.sahm.pos.domain.entity.MenuItem
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class SqlDelightLocalDataSourceMenuItemsTest {

    @Test
    fun insertMenuItemsSavesAllFieldsCorrectly() = runTest {
        val local = localDataSource()

        local.replaceMenuItemsSnapshot(listOf(item))

        assertEquals(item, local.getMenuItemById(item.id))
    }

    @Test
    fun upsertSameIdUpdatesName() = runTest {
        val local = localDataSource()

        local.replaceMenuItemsSnapshot(listOf(item))
        local.replaceMenuItemsSnapshot(listOf(item.copy(name = "New name")))

        assertEquals("New name", local.getMenuItemById(item.id)?.name)
    }

    @Test
    fun upsertSameIdUpdatesPrice() = runTest {
        val local = localDataSource()

        local.replaceMenuItemsSnapshot(listOf(item))
        local.replaceMenuItemsSnapshot(listOf(item.copy(price = 5900)))

        assertEquals(5900, local.getMenuItemById(item.id)?.price)
    }

    @Test
    fun upsertSameIdUpdatesCategory() = runTest {
        val local = localDataSource()

        local.replaceMenuItemsSnapshot(listOf(item))
        local.replaceMenuItemsSnapshot(listOf(item.copy(category = "Chicken")))

        assertEquals("Chicken", local.getMenuItemById(item.id)?.category)
    }

    @Test
    fun upsertSameIdUpdatesDescription() = runTest {
        val local = localDataSource()

        local.replaceMenuItemsSnapshot(listOf(item))
        local.replaceMenuItemsSnapshot(listOf(item.copy(description = "Updated")))

        assertEquals("Updated", local.getMenuItemById(item.id)?.description)
    }

    @Test
    fun upsertSameIdUpdatesImageUrl() = runTest {
        val local = localDataSource()

        local.replaceMenuItemsSnapshot(listOf(item))
        local.replaceMenuItemsSnapshot(listOf(item.copy(imageUrl = "https://example.com/new.webp")))

        assertEquals("https://example.com/new.webp", local.getMenuItemById(item.id)?.imageUrl)
    }

    @Test
    fun upsertSameIdUpdatesLocalImageUrl() = runTest {
        val local = localDataSource()

        local.replaceMenuItemsSnapshot(listOf(item))
        local.replaceMenuItemsSnapshot(listOf(item.copy(localImageUrl = "file:///cache/new.webp")))

        assertEquals("file:///cache/new.webp", local.getMenuItemById(item.id)?.localImageUrl)
    }

    @Test
    fun upsertSameIdUpdatesLastSyncedAt() = runTest {
        val local = localDataSource()

        local.replaceMenuItemsSnapshot(listOf(item))
        local.replaceMenuItemsSnapshot(listOf(item.copy(lastSyncedAt = 2000)))

        assertEquals(2000, local.getMenuItemById(item.id)?.lastSyncedAt)
    }

    @Test
    fun upsertSameIdDoesNotCreateDuplicates() = runTest {
        val local = localDataSource()

        local.replaceMenuItemsSnapshot(listOf(item))
        local.replaceMenuItemsSnapshot(listOf(item.copy(name = "Updated")))

        assertEquals(1, local.getMenuItemCountById(item.id))
    }

    @Test
    fun replaceMenuItemsSnapshotSavesAllValidItemsInOneTransaction() = runTest {
        val local = localDataSource()
        val second = item.copy(id = "fries", name = "Fries")

        local.replaceMenuItemsSnapshot(listOf(item, second))

        assertEquals(listOf(item, second), local.getActiveMenuItems().sortedBy { it.id })
    }

    @Test
    fun replaceMenuItemsSnapshotMarksMissingOldItemsInactiveIfIsActiveExists() = runTest {
        val local = localDataSource()
        val removed = item.copy(id = "removed")

        local.replaceMenuItemsSnapshot(listOf(item, removed))
        local.replaceMenuItemsSnapshot(listOf(item))

        assertFalse(local.getMenuItemById(removed.id)?.isActive ?: true)
    }

    @Test
    fun getActiveMenuItemsDoesNotReturnInactiveItems() = runTest {
        val local = localDataSource()
        val removed = item.copy(id = "removed")

        local.replaceMenuItemsSnapshot(listOf(item, removed))
        local.replaceMenuItemsSnapshot(listOf(item))

        assertEquals(listOf(item), local.getActiveMenuItems())
    }

    @Test
    fun localSaveRollbackWorksWhenTransactionFails() = runTest {
        val local = localDataSource()
        local.replaceMenuItemsSnapshot(listOf(item))

        runCatching {
            local.replaceMenuItemsSnapshot(
                listOf(item.copy(name = "Should rollback"), item.copy(id = ""))
            )
        }

        assertEquals(item, local.getMenuItemById(item.id))
        assertNull(local.getMenuItemById(""))
    }

    @Test
    fun repeatedSyncWithSameDataStaysIdempotent() = runTest {
        val local = localDataSource()

        local.replaceMenuItemsSnapshot(listOf(item))
        local.replaceMenuItemsSnapshot(listOf(item))

        assertEquals(1, local.getMenuItemCount())
        assertEquals(item, local.getMenuItemById(item.id))
    }

    private fun localDataSource(): SqlDelightLocalDataSource {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        SahmPosDatabase.Schema.create(driver)
        return SqlDelightLocalDataSourceImpl(SahmPosDatabase(driver))
    }

    private companion object {
        val item = MenuItem(
            id = "burger_animal_style",
            category = "Beef Burgers",
            name = "Animal Style",
            description = "Crispy onion rings",
            imageUrl = "https://example.com/burger.webp",
            localImageUrl = "file:///cache/burger.webp",
            price = 5702,
            lastSyncedAt = 1000,
            isActive = true,
        )
    }
}
