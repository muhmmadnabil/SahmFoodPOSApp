package com.sahm.pos.data.remote

sealed class RemoteDataException : Exception() {
    data object NoInternet : RemoteDataException()
    data object RequestTimeout : RemoteDataException()
    data object PermissionDenied : RemoteDataException()
    data object SerializationError : RemoteDataException()
    data object InvalidRemoteData : RemoteDataException()
    data object ServerError : RemoteDataException()
    data object Unknown : RemoteDataException()
}