package com.sahm.pos.domain.sync

enum class SyncReason {
    AppStarted,
    NetworkRestored,
    OrderCreated,
    PaymentCreated,
    RefundCreated,
    Manual,
    EndShift,
}
