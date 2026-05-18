package com.sahm.pos.domain.sync

object SyncIdempotencyKey {
    fun create(type: SyncOutboxType, aggregateId: String): String =
        "${type.name}:$aggregateId"
}

