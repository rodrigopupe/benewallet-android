package com.rpstudio.benewallet.bff

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.ContentTransformationException
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.SerializationException

private const val RETRY_AFTER_SECONDS = "60"

internal fun Route.authRoutes(auth: AuthService) {
    route("/auth") {
        post("/login") {
            val request = call.receiveBody<LoginRequest>() ?: return@post call.badRequest("Invalid request body")
            when (val result = auth.login(request.email, request.password)) {
                is LoginResult.Success -> call.respond(result.tokens.toResponse())

                // Same answer for an unknown e-mail and a wrong password: the API must not reveal which accounts exist
                LoginResult.InvalidCredentials ->
                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid credentials"))

                LoginResult.LockedOut -> {
                    call.response.header(HttpHeaders.RetryAfter, RETRY_AFTER_SECONDS)
                    call.respond(HttpStatusCode.TooManyRequests, ErrorResponse("Too many attempts"))
                }
            }
        }

        post("/refresh") {
            val request = call.receiveBody<RefreshRequest>() ?: return@post call.badRequest("Invalid request body")
            val tokens = auth.refresh(request.refreshToken)
            if (tokens == null) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid refresh token"))
            } else {
                call.respond(tokens.toResponse())
            }
        }

        // Idempotent and unauthenticated: logging out must work even after the access token has expired
        post("/logout") {
            val request = call.receiveBody<RefreshRequest>() ?: return@post call.badRequest("Invalid request body")
            auth.logout(request.refreshToken)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

private fun TokenPair.toResponse() = TokenResponse(accessToken, refreshToken, expiresInSeconds)

private suspend inline fun <reified T : Any> ApplicationCall.receiveBody(): T? = try {
    receive<T>()
} catch (ignored: ContentTransformationException) {
    null
} catch (ignored: SerializationException) {
    null
}
