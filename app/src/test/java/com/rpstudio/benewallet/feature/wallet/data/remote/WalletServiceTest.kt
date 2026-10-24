package com.rpstudio.benewallet.feature.wallet.data.remote

import com.rpstudio.benewallet.core.network.networkJson
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

private const val JSON = "application/json"

private class RecordingInterceptor(private val body: String) : Interceptor {
    var lastRequest: Request? = null

    override fun intercept(chain: Interceptor.Chain): Response {
        lastRequest = chain.request()
        return Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(body.toResponseBody(JSON.toMediaType()))
            .build()
    }
}

private fun buildApi(interceptor: Interceptor): WalletService = Retrofit.Builder()
    .baseUrl("http://localhost:8080")
    .client(OkHttpClient.Builder().addInterceptor(interceptor).build())
    .addConverterFactory(networkJson.asConverterFactory(JSON.toMediaType()))
    .build().create(WalletService::class.java)

class WalletServiceTest {
    private val emptyPage = """{"items":[],"nextCursor":null}"""

    @Test
    fun `transactions sends cursor and filters as query parameters`() = runBlocking {
        val recorder = RecordingInterceptor(emptyPage)

        buildApi(recorder).getTransactions(
            cursor = "abc",
            category = "GROCERY",
            from = "2026-10-01",
            to = "2026-10-24",
            limit = 20
        )

        val url = checkNotNull(recorder.lastRequest).url
        assertEquals("/transactions", url.encodedPath)
        assertEquals("abc", url.queryParameter("cursor"))
        assertEquals("GROCERY", url.queryParameter("category"))
        assertEquals("2026-10-01", url.queryParameter("from"))
        assertEquals("2026-10-24", url.queryParameter("to"))
        assertEquals("20", url.queryParameter("limit"))
    }

    @Test
    fun `transactions without filters sends no query string`() = runBlocking {
        val recorder = RecordingInterceptor(emptyPage)

        buildApi(recorder).getTransactions()

        assertNull(checkNotNull(recorder.lastRequest).url.query)
    }

    @Test
    fun `feature flags sends the user id header`() = runBlocking {
        val recorder = RecordingInterceptor("""{"flags":[]}""")

        buildApi(recorder).getFeatureFlags("user-1")

        val request = checkNotNull(recorder.lastRequest)
        assertEquals("/feature-flags", request.url.encodedPath)
        assertEquals("user-1", request.header("X-User-Id"))
    }
}
