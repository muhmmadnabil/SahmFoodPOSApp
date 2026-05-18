package com.sahm.pos.domain.entity

data class SyncOutboxCounts(
    val pending: Long,
    val failed: Long,
    val conflicts: Long,
) {
    val hasPendingRows: Boolean = pending > 0
    val hasBlockingProblems: Boolean = failed > 0 || conflicts > 0
}