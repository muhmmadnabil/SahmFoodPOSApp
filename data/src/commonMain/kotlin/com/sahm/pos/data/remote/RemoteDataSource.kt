package com.sahm.pos.data.remote

import com.sahm.pos.data.model.RemoteDiscountDocument
import com.sahm.pos.data.model.RemoteMenuItemDocument
import com.sahm.pos.data.model.RemoteUserDocument
import com.sahm.pos.domain.entity.SyncOutboxItem
import com.sahm.pos.domain.entity.User
import com.sahm.pos.domain.results.SyncUploadResult

interface RemoteDataSource {
    suspend fun getUsers(): List<User>
    suspend fun getUserDocuments(): List<RemoteUserDocument>
    suspend fun getMenuItemDocuments(): List<RemoteMenuItemDocument>
    suspend fun getDiscountDocuments(): List<RemoteDiscountDocument>
    suspend fun uploadOutboxItem(item: SyncOutboxItem): SyncUploadResult =
        SyncUploadResult.RetryableError(
            code = "SERVER_UNAVAILABLE",
            message = "Outbox upload is not available on this platform.",
        )
}

expect fun createRemoteDataSource(): RemoteDataSource
