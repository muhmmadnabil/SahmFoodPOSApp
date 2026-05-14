package com.sahm.pos.domain.repository

interface SyncDataRepo {
    suspend fun hasUsers(): Boolean
    suspend fun syncUsers()
}