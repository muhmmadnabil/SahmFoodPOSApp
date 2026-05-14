package com.sahm.pos.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.sahm.pos.domain.entity.CurrentUser
import kotlinx.coroutines.flow.first

internal const val CURRENT_USER_PREFS_FILE = "current_user.preferences_pb"

internal class DataStorePref(
    private val dataStore: DataStore<Preferences>,
) : DataStoreLocalDataSource {

    override suspend fun saveCurrentUser(currentUser: CurrentUser) {
        dataStore.edit { preferences ->
            preferences[CurrentUserIdKey] = currentUser.id
            preferences[CurrentUsernameKey] = currentUser.username
            preferences[CurrentUserPhoneKey] = currentUser.phone
        }
    }

    override suspend fun getCurrentUser(): CurrentUser? {
        val preferences = dataStore.data.first()
        val id = preferences[CurrentUserIdKey] ?: return null
        val username = preferences[CurrentUsernameKey] ?: return null
        val phone = preferences[CurrentUserPhoneKey] ?: return null

        return CurrentUser(
            id = id,
            username = username,
            phone = phone,
        )
    }

    private companion object {
        val CurrentUserIdKey = stringPreferencesKey("current_user_id")
        val CurrentUsernameKey = stringPreferencesKey("current_username")
        val CurrentUserPhoneKey = stringPreferencesKey("current_user_phone")
    }
}
