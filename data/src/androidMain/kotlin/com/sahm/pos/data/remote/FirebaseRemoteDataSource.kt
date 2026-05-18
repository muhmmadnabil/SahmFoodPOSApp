package com.sahm.pos.data.remote

import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.sahm.pos.data.mapper.toUserOrNull
import com.sahm.pos.data.model.RemoteDiscountDocument
import com.sahm.pos.data.model.RemoteMenuItemDocument
import com.sahm.pos.data.model.RemoteUserDocument
import com.sahm.pos.domain.entity.SyncAggregateType
import com.sahm.pos.domain.entity.SyncOutboxItem
import com.sahm.pos.domain.entity.User
import com.sahm.pos.domain.results.SyncUploadResult
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.Clock

private const val USERS_COLLECTION = "users"
private const val ITEMS_COLLECTION = "items"
private const val DISCOUNTS_COLLECTION = "discounts"
private const val ORDER_UPLOADS_COLLECTION = "Order"
private const val PAYMENT_UPLOADS_COLLECTION = "Payment"
private const val REFUND_UPLOADS_COLLECTION = "Refund"

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

    override suspend fun getDiscountDocuments(): List<RemoteDiscountDocument> {
        return mapFirebaseException {
            firestore.collection(DISCOUNTS_COLLECTION)
                .get()
                .await()
                .documents
                .map { document -> document.toDiscountDocument() }
        }
    }

    override suspend fun uploadOutboxItem(item: SyncOutboxItem): SyncUploadResult =
        try {
            val document = firestore.collection(item.uploadCollectionName())
                .document(item.idempotencyKey)
            if (document.get().await().exists()) {
                SyncUploadResult.DuplicateIdempotencyKey
            } else {
                document.set(item.toUploadDocument()).awaitUnit()
                SyncUploadResult.Success
            }
        } catch (exception: FirebaseFirestoreException) {
            when (exception.code) {
                FirebaseFirestoreException.Code.ALREADY_EXISTS ->
                    SyncUploadResult.DuplicateIdempotencyKey
                FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                    SyncUploadResult.RetryableError("TIMEOUT", "Request timed out.")
                FirebaseFirestoreException.Code.UNAVAILABLE ->
                    SyncUploadResult.RetryableError("NO_INTERNET", "Network is unavailable.")
                FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED ->
                    SyncUploadResult.RetryableError("RATE_LIMIT", "Rate limit reached.")
                FirebaseFirestoreException.Code.INTERNAL ->
                    SyncUploadResult.RetryableError("HTTP_500", "Temporary server error.")
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    SyncUploadResult.NonRetryableError("UNAUTHORIZED_CASHIER", "Cashier is not authorized.")
                FirebaseFirestoreException.Code.INVALID_ARGUMENT ->
                    SyncUploadResult.NonRetryableError("INVALID_PAYLOAD", "Remote rejected the payload.")
                else ->
                    SyncUploadResult.RetryableError("TEMPORARY_FIRESTORE_FAILURE", exception.message.orEmpty())
            }
        } catch (throwable: Throwable) {
            SyncUploadResult.RetryableError("TEMPORARY_API_FAILURE", throwable.message.orEmpty())
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

private fun DocumentSnapshot.toDiscountDocument(): RemoteDiscountDocument =
    RemoteDiscountDocument(
        id = id,
        promo = getString("promo"),
        percent = getNumberAsDouble("percent"),
        minValue = getNumberAsDouble("minValue"),
        maxValue = getNumberAsDouble("maxValue"),
        startAt = getTimestampMillis("startAt"),
        endAt = getTimestampMillis("endAt"),
    )

private fun SyncOutboxItem.toUploadDocument(): Map<String, Any?> =
    mapOf(
        "type" to type.name,
        "aggregateId" to aggregateId,
        "aggregateType" to aggregateType.name,
        "payloadJson" to payloadJson,
        "idempotencyKey" to idempotencyKey,
        "createdAt" to createdAt,
        "uploadedAt" to currentTimestampMillis(),
    )

private fun SyncOutboxItem.uploadCollectionName(): String =
    when (aggregateType) {
        SyncAggregateType.ORDER -> ORDER_UPLOADS_COLLECTION
        SyncAggregateType.PAYMENT -> PAYMENT_UPLOADS_COLLECTION
        SyncAggregateType.REFUND -> REFUND_UPLOADS_COLLECTION
    }

private suspend inline fun <T> mapFirebaseException(block: suspend () -> T): T =
    try {
        block()
    } catch (exception: FirebaseFirestoreException) {
        throw when (exception.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                RemoteDataException.PermissionDenied
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                RemoteDataException.RequestTimeout
            FirebaseFirestoreException.Code.UNAVAILABLE ->
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

private fun DocumentSnapshot.getNumberAsDouble(field: String): Double? =
    when (val value = get(field)) {
        is Long -> value.toDouble()
        is Int -> value.toDouble()
        is Double -> value
        is Float -> value.toDouble()
        else -> null
    }

private fun DocumentSnapshot.getTimestampMillis(field: String): Long? =
    when (val value = get(field)) {
        is Timestamp -> value.seconds * 1_000L + value.nanoseconds / 1_000_000L
        is Long -> value
        is Int -> value.toLong()
        else -> null
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
