package com.sahm.pos.domain.sync

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SyncRetryPolicyTest {
    @Test
    fun retryDelaysFollowConfiguredBackoff() {
        assertEquals(60_000L, SyncRetryPolicy.delayMillisForRetryCount(0))
        assertEquals(120_000L, SyncRetryPolicy.delayMillisForRetryCount(1))
        assertEquals(300_000L, SyncRetryPolicy.delayMillisForRetryCount(2))
        assertEquals(900_000L, SyncRetryPolicy.delayMillisForRetryCount(3))
        assertEquals(1_800_000L, SyncRetryPolicy.delayMillisForRetryCount(4))
        assertEquals(3_600_000L, SyncRetryPolicy.delayMillisForRetryCount(5))
        assertEquals(3_600_000L, SyncRetryPolicy.delayMillisForRetryCount(99))
    }

    @Test
    fun retryableAndPermanentErrorsAreClassified() {
        assertTrue(SyncRetryPolicy.isRetryable("NO_INTERNET"))
        assertTrue(SyncRetryPolicy.isRetryable("TIMEOUT"))
        assertTrue(SyncRetryPolicy.isRetryable("HTTP_500"))
        assertTrue(SyncRetryPolicy.isRetryable("HTTP_503"))
        assertTrue(SyncRetryPolicy.isRetryable("RATE_LIMIT"))
        assertFalse(SyncRetryPolicy.isRetryable("VALIDATION_FAILED"))
        assertFalse(SyncRetryPolicy.isRetryable("UNAUTHORIZED_CASHIER"))
        assertFalse(SyncRetryPolicy.isRetryable("REFUND_QUANTITY_EXCEEDED"))
    }
}
