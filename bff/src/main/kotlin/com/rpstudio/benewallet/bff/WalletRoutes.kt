package com.rpstudio.benewallet.bff

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import java.time.Clock
import java.time.LocalDate

private const val DEFAULT_LIMIT = 20
private const val MAX_LIMIT = 50

internal fun Route.walletRoutes(dataFor: (userId: String) -> MockWalletData, clock: Clock) {
    get("/balance") {
        val data = dataFor(call.userId())
        call.respond(
            BalanceResponse(
                balances = Benefit.entries.map { BenefitBalanceDto(it, data.balanceOf(it)) },
                updatedAt = clock.instant().toString()
            )
        )
    }

    get("/transactions") {
        val query = call.request.queryParameters
        val limit = query["limit"]?.toIntOrNull()?.coerceIn(1, MAX_LIMIT) ?: DEFAULT_LIMIT

        val category = query["category"]?.let { raw ->
            Category.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
                ?: return@get call.badRequest("Invalid 'category' parameter")
        }
        val from = query["from"]?.let { parseDate(it) ?: return@get call.badRequest("Invalid 'from' parameter") }
        val to = query["to"]?.let { parseDate(it) ?: return@get call.badRequest("Invalid 'to' parameter") }
        val cursor = query["cursor"]?.let {
            Cursor.decode(it) ?: return@get call.badRequest("Invalid 'cursor' parameter")
        }

        val page = dataFor(call.userId()).page(TransactionFilter(category, from, to), cursor, limit)
        call.respond(TransactionsResponse(page.items.map { it.toDto() }, page.nextCursor?.encode()))
    }

    get("/feature-flags") {
        call.respond(FeatureFlagsResponse(FeatureFlags.resolve(call.userId())))
    }
}

private fun ApplicationCall.userId(): String = checkNotNull(principal<UserIdPrincipal>()) {
    "route is not authenticated"
}.name

private fun parseDate(raw: String): LocalDate? = runCatching { LocalDate.parse(raw) }.getOrNull()

internal suspend fun ApplicationCall.badRequest(message: String) =
    respond(HttpStatusCode.BadRequest, ErrorResponse(message))
