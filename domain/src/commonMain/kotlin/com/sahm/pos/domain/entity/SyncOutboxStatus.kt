package com.sahm.pos.domain.entity

enum class SyncOutboxStatus {
    PENDING,
    IN_PROGRESS,
    SUCCEEDED,
    RETRY_WAITING,
    FAILED,
    CONFLICT,
}