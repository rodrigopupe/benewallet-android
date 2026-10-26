package com.rpstudio.benewallet.bff

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json

private val NOW = Instant.parse("2026-10-24T15:00:00Z")
private val ZONE = ZoneId.of("America/Sao_Paulo")
private val JSON = Json { ignoreUnknownKeys = true }

class AuthRoutesTest {
    @Test
    fun `health stays public`() = testApplication {
        application { bffModule(MutableClock(NOW, ZONE)) }

        assertEquals(HttpStatusCode.OK, client.get("/health").status)
    }

    @Test
    fun `data routes reject requests without a valid token`() = testApplication {
        application { bffModule(MutableClock(NOW, ZONE)) }

        for (path in listOf("/balance", "/transactions", "/feature-flags")) {
            assertEquals(HttpStatusCode.Unauthorized, client.get(path).status, "$path without token")
            val forged = client.get(path) { header(HttpHeaders.Authorization, "Bearer forged-token") }
            assertEquals(HttpStatusCode.Unauthorized, forged.status, "$path with a forged token")
        }
    }

    @Test
    fun `login returns tokens that open the data routes`() = testApplication {
        application { bffModule(MutableClock(NOW, ZONE)) }

        val tokens = login()

        assertEquals(15 * 60L, tokens.expiresInSeconds)
        assertNotEquals(tokens.accessToken, tokens.refreshToken)
        assertEquals(HttpStatusCode.OK, authorizedClient().get("/balance").status)
    }

    @Test
    fun `unknown e-mail and wrong password get the same answer`() = testApplication {
        application { bffModule(MutableClock(NOW, ZONE)) }

        val wrongPassword = postJson("/auth/login", """{"email":"$RODRIGO","password":"nope"}""")
        val unknownUser = postJson("/auth/login", """{"email":"ghost@benewallet.demo","password":"nope"}""")

        assertEquals(HttpStatusCode.Unauthorized, wrongPassword.status)
        assertEquals(HttpStatusCode.Unauthorized, unknownUser.status)
        assertEquals(
            wrongPassword.bodyAsText(),
            unknownUser.bodyAsText(),
            "the API must not reveal which e-mails exist"
        )
    }

    @Test
    fun `malformed login body returns 400`() = testApplication {
        application { bffModule(MutableClock(NOW, ZONE)) }

        assertEquals(HttpStatusCode.BadRequest, postJson("/auth/login", "not json").status)
        assertEquals(HttpStatusCode.BadRequest, postJson("/auth/login", """{"email":"$RODRIGO"}""").status)
    }

    @Test
    fun `five wrong passwords lock the account for a minute`() = testApplication {
        val clock = MutableClock(NOW, ZONE)
        application { bffModule(clock) }
        repeat(5) {
            assertEquals(
                HttpStatusCode.Unauthorized,
                postJson("/auth/login", """{"email":"$RODRIGO","password":"nope"}""").status
            )
        }

        val locked = postJson("/auth/login", """{"email":"$RODRIGO","password":"$DEMO_PASSWORD"}""")
        assertEquals(HttpStatusCode.TooManyRequests, locked.status, "the right password is refused while locked")
        assertEquals("60", locked.headers[HttpHeaders.RetryAfter])

        clock.advance(Duration.ofSeconds(61))
        assertEquals(
            HttpStatusCode.OK,
            postJson("/auth/login", """{"email":"$RODRIGO","password":"$DEMO_PASSWORD"}""").status
        )
    }

    @Test
    fun `access token expires after 15 minutes`() = testApplication {
        val clock = MutableClock(NOW, ZONE)
        application { bffModule(clock) }
        val client = authorizedClient()

        clock.advance(Duration.ofMinutes(14))
        assertEquals(HttpStatusCode.OK, client.get("/balance").status)

        clock.advance(Duration.ofMinutes(2))
        assertEquals(HttpStatusCode.Unauthorized, client.get("/balance").status)
    }

    @Test
    fun `refresh rotates the pair and the new access token works`() = testApplication {
        application { bffModule(MutableClock(NOW, ZONE)) }
        val first = login()

        val response = refresh(first.refreshToken)
        val second = JSON.decodeFromString<TokenResponse>(response.bodyAsText())

        assertEquals(HttpStatusCode.OK, response.status)
        assertNotEquals(first.accessToken, second.accessToken)
        assertNotEquals(first.refreshToken, second.refreshToken)
        val withNewToken = client.get("/balance") { header(HttpHeaders.Authorization, "Bearer ${second.accessToken}") }
        assertEquals(HttpStatusCode.OK, withNewToken.status)
    }

    @Test
    fun `reusing a refresh token revokes the whole session`() = testApplication {
        application { bffModule(MutableClock(NOW, ZONE)) }
        val first = login()
        val second = JSON.decodeFromString<TokenResponse>(refresh(first.refreshToken).bodyAsText())

        val replay = refresh(first.refreshToken)

        assertEquals(HttpStatusCode.Unauthorized, replay.status)
        assertEquals(
            HttpStatusCode.Unauthorized,
            refresh(second.refreshToken).status,
            "the legitimate holder of the newest tokens is logged out too"
        )
        val stillOpen = client.get("/balance") { header(HttpHeaders.Authorization, "Bearer ${second.accessToken}") }
        assertEquals(HttpStatusCode.Unauthorized, stillOpen.status)
    }

    @Test
    fun `refresh token expires after 7 days`() = testApplication {
        val clock = MutableClock(NOW, ZONE)
        application { bffModule(clock) }
        val tokens = login()

        clock.advance(Duration.ofDays(7).plusMinutes(1))

        assertEquals(HttpStatusCode.Unauthorized, refresh(tokens.refreshToken).status)
    }

    @Test
    fun `an access token is not accepted as a refresh token`() = testApplication {
        application { bffModule(MutableClock(NOW, ZONE)) }

        assertEquals(HttpStatusCode.Unauthorized, refresh(login().accessToken).status)
    }

    @Test
    fun `logout revokes both tokens and is idempotent`() = testApplication {
        application { bffModule(MutableClock(NOW, ZONE)) }
        val tokens = login()

        assertEquals(
            HttpStatusCode.NoContent,
            postJson("/auth/logout", """{"refreshToken":"${tokens.refreshToken}"}""").status
        )
        assertEquals(
            HttpStatusCode.NoContent,
            postJson("/auth/logout", """{"refreshToken":"${tokens.refreshToken}"}""").status
        )

        assertEquals(HttpStatusCode.Unauthorized, refresh(tokens.refreshToken).status)
        val afterLogout = client.get("/balance") { header(HttpHeaders.Authorization, "Bearer ${tokens.accessToken}") }
        assertEquals(HttpStatusCode.Unauthorized, afterLogout.status)
    }

    @Test
    fun `logout of one session leaves other sessions of the same user open`() = testApplication {
        application { bffModule(MutableClock(NOW, ZONE)) }
        val phone = login()
        val tablet = login()

        postJson("/auth/logout", """{"refreshToken":"${phone.refreshToken}"}""")

        val tabletCall = client.get("/balance") { header(HttpHeaders.Authorization, "Bearer ${tablet.accessToken}") }
        assertEquals(HttpStatusCode.OK, tabletCall.status)
    }

    @Test
    fun `each account sees its own data`() = testApplication {
        application { bffModule(MutableClock(NOW, ZONE)) }

        val rodrigo = authorizedClient(RODRIGO).get("/transactions?limit=50").bodyAsText()
        val bruno = authorizedClient(BRUNO).get("/transactions?limit=50").bodyAsText()

        assertTrue(rodrigo.isNotBlank() && bruno.isNotBlank())
        assertNotEquals(rodrigo, bruno)
    }
}
