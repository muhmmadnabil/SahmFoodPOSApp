package com.sahm.pos.data.remote

sealed class RemoteDataException : Exception() {
    data object NoInternet : RemoteDataException()
    data object PermissionDenied : RemoteDataException()
}
