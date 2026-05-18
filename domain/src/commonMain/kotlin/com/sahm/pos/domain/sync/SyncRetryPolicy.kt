package com.sahm.pos.domain.sync

object SyncRetryPolicy {
    fun delayMillisForRetryCount(retryCount: Int): Long =
        when (retryCount) {
            //1 Minute, 2 Minutes, 5 Minutes, 15 Minutes, 30 Minutes, 1 Hour
            0 -> 60_000L
            1 -> 120_000L
            2 -> 300_000L
            3 -> 900_000L
            4 -> 1_800_000L
            else -> 3_600_000L
        }

    fun isRetryable(code: String): Boolean =
        code.uppercase() in setOf(
            "NO_INTERNET",
            "TIMEOUT",
            "HTTP_500",
            "HTTP_503",
            "SERVER_UNAVAILABLE",
            "RATE_LIMIT",
            "TEMPORARY_API_FAILURE",
            "TEMPORARY_FIRESTORE_FAILURE",
        )
}
