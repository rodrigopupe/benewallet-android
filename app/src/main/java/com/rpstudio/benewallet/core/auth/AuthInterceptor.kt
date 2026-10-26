package com.rpstudio.benewallet.core.auth

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

private const val HEADER_AUTHORIZATION = "Authorization"
private const val BEARER_PREFIX = "Bearer "

internal class AuthInterceptor(private val sessionManager: SessionManager) : Interceptor {
    // OkHttp calls interceptors on its own worker threads, so blocking here never blocks the main thread
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { sessionManager.accessToken() }
        val request = if (token == null) chain.request() else chain.request().withToken(token)
        return chain.proceed(request)
    }
}

/** On an HTTP 401, renews the session once and retries the request with the new token. */
internal class TokenAuthenticator(private val sessionManager: SessionManager) : okhttp3.Authenticator {
    override fun authenticate(route: okhttp3.Route?, response: Response): Request? {
        // A retry that is rejected again (priorResponse) gives up instead of looping
        val rejected = response.request.header(HEADER_AUTHORIZATION)
            ?.removePrefix(BEARER_PREFIX)
            ?.takeIf { response.priorResponse == null }
        val fresh = rejected?.let { runBlocking { sessionManager.refresh(it) } }
        return fresh?.let { response.request.withToken(it) }
    }
}

private fun Request.withToken(token: String): Request =
    newBuilder().header(HEADER_AUTHORIZATION, BEARER_PREFIX + token).build()
