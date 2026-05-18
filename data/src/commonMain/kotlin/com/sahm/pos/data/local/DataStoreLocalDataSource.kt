package com.sahm.pos.data.local

import com.sahm.pos.domain.entity.CurrentUser
import com.sahm.pos.domain.entity.TimeSyncInfo

interface DataStoreLocalDataSource {
    suspend fun saveCurrentUser(currentUser: CurrentUser)
    suspend fun getCurrentUser(): CurrentUser?
    suspend fun clearCurrentUser() = Unit
    suspend fun saveTimeSyncInfo(info: TimeSyncInfo) = Unit
    suspend fun getTimeSyncInfo(): TimeSyncInfo? = null
}
