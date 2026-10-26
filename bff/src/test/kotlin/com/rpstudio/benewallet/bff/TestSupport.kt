package com.rpstudio.benewallet.bff

import io.ktor.client.HttpClient
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json

internal const val RODRIGO = "rodrigo@benewallet.demo"
internal const val BRUNO = "bruno@benewallet.demo"

private val testJson = Json { ignoreUnknownKeys = true }

internal class MutableClock(private var now: Instant, private val zone: ZoneId) : Clock() {
    override fun getZone(): ZoneId = zone

    override fun withZone(zone: ZoneId): Clock = MutableClock(now, zone)

    override fun instant(): Instant = now

    fun advance(duration: Duration) {
        now = now.plus(duration)
    }
}

internal suspend fun ApplicationTestBuilder.postJson(path: String, body: String): HttpResponse = client.post(path) {
    contentType(ContentType.Application.Json)
    setBody(body)
}

internal suspend fun ApplicationTestBuilder.login(
    email: String = RODRIGO,
    password: String = DEMO_PASSWORD
): TokenResponse {
    val response = postJson("/auth/login", """{"email":"$email","password":"$password"}""")
    val body = response.bodyAsText()
    assertEquals(HttpStatusCode.OK, response.status, "login as $email -> $body")
    return testJson.decodeFromString(body)
}

internal suspend fun ApplicationTestBuilder.refresh(refreshToken: String): HttpResponse =
    postJson("/auth/refresh", """{"refreshToken":"$refreshToken"}""")

/** A client that sends a valid access token, obtained through the real login endpoint. */
internal suspend fun ApplicationTestBuilder.authorizedClient(email: String = RODRIGO): HttpClient {
    val token = login(email).accessToken
    return createClient { defaultRequest { header(HttpHeaders.Authorization, "Bearer $token") } }
}
