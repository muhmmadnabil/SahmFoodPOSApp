package com.sahm.pos.data.remote

import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.sahm.pos.domain.entity.User
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val USERS_COLLECTION = "users"

internal class FirebaseRemoteDataSource(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : RemoteDataSource {
    override suspend fun getUsers(): List<User> {
        return firestore.collection(USERS_COLLECTION)
            .get()
            .await()
            .documents
            .mapNotNull { document -> document.toUserOrNull() }
    }
}

actual fun createRemoteDataSource(): RemoteDataSource = FirebaseRemoteDataSource()

private fun DocumentSnapshot.toUserOrNull(): User? {
    val username = getString("username") ?: return null
    val phone = getString("phone") ?: return null
    val password = getString("password") ?: return null

    return User(
        id = getString("id").orEmpty().ifBlank { id },
        username = username,
        phone = phone,
        createdAt = getTimestampString("createdAt"),
        isActive = getBoolean("isActive") ?: false,
        lastLoginAt = getTimestampString("lastLoginAt"),
        password = password,
    )
}

private fun DocumentSnapshot.getTimestampString(field: String): String {
    return when (val value = get(field)) {
        is Timestamp -> value.seconds.toString()
        is String -> value
        else -> ""
    }
}

private suspend fun <T> Task<T>.await(): T {
    return suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result ->
            continuation.resume(result)
        }
        addOnFailureListener { exception ->
            continuation.resumeWithException(exception)
        }
    }
}

private suspend fun Task<Void>.awaitUnit() {
    suspendCancellableCoroutine { continuation ->
        addOnSuccessListener {
            continuation.resume(Unit)
        }
        addOnFailureListener { exception ->
            continuation.resumeWithException(exception)
        }
    }
}