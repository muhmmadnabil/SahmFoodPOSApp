package com.sahm.pos.domain.sync

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class SyncIdempotencyKeyTest {
    @Test
    fun createOrderKeyIsStable() {
        assertEquals("CREATE_ORDER:order-1", SyncIdempotencyKey.create(SyncOutboxType.CREATE_ORDER, "order-1"))
        assertEquals(
            SyncIdempotencyKey.create(SyncOutboxType.CREATE_ORDER, "order-1"),
            SyncIdempotencyKey.create(SyncOutboxType.CREATE_ORDER, "order-1"),
        )
    }

    @Test
    fun paymentAndRefundKeysUseAggregateIds() {
        assertEquals("CREATE_PAYMENT:payment-1", SyncIdempotencyKey.create(SyncOutboxType.CREATE_PAYMENT, "payment-1"))
        assertEquals("CREATE_REFUND:refund-1", SyncIdempotencyKey.create(SyncOutboxType.CREATE_REFUND, "refund-1"))
    }

    @Test
    fun typeAndAggregateBothAffectKey() {
        assertNotEquals(
            SyncIdempotencyKey.create(SyncOutboxType.CREATE_ORDER, "same-id"),
            SyncIdempotencyKey.create(SyncOutboxType.CREATE_PAYMENT, "same-id"),
        )
        assertNotEquals(
            SyncIdempotencyKey.create(SyncOutboxType.CREATE_ORDER, "order-1"),
            SyncIdempotencyKey.create(SyncOutboxType.CREATE_ORDER, "order-2"),
        )
    }
}
