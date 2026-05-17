package com.sahm.pos.domain.entity

data class SyncOutboxItem(
    val id: String,
    val type: SyncOutboxType,
    val aggregateId: String,
    val aggregateType: SyncAggregateType,
    val payloadJson: String,
    val idempotencyKey: String,
    val status: SyncOutboxStatus,
    val retryCount: Int = 0,
    val maxRetries: Int = 10,
    val nextAttemptAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val lockedAt: Long? = null,
    val lastErrorCode: String? = null,
    val lastErrorMessage: String? = null,
)