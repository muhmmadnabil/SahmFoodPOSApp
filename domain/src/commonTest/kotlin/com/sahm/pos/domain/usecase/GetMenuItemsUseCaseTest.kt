package com.sahm.pos.domain.usecase

import com.sahm.pos.domain.SyncResult
import com.sahm.pos.domain.entity.MenuItem
import com.sahm.pos.domain.repository.SyncDataRepo
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetMenuItemsUseCaseTest {

    @Test
    fun returnsLocalMenuItems() = runTest {
        val useCase = GetMenuItemsUseCase(FakeMenuItemsRepo(items = listOf(activeItem)))

        assertEquals(listOf(activeItem), useCase())
    }

    @Test
    fun returnsEmptyListWhenNoLocalItemsExist() = runTest {
        val useCase = GetMenuItemsUseCase(FakeMenuItemsRepo(items = emptyList()))

        assertEquals(emptyList(), useCase())
    }

    @Test
    fun returnsOnlyActiveItemsIfIsActiveExists() = runTest {
        val useCase = GetMenuItemsUseCase(
            FakeMenuItemsRepo(items = listOf(activeItem, inactiveItem).filter { it.isActive })
        )

        assertEquals(listOf(activeItem), useCase())
    }

    @Test
    fun doesNotCallFirestore() = runTest {
        val repo = FakeMenuItemsRepo(items = listOf(activeItem))

        GetMenuItemsUseCase(repo).invoke()

        assertEquals(0, repo.firestoreCalls)
    }

    @Test
    fun doesNotTriggerSync() = runTest {
        val repo = FakeMenuItemsRepo(items = listOf(activeItem))

        GetMenuItemsUseCase(repo).invoke()

        assertEquals(0, repo.syncCalls)
    }

    private class FakeMenuItemsRepo(
        private val items: List<MenuItem>,
    ) : SyncDataRepo {
        var firestoreCalls = 0
        var syncCalls = 0

        override suspend fun hasUsers(): Boolean = false

        override suspend fun syncUsers(): SyncResult {
            syncCalls += 1
            return SyncResult.EmptyRemoteData
        }

        override suspend fun syncMenuItems(): SyncResult {
            syncCalls += 1
            return SyncResult.EmptyRemoteData
        }

        override suspend fun getActiveMenuItems(): List<MenuItem> = items

        override suspend fun getUserCount(): Long = 0

        override suspend fun getMenuItemCount(): Long = items.size.toLong()

        override suspend fun getLastUsersSyncAt(): Long? = null

        override suspend fun getLastMenuItemsSyncAt(): Long? = null
    }

    private companion object {
        val activeItem = MenuItem(
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

        val inactiveItem = activeItem.copy(id = "old_burger", isActive = false)
    }
}
