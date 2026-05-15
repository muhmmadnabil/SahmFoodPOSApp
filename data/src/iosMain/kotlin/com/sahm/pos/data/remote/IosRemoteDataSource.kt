package com.sahm.pos.data.remote

import com.sahm.pos.data.model.RemoteMenuItemDocument
import com.sahm.pos.data.model.RemoteUserDocument
import com.sahm.pos.domain.entity.User

actual fun createRemoteDataSource(): RemoteDataSource = IosRemoteDataSource

private object IosRemoteDataSource : RemoteDataSource {
    override suspend fun getUsers(): List<User> {
        throw UnsupportedOperationException("Firebase Firestore is configured for Android only.")
    }

    override suspend fun getUserDocuments(): List<RemoteUserDocument> {
        throw UnsupportedOperationException("Firebase Firestore is configured for Android only.")
    }

    override suspend fun getMenuItemDocuments(): List<RemoteMenuItemDocument> {
        throw UnsupportedOperationException("Firebase Firestore is configured for Android only.")
    }
}
