package com.sahm.pos.domain.entity

enum class OrderStatus {
    PendingPayment,
    Paid,
    Cancelled,
    Refunded,
    PartiallyRefunded,
}