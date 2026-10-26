package com.rpstudio.benewallet.core.auth

import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private fun request() = Request.Builder().url("http://localhost:8080/balance").build()

private fun unauthorized(request: Request, prior: Response? = null) = Response.Builder()
    .request(request)
    .protocol(Protocol.HTTP_1_1)
    .code(401)
    .message("Unauthorized")
    .priorResponse(prior)
    .build()

class AuthInterceptorTest {
    private val session = FakeSessionManager()

    private fun send(): Request {
        var sent: Request? = null
        // Stands in for the network: returns right away and keeps what the interceptor produced
        val terminal = Interceptor { chain ->
            sent = chain.request()
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .build()
        }
        OkHttpClient.Builder().addInterceptor(AuthInterceptor(session)).addInterceptor(terminal).build()
            .newCall(request()).execute().close()
        return checkNotNull(sent)
    }

    @Test
    fun `the access token is sent as a bearer header`() {
        assertEquals("Bearer token-1", send().header("Authorization"))
    }

    @Test
    fun `without a session no header is sent`() {
        session.token = null

        assertNull(send().header("Authorization"))
    }

    @Test
    fun `an HTTP 401 is retried once with the renewed token`() = runTest {
        val rejected = request().newBuilder().header("Authorization", "Bearer token-1").build()

        val retry = TokenAuthenticator(session).authenticate(null, unauthorized(rejected))

        assertEquals("Bearer token-2", retry?.header("Authorization"))
        assertEquals(listOf("token-1"), session.rejected)
    }

    @Test
    fun `a retry that is rejected again gives up`() = runTest {
        val rejected = request().newBuilder().header("Authorization", "Bearer token-2").build()
        val first = unauthorized(request().newBuilder().header("Authorization", "Bearer token-1").build())

        val retry = TokenAuthenticator(session).authenticate(null, unauthorized(rejected, prior = first))

        assertNull(retry)
        assertEquals("no new renewal may be tried", emptyList<String>(), session.rejected)
    }

    @Test
    fun `a request that never had a token is not retried`() = runTest {
        val retry = TokenAuthenticator(session).authenticate(null, unauthorized(request()))

        assertNull(retry)
        assertEquals(emptyList<String>(), session.rejected)
    }

    @Test
    fun `no retry when the session could not be renewed`() = runTest {
        session.refreshedTo = null
        val rejected = request().newBuilder().header("Authorization", "Bearer token-1").build()

        assertNull(TokenAuthenticator(session).authenticate(null, unauthorized(rejected)))
    }
}
