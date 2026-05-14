package com.sahm.pos.data.remote

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.sahm.pos.domain.entity.User
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private const val USERS_COLLECTION = "users"

internal class FirebaseRemoteDataSource(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : RemoteDataSource {
    override suspend fun createUser(user: User) {
        firestore.collection(USERS_COLLECTION)
            .document(user.id)
            .set(user.toFirestoreMap())
            .awaitUnit()
    }
}

actual fun createRemoteDataSource(): RemoteDataSource = FirebaseRemoteDataSource()

private fun User.toFirestoreMap(): Map<String, Any> = mapOf(
    "username" to username,
    "createdAt" to createdAt,
    "isActive" to isActive,
    "lastLoginAt" to lastLoginAt,
    "password" to password,
    "id" to id,
)

private suspend fun Task<Void>.awaitUnit() {
    suspendCoroutine { continuation ->
        addOnSuccessListener {
            continuation.resume(Unit)
        }
        addOnFailureListener { exception ->
            continuation.resumeWithException(exception)
        }
    }
}
