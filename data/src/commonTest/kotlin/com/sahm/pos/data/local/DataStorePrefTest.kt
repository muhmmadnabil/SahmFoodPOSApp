package com.sahm.pos.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.sahm.pos.domain.entity.CurrentUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DataStorePrefTest {

    @Test
    fun givenCurrentUser_whenSaveCurrentUser_thenCanReadSameCurrentUser() = runTest {
        val dataSource = DataStorePref(FakePreferencesDataStore())

        dataSource.saveCurrentUser(currentUser)

        assertEquals(currentUser, dataSource.getCurrentUser())
    }

    @Test
    fun givenEmptyPreferences_whenGetCurrentUser_thenReturnsNull() = runTest {
        val dataSource = DataStorePref(FakePreferencesDataStore())

        val result = dataSource.getCurrentUser()

        assertNull(result)
    }

    @Test
    fun givenMissingUsername_whenGetCurrentUser_thenReturnsNull() = runTest {
        val dataSource = DataStorePref(
            FakePreferencesDataStore(
                preferencesOf(
                    stringPreferencesKey("current_user_id") to currentUser.id,
                    stringPreferencesKey("current_user_phone") to currentUser.phone,
                )
            )
        )

        val result = dataSource.getCurrentUser()

        assertNull(result)
    }

    private class FakePreferencesDataStore(
        initialPreferences: Preferences = emptyPreferences(),
    ) : DataStore<Preferences> {
        private val state = MutableStateFlow(initialPreferences)

        override val data: Flow<Preferences> = state

        override suspend fun updateData(
            transform: suspend (t: Preferences) -> Preferences,
        ): Preferences {
            val updated = transform(state.value)
            state.value = updated
            return updated
        }
    }

    private companion object {
        val currentUser = CurrentUser(
            id = "cashier-1",
            username = "Noura",
            phone = "01012345678",
        )
    }
}
