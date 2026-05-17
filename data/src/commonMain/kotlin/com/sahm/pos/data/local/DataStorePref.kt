package com.sahm.pos.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.sahm.pos.domain.entity.CurrentUser
import com.sahm.pos.domain.entity.TimeSyncInfo
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

    override suspend fun clearCurrentUser() {
        dataStore.edit { preferences ->
            preferences.remove(CurrentUserIdKey)
            preferences.remove(CurrentUsernameKey)
            preferences.remove(CurrentUserPhoneKey)
        }
    }

    override suspend fun saveTimeSyncInfo(info: TimeSyncInfo) {
        dataStore.edit { preferences ->
            preferences[OffsetMillisKey] = info.offsetMillis
            preferences[ServerTimeMillisKey] = info.serverTimeMillis
            preferences[PhoneTimeMillisKey] = info.phoneTimeMillis
            preferences[CheckedAtMillisKey] = info.checkedAtMillis
        }
    }

    override suspend fun getTimeSyncInfo(): TimeSyncInfo? {
        val preferences = dataStore.data.first()
        return TimeSyncInfo(
            offsetMillis = preferences[OffsetMillisKey] ?: return null,
            serverTimeMillis = preferences[ServerTimeMillisKey] ?: return null,
            phoneTimeMillis = preferences[PhoneTimeMillisKey] ?: return null,
            checkedAtMillis = preferences[CheckedAtMillisKey] ?: return null,
        )
    }

    private companion object {
        val CurrentUserIdKey = stringPreferencesKey("current_user_id")
        val CurrentUsernameKey = stringPreferencesKey("current_username")
        val CurrentUserPhoneKey = stringPreferencesKey("current_user_phone")
        val OffsetMillisKey = longPreferencesKey("time_offset_millis")
        val ServerTimeMillisKey = longPreferencesKey("time_server_time_millis")
        val PhoneTimeMillisKey = longPreferencesKey("time_phone_time_millis")
        val CheckedAtMillisKey = longPreferencesKey("time_checked_at_millis")
    }
}
