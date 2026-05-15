package com.sahm.pos.domain.repository

import com.sahm.pos.domain.SyncResult
import com.sahm.pos.domain.entity.MenuItem

interface SyncDataRepo {
    suspend fun hasUsers(): Boolean
    suspend fun syncUsers(): SyncResult
    suspend fun syncMenuItems(): SyncResult
    suspend fun getActiveMenuItems(): List<MenuItem>
    suspend fun getUserCount(): Long
    suspend fun getMenuItemCount(): Long
    suspend fun getLastUsersSyncAt(): Long?
    suspend fun getLastMenuItemsSyncAt(): Long?
}