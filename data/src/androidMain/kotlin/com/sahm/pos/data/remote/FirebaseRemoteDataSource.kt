package com.sahm.pos.data.remote

import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.sahm.pos.data.mapper.toUserOrNull
import com.sahm.pos.data.model.RemoteMenuItemDocument
import com.sahm.pos.data.model.RemoteUserDocument
import com.sahm.pos.data.remote.RemoteDataException
import com.sahm.pos.domain.entity.User
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.Clock

private const val USERS_COLLECTION = "users"
private const val ITEMS_COLLECTION = "items"

internal class FirebaseRemoteDataSource(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : RemoteDataSource {
    override suspend fun getUsers(): List<User> {
        return mapFirebaseException {
            firestore.collection(USERS_COLLECTION)
                .get()
                .await()
                .documents
                .mapNotNull { document ->
                    document.toUserDocument().toUserOrNull(currentTimestampMillis())
                }
        }
    }

    override suspend fun getUserDocuments(): List<RemoteUserDocument> {
        return mapFirebaseException {
            firestore.collection(USERS_COLLECTION)
                .get()
                .await()
                .documents
                .map { document -> document.toUserDocument() }
        }
    }

    override suspend fun getMenuItemDocuments(): List<RemoteMenuItemDocument> {
        return mapFirebaseException {
            firestore.collection(ITEMS_COLLECTION)
                .get()
                .await()
                .documents
                .map { document -> document.toMenuItemDocument() }
        }
    }
}

actual fun createRemoteDataSource(): RemoteDataSource = FirebaseRemoteDataSource()

private fun DocumentSnapshot.toUserDocument(): RemoteUserDocument =
    RemoteUserDocument(
        id = getString("id").orEmpty().ifBlank { id },
        username = getString("username"),
        phone = getString("phone"),
        password = getString("password"),
        createdAt = getTimestampString("createdAt"),
        isActive = getBoolean("isActive"),
        lastLoginAt = getTimestampString("lastLoginAt"),
    )

private fun DocumentSnapshot.toMenuItemDocument(): RemoteMenuItemDocument =
    RemoteMenuItemDocument(
        id = id,
        category = getString("category"),
        name = getString("name"),
        description = getString("description"),
        imageUrl = getString("imageUrl"),
        price = getLong("price"),
    )

private suspend inline fun <T> mapFirebaseException(block: suspend () -> T): T =
    try {
        block()
    } catch (exception: FirebaseFirestoreException) {
        throw when (exception.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                RemoteDataException.PermissionDenied
            FirebaseFirestoreException.Code.UNAVAILABLE,
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                RemoteDataException.NoInternet
            else -> exception
        }
    }

private fun currentTimestampMillis(): Long {
    return Clock.System.now().toEpochMilliseconds()
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
