package com.sahm.pos.data.remote

interface TimeRemoteDataSource {
    suspend fun getUnixTimeMillis(): Result<Long>
}