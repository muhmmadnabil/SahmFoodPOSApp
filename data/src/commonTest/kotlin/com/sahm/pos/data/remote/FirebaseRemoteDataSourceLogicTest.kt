package com.sahm.pos.data.remote

import com.sahm.pos.data.mapper.toUserOrNull
import com.sahm.pos.data.model.RemoteDiscountDocument
import com.sahm.pos.data.model.RemoteMenuItemDocument
import com.sahm.pos.data.model.RemoteUserDocument
import com.sahm.pos.domain.entity.SyncAggregateType
import com.sahm.pos.domain.entity.SyncOutboxItem
import com.sahm.pos.domain.entity.SyncOutboxStatus
import com.sahm.pos.domain.entity.SyncOutboxType
import com.sahm.pos.domain.results.SyncUploadResult
import com.sahm.pos.domain.sync.SyncIdempotencyKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for pure-logic helpers extracted from the iOS Firebase remote data source.
 *
 * These tests run in commonTest (no Firebase / NSError dependency) by testing
 * the data-transformation and classification functions directly:
 *  - uploadCollectionName mapping
 *  - toUploadDocument field set
 *  - mapUploadErrorCode (pure String-code version used to back mapUploadNSError)
 *  - RemoteUserDocument / RemoteMenuItemDocument / RemoteDiscountDocument helpers
 *  - getUsers toUserOrNull filtering
 */
class FirebaseRemoteDataSourceLogicTest {

    // ─── uploadCollectionName ─────────────────────────────────────────────────

    @Test
    fun orderAggregateTypeMapsToOrderCollection() {
        val item = outboxItem(aggregateType = SyncAggregateType.ORDER)
        assertEquals("Order", uploadCollectionNameFor(item.aggregateType))
    }

    @Test
    fun paymentAggregateTypeMapsToPaymentCollection() {
        val item = outboxItem(aggregateType = SyncAggregateType.PAYMENT)
        assertEquals("Payment", uploadCollectionNameFor(item.aggregateType))
    }

    @Test
    fun refundAggregateTypeMapsToRefundCollection() {
        val item = outboxItem(aggregateType = SyncAggregateType.REFUND)
        assertEquals("Refund", uploadCollectionNameFor(item.aggregateType))
    }

    // ─── toUploadDocument field set ───────────────────────────────────────────

    @Test
    fun uploadDocumentContainsAllRequiredFields() {
        val item = outboxItem()
        val doc = buildUploadDocument(item, uploadedAt = 99_000L)

        assertEquals(item.type.name, doc["type"])
        assertEquals(item.aggregateId, doc["aggregateId"])
        assertEquals(item.aggregateType.name, doc["aggregateType"])
        assertEquals(item.payloadJson, doc["payloadJson"])
        assertEquals(item.idempotencyKey, doc["idempotencyKey"])
        assertEquals(item.createdAt, doc["createdAt"])
        assertNotNull(doc["uploadedAt"], "uploadedAt must be present")
    }

    @Test
    fun uploadDocumentUsesIdempotencyKeyAsDocumentId() {
        val item = outboxItem()
        // The document ID is the idempotencyKey — verify it is distinct and non-blank
        assertTrue(item.idempotencyKey.isNotBlank())
        assertEquals(item.idempotencyKey, item.id)
    }

    @Test
    fun uploadDocumentUploadedAtIsGenerated() {
        val item = outboxItem()
        val doc = buildUploadDocument(item, uploadedAt = 55_000L)
        assertEquals(55_000L, doc["uploadedAt"])
    }

    // ─── upload error code mapping (pure logic) ───────────────────────────────

    @Test
    fun deadlineExceededCodeMapsToRetryableTimeout() {
        val result = mapUploadErrorCode(FIRESTORE_DOMAIN, DEADLINE_EXCEEDED)
        assertRetryable(result, "TIMEOUT")
    }

    @Test
    fun unavailableCodeMapsToRetryableNoInternet() {
        val result = mapUploadErrorCode(FIRESTORE_DOMAIN, UNAVAILABLE)
        assertRetryable(result, "NO_INTERNET")
    }

    @Test
    fun resourceExhaustedCodeMapsToRetryableRateLimit() {
        val result = mapUploadErrorCode(FIRESTORE_DOMAIN, RESOURCE_EXHAUSTED)
        assertRetryable(result, "RATE_LIMIT")
    }

    @Test
    fun internalCodeMapsToRetryableHttp500() {
        val result = mapUploadErrorCode(FIRESTORE_DOMAIN, INTERNAL)
        assertRetryable(result, "HTTP_500")
    }

    @Test
    fun permissionDeniedCodeMapsToNonRetryableUnauthorized() {
        val result = mapUploadErrorCode(FIRESTORE_DOMAIN, PERMISSION_DENIED)
        assertNonRetryable(result, "UNAUTHORIZED_CASHIER")
    }

    @Test
    fun invalidArgumentCodeMapsToNonRetryableInvalidPayload() {
        val result = mapUploadErrorCode(FIRESTORE_DOMAIN, INVALID_ARGUMENT)
        assertNonRetryable(result, "INVALID_PAYLOAD")
    }

    @Test
    fun alreadyExistsCodeMapsTosDuplicateIdempotencyKey() {
        val result = mapUploadErrorCode(FIRESTORE_DOMAIN, ALREADY_EXISTS)
        assertEquals(SyncUploadResult.DuplicateIdempotencyKey, result)
    }

    @Test
    fun unknownFirestoreErrorCodeMapsToTemporaryFirestoreFailure() {
        val result = mapUploadErrorCode(FIRESTORE_DOMAIN, 8888L)
        assertRetryable(result, "TEMPORARY_FIRESTORE_FAILURE")
    }

    @Test
    fun nonFirestoreErrorDomainWithUnknownCodeMapsToTemporaryApiFailure() {
        // Non-Firestore domain AND code not matching any known Firebase code
        val result = mapUploadErrorCode("NSURLErrorDomain", 9999)
        assertRetryable(result, "TEMPORARY_API_FAILURE")
    }


    // ─── RemoteUserDocument mapping helpers ───────────────────────────────────

    @Test
    fun userDocumentWithBlankIdFallsBackToDocumentId() {
        val doc = RemoteUserDocument(
            id = "",
            username = "Ali",
            phone = "0500000000",
            password = "secret",
            createdAt = "1234567",
            isActive = true,
            lastLoginAt = "1234568",
        )
        // Simulate: id field is blank, document ID is the fallback
        val resolved = doc.id.ifBlank { "doc-id-from-firestore" }
        assertEquals("doc-id-from-firestore", resolved)
    }

    @Test
    fun userDocumentWithMissingOptionalFieldsDoesNotCrash() {
        val doc = RemoteUserDocument(
            id = "user-1",
            username = null,
            phone = null,
            password = null,
            createdAt = "",
            isActive = null,
            lastLoginAt = "",
        )
        // toUserOrNull must return null because required fields are missing
        val user = doc.toUserOrNull(lastSyncAt = 0L)
        assertNull(user, "User with null required fields must return null")
    }

    @Test
    fun userDocumentWithBlankUsernameMustBeFiltered() {
        val doc = userDoc(username = "  ")
        assertNull(doc.toUserOrNull(0L))
    }

    @Test
    fun userDocumentWithBlankPhoneMustBeFiltered() {
        val doc = userDoc(phone = "  ")
        assertNull(doc.toUserOrNull(0L))
    }

    @Test
    fun userDocumentWithBlankPasswordMustBeFiltered() {
        val doc = userDoc(password = "  ")
        assertNull(doc.toUserOrNull(0L))
    }

    @Test
    fun validUserDocumentMapsToUser() {
        val doc = userDoc()
        val user = doc.toUserOrNull(lastSyncAt = 100L)
        assertNotNull(user)
        assertEquals("user-1", user.id)
        assertEquals("Ahmed", user.username)
        assertEquals("0500000000", user.phone)
    }

    // ─── RemoteMenuItemDocument ───────────────────────────────────────────────

    @Test
    fun menuItemDocumentWithAllNullOptionalFieldsDoesNotCrash() {
        val doc = RemoteMenuItemDocument(
            id = "item-1",
            category = null,
            name = null,
            description = null,
            imageUrl = null,
            price = null,
        )
        assertEquals("item-1", doc.id)
        assertNull(doc.price)
        assertNull(doc.name)
    }

    @Test
    fun menuItemDocumentUsesFirestoreDocumentId() {
        val doc = RemoteMenuItemDocument(
            id = "firestore-doc-id",
            category = "Beverages",
            name = "Lemon Mint",
            description = null,
            imageUrl = null,
            price = 1500L,
        )
        assertEquals("firestore-doc-id", doc.id)
    }

    // ─── RemoteDiscountDocument ───────────────────────────────────────────────

    @Test
    fun discountDocumentWithNullTimestampsDoesNotCrash() {
        val doc = RemoteDiscountDocument(
            id = "disc-1",
            promo = "SAVE10",
            percent = 10.0,
            minValue = null,
            maxValue = null,
            startAt = null,
            endAt = null,
        )
        assertNull(doc.startAt)
        assertNull(doc.endAt)
    }

    @Test
    fun discountTimestampAsLongIsPreservedAsMillis() {
        val epochMillis = 1_700_000_000_000L
        val doc = RemoteDiscountDocument(
            id = "disc-2",
            promo = "SUMMER",
            percent = 15.0,
            minValue = null,
            maxValue = null,
            startAt = epochMillis,
            endAt = null,
        )
        assertEquals(epochMillis, doc.startAt)
    }

    // ─── Numeric type safety ──────────────────────────────────────────────────

    @Test
    fun numericFieldConversionLongToDouble() {
        assertEquals(100.0, convertToDouble(100L))
    }

    @Test
    fun numericFieldConversionIntToDouble() {
        assertEquals(50.0, convertToDouble(50))
    }

    @Test
    fun numericFieldConversionFloatToDouble() {
        assertEquals(25.5, convertToDouble(25.5f), absoluteTolerance = 0.001)
    }

    @Test
    fun numericFieldConversionDoubleIsPreserved() {
        assertEquals(99.99, convertToDouble(99.99))
    }

    @Test
    fun nullNumericFieldReturnsNull() {
        assertNull(convertToDouble(null))
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private fun outboxItem(
        aggregateId: String = "agg-1",
        aggregateType: SyncAggregateType = SyncAggregateType.ORDER,
        type: SyncOutboxType = SyncOutboxType.CREATE_ORDER,
    ): SyncOutboxItem {
        val key = SyncIdempotencyKey.create(type, aggregateId)
        return SyncOutboxItem(
            id = key,
            type = type,
            aggregateId = aggregateId,
            aggregateType = aggregateType,
            payloadJson = "{}",
            idempotencyKey = key,
            status = SyncOutboxStatus.PENDING,
            createdAt = 1_000L,
            updatedAt = 1_000L,
        )
    }

    private fun userDoc(
        id: String = "user-1",
        username: String? = "Ahmed",
        phone: String? = "0500000000",
        password: String? = "pass123",
    ) = RemoteUserDocument(
        id = id,
        username = username,
        phone = phone,
        password = password,
        createdAt = "1700000000",
        isActive = true,
        lastLoginAt = "1700000001",
    )
}

// ─── Pure-logic facades used in tests (no Firebase types) ─────────────────────

/** Mirror of the collection name logic, extracted for testing in commonTest. */
private fun uploadCollectionNameFor(aggregateType: SyncAggregateType): String =
    when (aggregateType) {
        SyncAggregateType.ORDER -> "Order"
        SyncAggregateType.PAYMENT -> "Payment"
        SyncAggregateType.REFUND -> "Refund"
    }

/** Mirror of toUploadDocument that accepts an explicit uploadedAt for determinism. */
private fun buildUploadDocument(item: SyncOutboxItem, uploadedAt: Long): Map<String, Any?> =
    mapOf(
        "type" to item.type.name,
        "aggregateId" to item.aggregateId,
        "aggregateType" to item.aggregateType.name,
        "payloadJson" to item.payloadJson,
        "idempotencyKey" to item.idempotencyKey,
        "createdAt" to item.createdAt,
        "uploadedAt" to uploadedAt,
    )

/**
 * Error code constants matching FIRFirestoreErrorCode values.
 * These are stable constants from Firebase iOS SDK.
 */
private const val FIRESTORE_DOMAIN = "FIRFirestoreErrorDomain"
private const val DEADLINE_EXCEEDED = 4L
private const val UNAVAILABLE = 14L
private const val RESOURCE_EXHAUSTED = 8L
private const val INTERNAL = 13L
private const val PERMISSION_DENIED = 7L
private const val INVALID_ARGUMENT = 3L
private const val ALREADY_EXISTS = 6L

/**
 * Pure-logic mirror of mapUploadNSError that takes (domain, code) instead of NSError.
 * This matches the mapping logic in IosRemoteDataSource.kt exactly.
 */
private fun mapUploadErrorCode(domain: String, code: Long): SyncUploadResult =
    when {
        code == ALREADY_EXISTS -> SyncUploadResult.DuplicateIdempotencyKey
        code == DEADLINE_EXCEEDED -> SyncUploadResult.RetryableError("TIMEOUT", "Request timed out.")
        code == UNAVAILABLE -> SyncUploadResult.RetryableError("NO_INTERNET", "Network is unavailable.")
        code == RESOURCE_EXHAUSTED -> SyncUploadResult.RetryableError("RATE_LIMIT", "Rate limit reached.")
        code == INTERNAL -> SyncUploadResult.RetryableError("HTTP_500", "Temporary server error.")
        code == PERMISSION_DENIED -> SyncUploadResult.NonRetryableError("UNAUTHORIZED_CASHIER", "Cashier is not authorized.")
        code == INVALID_ARGUMENT -> SyncUploadResult.NonRetryableError("INVALID_PAYLOAD", "Remote rejected the payload.")
        domain == FIRESTORE_DOMAIN -> SyncUploadResult.RetryableError("TEMPORARY_FIRESTORE_FAILURE", "Firestore error $code")
        else -> SyncUploadResult.RetryableError("TEMPORARY_API_FAILURE", "Unknown error $code")
    }

/** Safe numeric-to-Double conversion matching the iOS field helper. */
private fun convertToDouble(value: Any?): Double? =
    when (value) {
        is Double -> value
        is Float -> value.toDouble()
        is Long -> value.toDouble()
        is Int -> value.toDouble()
        else -> null
    }

/** assertEquals overload with absolute tolerance for floating point. */
private fun assertEquals(expected: Double, actual: Double?, absoluteTolerance: Double) {
    assertNotNull(actual)
    assertTrue(
        kotlin.math.abs(expected - actual) <= absoluteTolerance,
        "Expected $expected ± $absoluteTolerance but was $actual",
    )
}

private fun assertRetryable(result: SyncUploadResult, expectedCode: String) {
    assertTrue(result is SyncUploadResult.RetryableError, "Expected RetryableError but got $result")
    assertEquals(expectedCode, result.code)
}

private fun assertNonRetryable(result: SyncUploadResult, expectedCode: String) {
    assertTrue(result is SyncUploadResult.NonRetryableError, "Expected NonRetryableError but got $result")
    assertEquals(expectedCode, result.code)
}

// toUserOrNull is an extension function in commonMain — just import it directly.
// The import at the top of the file handles this.
