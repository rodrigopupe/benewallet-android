package com.rpstudio.benewallet.bff

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json

private val ZONE = ZoneId.of("America/Sao_Paulo")
private val CLOCK = Clock.fixed(Instant.parse("2026-10-24T15:00:00Z"), ZONE)
private val JSON = Json { ignoreUnknownKeys = true }

private suspend fun ApplicationTestBuilder.fetchPage(query: String = ""): TransactionsResponse {
    val response = client.get("/transactions$query")
    val body = response.bodyAsText()
    assertEquals(HttpStatusCode.OK, response.status, "GET /transactions$query -> $body")
    return JSON.decodeFromString(body)
}
class WalletRoutesTest {
    @Test
    fun `balance has one non-negative entry per benefit`() = testApplication {
        application { bffModule(CLOCK) }

        val body = JSON.decodeFromString<BalanceResponse>(client.get("/balance").bodyAsText())

        assertEquals(Benefit.entries.toSet(), body.balances.map { it.benefit }.toSet())
        assertTrue(body.balances.all { it.balanceCents >= 0 })
    }

    @Test
    fun `first page is newest first and has a next cursor`() = testApplication {
        application { bffModule(CLOCK) }

        val page = fetchPage("?limit=10")

        assertEquals(10, page.items.size)
        assertTrue(page.nextCursor != null)
        assertEquals(
            page.items.map { it.occurredAt }.sortedDescending(),
            page.items.map { it.occurredAt }
        )
    }

    @Test
    fun `following nextCursor visits every transaction exactly once`() = testApplication {
        application { bffModule(CLOCK) }
        val ids = mutableListOf<Long>()
        var cursor: String? = null

        do {
            val query = if (cursor == null) "?limit=15" else "?limit=15&cursor=$cursor"
            val page = fetchPage(query)
            ids += page.items.map { it.id }
            cursor = page.nextCursor
        } while (cursor != null)

        assertTrue(ids.size > 15)
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `category filter returns only that category`() = testApplication {
        application { bffModule(CLOCK) }

        val page = fetchPage("?category=restaurant&limit=50")

        assertTrue(page.items.isNotEmpty())
        assertTrue(page.items.all { it.category == Category.RESTAURANT })
    }

    @Test
    fun `date filter is inclusive on both ends`() = testApplication {
        application { bffModule(CLOCK) }
        fun TransactionDto.day() = Instant.parse(occurredAt).atZone(ZONE).toLocalDate()

        val newest = fetchPage("?limit=50").items
        val day = newest.first().day()

        val page = fetchPage("?from=$day&to=$day&limit=50")

        assertTrue(
            page.items.isNotEmpty(),
            "no items returned for $day; newest item was ${newest.first()}"
        )
        val outside = page.items.filter { it.day() != day }
        assertTrue(outside.isEmpty(), "items outside $day: $outside")
        assertEquals(newest.count { it.day() == day }, page.items.size)
    }

    @Test
    fun `data is deterministic for the same clock`() = testApplication {
        application { bffModule(CLOCK) }

        assertEquals(fetchPage("?limit=50"), fetchPage("?limit=50"))
    }

    @Test
    fun `invalid cursor returns 400`() = testApplication {
        application { bffModule(CLOCK) }

        val response = client.get("/transactions?cursor=not-a-cursor")

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("cursor"))
    }

    @Test
    fun `feature flags are resolved for the user`() = testApplication {
        application { bffModule(CLOCK) }

        val response = client.get("/feature-flags") { header("X-User-Id", "user-1") }
        val body = JSON.decodeFromString<FeatureFlagsResponse>(response.bodyAsText())

        assertTrue(body.flags.first { it.key == "goals" }.enabled)
    }

    @Test
    fun `each invalid query parameter returns 400 naming the parameter`() = testApplication {
        application { bffModule(CLOCK) }

        for (param in listOf("category=xyz", "from=20-10-2026", "to=abc", "cursor=not-a-cursor")) {
            val response = client.get("/transactions?$param")

            assertEquals(HttpStatusCode.BadRequest, response.status, param)
            assertTrue(
                response.bodyAsText().contains(param.substringBefore("=")),
                param
            )
        }
    }
}
