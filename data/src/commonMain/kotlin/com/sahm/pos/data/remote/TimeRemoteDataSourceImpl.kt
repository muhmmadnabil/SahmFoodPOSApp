package com.sahm.pos.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull

private const val TIME_API_URL = "https://timeapi.io/api/v1/time/current/unix_ms"

class TimeRemoteDataSourceImpl(
    private val client: HttpClient = defaultTimeHttpClient(),
    private val url: String = TIME_API_URL,
) : TimeRemoteDataSource {
    override suspend fun getUnixTimeMillis(): Result<Long> =
        try {
            val response = client.get(url)
            if (response.status.value >= HttpStatusCode.InternalServerError.value) {
                Result.failure(RemoteDataException.ServerError)
            } else if (response.status.value !in 200..299) {
                Result.failure(RemoteDataException.Unknown)
            } else {
                val timestamp = response.bodyAsText().parseUnixTimestamp()
                if (timestamp > 0) {
                    Result.success(timestamp)
                } else {
                    Result.failure(RemoteDataException.InvalidRemoteData)
                }
            }
        } catch (exception: HttpRequestTimeoutException) {
            Result.failure(RemoteDataException.RequestTimeout)
        } catch (exception: IOException) {
            Result.failure(RemoteDataException.NoInternet)
        } catch (exception: SerializationException) {
            Result.failure(RemoteDataException.SerializationError)
        } catch (exception: IllegalArgumentException) {
            Result.failure(RemoteDataException.SerializationError)
        }
}

private fun String.parseUnixTimestamp(): Long {
    val element = Json.parseToJsonElement(this) as? JsonObject
        ?: throw SerializationException("Expected JSON object.")
    val primitive = element["unix_timestamp"] as? JsonPrimitive
        ?: throw SerializationException("Missing unix_timestamp.")
    if (primitive.isString) {
        throw SerializationException("unix_timestamp must be a number.")
    }
    return primitive.longOrNull
        ?: throw SerializationException("unix_timestamp must be a number.")
}

private fun defaultTimeHttpClient(): HttpClient =
    HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 5_000
            connectTimeoutMillis = 5_000
            socketTimeoutMillis = 5_000
        }
    }