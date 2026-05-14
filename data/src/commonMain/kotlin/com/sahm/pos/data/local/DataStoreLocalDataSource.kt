package com.sahm.pos.data.local

import com.sahm.pos.domain.entity.CurrentUser

interface DataStoreLocalDataSource {
    suspend fun saveCurrentUser(currentUser: CurrentUser)
    suspend fun getCurrentUser(): CurrentUser?
}
