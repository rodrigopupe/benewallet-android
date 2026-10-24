package com.rpstudio.benewallet.bff

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val DEFAULT_PORT = 8080
private const val DEFAULT_HOST = "0.0.0.0"

fun main() {
    embeddedServer(
        Netty,
        port = DEFAULT_PORT,
        host = DEFAULT_HOST,
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        json(
            Json {
                encodeDefaults = true
                ignoreUnknownKeys = true
            }
        )
    }

    routing {
        get("/health") {
            call.respond(HealthResponse(status = "ok"))
        }
    }
}

@Serializable
data class HealthResponse(val status: String)
