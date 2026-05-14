package com.sahm.pos.data.remote

import com.sahm.pos.domain.entity.User

actual fun createRemoteDataSource(): RemoteDataSource = IosRemoteDataSource

private object IosRemoteDataSource : RemoteDataSource {
    override suspend fun createUser(user: User) {
        throw UnsupportedOperationException("Firebase Firestore is configured for Android only.")
    }
}
