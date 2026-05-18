package com.sahm.pos.domain

sealed interface DataError {
    enum class Remote : DataError {
        NO_INTERNET_CONNECTION,
        REQUEST_TIMEOUT,
        SERVER_ERROR,
        SERIALIZATION_ERROR,
        INVALID_REMOTE_DATA,
        PERMISSION_DENIED,
        UNKNOWN,
    }

    enum class Local : DataError {
        LOCAL_STORAGE_ERROR,
    }
}
