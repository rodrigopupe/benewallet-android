package com.rpstudio.benewallet.bff

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.bearer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import java.time.Clock
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val DEFAULT_PORT = 8080
private const val DEFAULT_HOST = "0.0.0.0"
private const val AUTH_NAME = "access-token"
private val BRAZIL_ZONE: ZoneId = ZoneId.of("America/Sao_Paulo")

fun main() {
    embeddedServer(
        Netty,
        port = DEFAULT_PORT,
        host = DEFAULT_HOST,
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {
    bffModule(Clock.system(BRAZIL_ZONE))
}

internal fun Application.bffModule(clock: Clock) {
    install(ContentNegotiation) {
        json(
            Json {
                encodeDefaults = true
                ignoreUnknownKeys = true
            }
        )
    }

    val auth = AuthService(clock)
    install(Authentication) {
        bearer(AUTH_NAME) {
            realm = "benewallet"
            authenticate { credential -> auth.userIdFor(credential.token)?.let(::UserIdPrincipal) }
        }
    }

    // Each account has its own mock data (a different seed), built once on first use
    val dataByUser = ConcurrentHashMap<String, MockWalletData>()

    routing {
        get("/health") {
            call.respond(HealthResponse(status = "ok"))
        }
        authRoutes(auth)
        authenticate(AUTH_NAME) {
            walletRoutes({ userId ->
                dataByUser.getOrPut(userId) { MockWalletData(clock, auth.seedFor(userId)) }
            }, clock)
        }
    }
}

@Serializable
data class HealthResponse(val status: String)
