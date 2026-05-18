package com.sahm.pos.data.remote

import com.sahm.pos.data.mapper.toUserOrNull
import com.sahm.pos.data.model.RemoteDiscountDocument
import com.sahm.pos.data.model.RemoteMenuItemDocument
import com.sahm.pos.data.model.RemoteUserDocument
import com.sahm.pos.domain.entity.SyncAggregateType
import com.sahm.pos.domain.entity.SyncOutboxItem
import com.sahm.pos.domain.entity.User
import com.sahm.pos.domain.results.SyncUploadResult
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.encodeURLPathPart
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.roundToLong
import kotlin.time.Clock

private const val USERS_COLLECTION = "users"
private const val ITEMS_COLLECTION = "items"
private const val DISCOUNTS_COLLECTION = "discounts"
private const val ORDER_UPLOADS_COLLECTION = "Order"
private const val PAYMENT_UPLOADS_COLLECTION = "Payment"
private const val REFUND_UPLOADS_COLLECTION = "Refund"

internal class IosRemoteDataSource(
    private val client: HttpClient = defaultFirestoreHttpClient(),
    private val config: IosFirestoreConfig = IosFirestoreConfig.fromGoogleServiceInfo(),
) : RemoteDataSource {
    private val baseUrl =
        "https://firestore.googleapis.com/v1/projects/${config.projectId}/databases/(default)/documents"

    override suspend fun getUsers(): List<User> =
        getUserDocuments()
            .mapNotNull { document -> document.toUserOrNull(Clock.System.now().toEpochMilliseconds()) }

    override suspend fun getUserDocuments(): List<RemoteUserDocument> =
        getCollection(USERS_COLLECTION).map { document ->
            RemoteUserDocument(
                id = document.stringField("id").orEmpty().ifBlank { document.documentId },
                username = document.stringField("username"),
                phone = document.stringField("phone"),
                password = document.stringField("password"),
                createdAt = document.timestampStringField("createdAt").orEmpty(),
                isActive = document.booleanField("isActive"),
                lastLoginAt = document.timestampStringField("lastLoginAt").orEmpty(),
            )
        }

    override suspend fun getMenuItemDocuments(): List<RemoteMenuItemDocument> =
        getCollection(ITEMS_COLLECTION).map { document ->
            RemoteMenuItemDocument(
                id = document.documentId,
                category = document.stringField("category"),
                name = document.stringField("name"),
                description = document.stringField("description"),
                imageUrl = document.stringField("imageUrl"),
                price = document.longField("price"),
            )
        }

    override suspend fun getDiscountDocuments(): List<RemoteDiscountDocument> =
        getCollection(DISCOUNTS_COLLECTION).map { document ->
            RemoteDiscountDocument(
                id = document.documentId,
                promo = document.stringField("promo"),
                percent = document.doubleField("percent"),
                minValue = document.doubleField("minValue"),
                maxValue = document.doubleField("maxValue"),
                startAt = document.timestampMillisField("startAt"),
                endAt = document.timestampMillisField("endAt"),
            )
        }

    override suspend fun uploadOutboxItem(item: SyncOutboxItem): SyncUploadResult =
        try {
            val response = client.post(
                "$baseUrl/${item.uploadCollectionName()}?documentId=${item.idempotencyKey.encodeURLPathPart()}&key=${config.apiKey}",
            ) {
                contentType(ContentType.Application.Json)
                setBody(JsonObject(mapOf("fields" to item.toFirestoreFields())).toString())
            }

            when (response.status) {
                HttpStatusCode.OK -> SyncUploadResult.Success
                HttpStatusCode.Conflict -> SyncUploadResult.DuplicateIdempotencyKey
                HttpStatusCode.Forbidden, HttpStatusCode.Unauthorized ->
                    SyncUploadResult.NonRetryableError("UNAUTHORIZED_CASHIER", "Cashier is not authorized.")
                HttpStatusCode.BadRequest ->
                    SyncUploadResult.NonRetryableError("INVALID_PAYLOAD", "Remote rejected the payload.")
                HttpStatusCode.RequestTimeout ->
                    SyncUploadResult.RetryableError("TIMEOUT", "Request timed out.")
                HttpStatusCode.TooManyRequests ->
                    SyncUploadResult.RetryableError("RATE_LIMIT", "Rate limit reached.")
                else ->
                    if (response.status.value >= HttpStatusCode.InternalServerError.value) {
                        SyncUploadResult.RetryableError("HTTP_500", "Temporary server error.")
                    } else {
                        SyncUploadResult.RetryableError("TEMPORARY_FIRESTORE_FAILURE", response.bodyAsText())
                    }
            }
        } catch (exception: HttpRequestTimeoutException) {
            SyncUploadResult.RetryableError("TIMEOUT", "Request timed out.")
        } catch (exception: IOException) {
            SyncUploadResult.RetryableError("NO_INTERNET", "Network is unavailable.")
        } catch (throwable: Throwable) {
            SyncUploadResult.RetryableError("TEMPORARY_API_FAILURE", throwable.message.orEmpty())
        }

    private suspend fun getCollection(collection: String): List<FirestoreDocument> =
        mapRemoteException {
            val response = client.get("$baseUrl/$collection?key=${config.apiKey}")
            when {
                response.status == HttpStatusCode.NotFound -> emptyList()
                response.status == HttpStatusCode.Forbidden || response.status == HttpStatusCode.Unauthorized ->
                    throw RemoteDataException.PermissionDenied
                response.status == HttpStatusCode.RequestTimeout ->
                    throw RemoteDataException.RequestTimeout
                response.status.value >= HttpStatusCode.InternalServerError.value ->
                    throw RemoteDataException.ServerError
                response.status.value !in 200..299 ->
                    throw RemoteDataException.Unknown
                else -> response.parseCollectionDocuments()
            }
        }
}

actual fun createRemoteDataSource(): RemoteDataSource = IosRemoteDataSource()

private const val FIRESTORE_PROJECT_ID = "sahm-pos-1eb1f"
private const val FIRESTORE_API_KEY = "AIzaSyCz_3GDOOIQBqB4YjOfpRVQyDhBWVaHX7c"

internal data class IosFirestoreConfig(
    val projectId: String,
    val apiKey: String,
) {
    companion object {
        fun fromGoogleServiceInfo(): IosFirestoreConfig =
            IosFirestoreConfig(
                projectId = FIRESTORE_PROJECT_ID,
                apiKey = FIRESTORE_API_KEY,
            )
    }
}

private data class FirestoreDocument(
    val documentId: String,
    val fields: JsonObject,
)

private suspend inline fun <T> mapRemoteException(block: () -> T): T =
    try {
        block()
    } catch (exception: HttpRequestTimeoutException) {
        throw RemoteDataException.RequestTimeout
    } catch (exception: IOException) {
        throw RemoteDataException.NoInternet
    } catch (exception: SerializationException) {
        throw RemoteDataException.SerializationError
    } catch (exception: IllegalArgumentException) {
        throw RemoteDataException.SerializationError
    }

private suspend fun HttpResponse.parseCollectionDocuments(): List<FirestoreDocument> {
    val root = firestoreJson.parseToJsonElement(bodyAsText()).jsonObject
    val documents = root["documents"] as? JsonArray ?: return emptyList()
    return documents.mapNotNull { element ->
        val document = element as? JsonObject ?: return@mapNotNull null
        val name = document["name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
        FirestoreDocument(
            documentId = name.substringAfterLast("/"),
            fields = document["fields"] as? JsonObject ?: JsonObject(emptyMap()),
        )
    }
}

private fun FirestoreDocument.value(name: String): JsonObject? =
    fields[name] as? JsonObject

private fun FirestoreDocument.stringField(name: String): String? =
    value(name)?.stringValue()

private fun FirestoreDocument.timestampStringField(name: String): String? {
    val value = value(name) ?: return null
    return value.stringValue()
        ?: value.timestampValue()
}

private fun FirestoreDocument.booleanField(name: String): Boolean? =
    value(name)?.get("booleanValue")?.jsonPrimitive?.booleanOrNull

private fun FirestoreDocument.longField(name: String): Long? =
    value(name)?.numberString()?.toLongOrNull()

private fun FirestoreDocument.doubleField(name: String): Double? =
    value(name)?.numberString()?.toDoubleOrNull()

private fun FirestoreDocument.timestampMillisField(name: String): Long? {
    val value = value(name) ?: return null
    return value.numberString()?.toDoubleOrNull()?.roundToLong()
        ?: value.timestampValue()?.toEpochMillisOrNull()
}

private fun JsonObject.stringValue(): String? =
    get("stringValue")?.jsonPrimitive?.contentOrNull

private fun JsonObject.timestampValue(): String? =
    get("timestampValue")?.jsonPrimitive?.contentOrNull

private fun JsonObject.numberString(): String? =
    get("integerValue")?.jsonPrimitive?.contentOrNull
        ?: get("doubleValue")?.jsonPrimitive?.contentOrNull

private fun SyncOutboxItem.uploadCollectionName(): String =
    when (aggregateType) {
        SyncAggregateType.ORDER -> ORDER_UPLOADS_COLLECTION
        SyncAggregateType.PAYMENT -> PAYMENT_UPLOADS_COLLECTION
        SyncAggregateType.REFUND -> REFUND_UPLOADS_COLLECTION
    }

private fun SyncOutboxItem.toFirestoreFields(): JsonObject =
    JsonObject(
        mapOf(
            "type" to firestoreString(type.name),
            "aggregateId" to firestoreString(aggregateId),
            "aggregateType" to firestoreString(aggregateType.name),
            "payloadJson" to firestoreString(payloadJson),
            "idempotencyKey" to firestoreString(idempotencyKey),
            "createdAt" to firestoreInteger(createdAt),
            "uploadedAt" to firestoreInteger(Clock.System.now().toEpochMilliseconds()),
        ),
    )

private fun firestoreString(value: String): JsonElement =
    JsonObject(mapOf("stringValue" to JsonPrimitive(value)))

private fun firestoreInteger(value: Long): JsonElement =
    JsonObject(mapOf("integerValue" to JsonPrimitive(value.toString())))

private fun String.toEpochMillisOrNull(): Long? {
    val dateTime = substringBefore(".").removeSuffix("Z")
    val dateAndTime = dateTime.split("T", limit = 2)
    if (dateAndTime.size != 2) return null
    val date = dateAndTime[0].split("-")
    val time = dateAndTime[1].split(":")
    if (date.size != 3 || time.size < 3) return null

    val year = date[0].toIntOrNull() ?: return null
    val month = date[1].toIntOrNull() ?: return null
    val day = date[2].toIntOrNull() ?: return null
    val hour = time[0].toIntOrNull() ?: return null
    val minute = time[1].toIntOrNull() ?: return null
    val second = time[2].toIntOrNull() ?: return null

    val days = daysBeforeYear(year) - daysBeforeYear(1970) + daysBeforeMonth(year, month) + day - 1
    return ((days * 24L + hour) * 60L + minute) * 60_000L + second * 1_000L
}

private fun daysBeforeYear(year: Int): Long {
    val previousYear = year - 1L
    return previousYear * 365L + previousYear / 4L - previousYear / 100L + previousYear / 400L
}

private fun daysBeforeMonth(year: Int, month: Int): Int {
    val daysBeforeMonth = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
    if (month !in 1..12) return 0
    val leapDay = if (month > 2 && isLeapYear(year)) 1 else 0
    return daysBeforeMonth[month - 1] + leapDay
}

private fun isLeapYear(year: Int): Boolean =
    year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)

private fun defaultFirestoreHttpClient(): HttpClient =
    HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = 10_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 10_000
        }
    }

private val firestoreJson = Json { ignoreUnknownKeys = true }
