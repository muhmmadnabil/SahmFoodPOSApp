package com.sahm.pos.domain.entity

enum class PaymentStatus {
    NotStarted,
    Processing,
    Paid,
    Failed,
    Cancelled,
    Refunded,
    PartiallyRefunded,
}