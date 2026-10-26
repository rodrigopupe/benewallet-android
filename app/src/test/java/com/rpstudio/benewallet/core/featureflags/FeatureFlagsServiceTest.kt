package com.rpstudio.benewallet.core.featureflags

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
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

private const val JSON = "application/json"

class FeatureFlagsServiceTest {
    @Test
    fun `feature flags sends the user id header`() = runBlocking {
        var lastRequest: Request? = null
        val interceptor = Interceptor { chain ->
            lastRequest = chain.request()
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("""{"flags":[]}""".toResponseBody(JSON.toMediaType()))
                .build()
        }
        val service = Retrofit.Builder()
            .baseUrl("http://localhost:8080")
            .client(OkHttpClient.Builder().addInterceptor(interceptor).build())
            .addConverterFactory(networkJson.asConverterFactory(JSON.toMediaType()))
            .build().create(FeatureFlagsService::class.java)

        service.getFeatureFlags("user-1")

        val request = checkNotNull(lastRequest)
        assertEquals("/feature-flags", request.url.encodedPath)
        assertEquals("user-1", request.header("X-User-Id"))
    }
}
