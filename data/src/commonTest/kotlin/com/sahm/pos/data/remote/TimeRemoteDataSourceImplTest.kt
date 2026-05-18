package com.sahm.pos.data.remote

import com.sahm.pos.domain.DataError
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class TimeRemoteDataSourceImplTest {
    @Test
    fun parsesValidResponse() = runTest {
        val result = dataSource("""{ "unix_timestamp": 1778895566921 }""").getUnixTimeMillis()

        assertEquals(1778895566921, result.getOrThrow())
    }

    @Test
    fun missingUnixTimestampMapsToSerializationError() = runTest {
        val result = dataSource("""{ "wrong_field": 1778895566921 }""").getUnixTimeMillis()

        assertFailure(DataError.Remote.SERIALIZATION_ERROR, result)
    }

    @Test
    fun wrongTypeMapsToSerializationError() = runTest {
        val result = dataSource("""{ "unix_timestamp": "1778895566921" }""").getUnixTimeMillis()

        assertFailure(DataError.Remote.SERIALIZATION_ERROR, result)
    }

    @Test
    fun httpErrorMapsToServerError() = runTest {
        val result = TimeRemoteDataSourceImpl(
            client = client(MockEngine { respondError(HttpStatusCode.InternalServerError) }),
            url = "https://example.test/time",
        ).getUnixTimeMillis()

        assertFailure(DataError.Remote.SERVER_ERROR, result)
    }

    @Test
    fun timeoutMapsToRequestTimeout() = runTest {
        val result = TimeRemoteDataSourceImpl(
            client = client(MockEngine { throw HttpRequestTimeoutException("https://example.test/time", 1) }),
            url = "https://example.test/time",
        ).getUnixTimeMillis()

        assertFailure(DataError.Remote.REQUEST_TIMEOUT, result)
    }

    @Test
    fun noInternetMapsToNoInternetConnection() = runTest {
        val result = TimeRemoteDataSourceImpl(
            client = client(MockEngine { throw IOException("offline") }),
            url = "https://example.test/time",
        ).getUnixTimeMillis()

        assertFailure(DataError.Remote.NO_INTERNET_CONNECTION, result)
    }

    private fun dataSource(json: String) = TimeRemoteDataSourceImpl(
        client = client(
            MockEngine {
                respond(
                    content = json,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            },
        ),
        url = "https://example.test/time",
    )

    private fun client(engine: MockEngine) = HttpClient(engine) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private fun assertFailure(error: DataError.Remote, result: Result<Long>) {
        val exception = result.exceptionOrNull() as RemoteDataException
        assertEquals(error, exception.toDataError())
    }

    private fun RemoteDataException.toDataError(): DataError.Remote =
        when (this) {
            RemoteDataException.NoInternet -> DataError.Remote.NO_INTERNET_CONNECTION
            RemoteDataException.RequestTimeout -> DataError.Remote.REQUEST_TIMEOUT
            RemoteDataException.PermissionDenied -> DataError.Remote.PERMISSION_DENIED
            RemoteDataException.SerializationError -> DataError.Remote.SERIALIZATION_ERROR
            RemoteDataException.InvalidRemoteData -> DataError.Remote.INVALID_REMOTE_DATA
            RemoteDataException.ServerError -> DataError.Remote.SERVER_ERROR
            RemoteDataException.Unknown -> DataError.Remote.UNKNOWN
        }
}
