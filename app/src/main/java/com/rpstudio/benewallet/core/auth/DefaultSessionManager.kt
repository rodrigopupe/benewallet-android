package com.rpstudio.benewallet.core.auth

import com.rpstudio.benewallet.core.crash.CrashReporter
import java.io.IOException
import java.time.Clock
import java.time.Duration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException

private val RENEW_MARGIN: Duration = Duration.ofSeconds(30)
private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_TOO_MANY_REQUESTS = 429

internal class DefaultSessionManager(
    private val store: TokenStore,
    private val service: AuthService,
    private val cleaners: List<SessionDataCleaner>,
    private val crashReporter: CrashReporter,
    private val clock: Clock
) : SessionManager {
    // One lock for everything that reads or changes the tokens: concurrent HTTP 401s renew the token only once
    private val mutex = Mutex()
    private var tokens: AuthTokens? = null
    private val mutableState = MutableStateFlow<SessionState>(SessionState.Loading)

    override val state: StateFlow<SessionState> = mutableState.asStateFlow()

    override suspend fun restore() = mutex.withLock {
        tokens = store.read()
        mutableState.value = if (tokens != null) SessionState.SignedIn else SessionState.SignedOut
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun login(email: String, password: String): LoginResult = try {
        val response = service.login(LoginRequestDto(email.trim(), password))
        mutex.withLock { open(response.toTokens()) }
        LoginResult.Success
    } catch (e: CancellationException) {
        throw e
    } catch (ioe: IOException) {
        LoginResult.NetworkError
    } catch (e: HttpException) {
        when (e.code()) {
            HTTP_UNAUTHORIZED -> LoginResult.InvalidCredentials

            HTTP_TOO_MANY_REQUESTS -> LoginResult.TooManyAttempts

            else -> {
                crashReporter.log("Server error ${e.code()} while signing in")
                LoginResult.UnexpectedError
            }
        }
    } catch (e: Exception) {
        crashReporter.recordException(e)
        LoginResult.UnexpectedError
    }

    override suspend fun logout() {
        val refreshToken = mutex.withLock {
            tokens?.refreshToken.also { end() }
        }
        // The device is already signed out; the server call only revokes the refresh token earlier than its expiry
        if (refreshToken != null) revokeOnServer(refreshToken)
    }

    override suspend fun accessToken(): String? = mutex.withLock {
        val current = tokens
        if (current != null && current.expiresWithin(RENEW_MARGIN)) renew(current) else current?.accessToken
    }

    override suspend fun refresh(rejectedAccessToken: String): String? = mutex.withLock {
        val current = tokens
        when {
            current == null -> null

            // Another request already renewed the token while this one waited for the lock
            current.accessToken != rejectedAccessToken -> current.accessToken

            else -> renew(current)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun renew(current: AuthTokens): String? = try {
        val renewed = service.refresh(RefreshRequestDto(current.refreshToken)).toTokens()
        open(renewed)
        renewed.accessToken
    } catch (e: CancellationException) {
        throw e
    } catch (ioe: IOException) {
        // Offline is not a reason to sign the user out
        null
    } catch (e: HttpException) {
        if (e.code() == HTTP_UNAUTHORIZED) {
            end()
        } else {
            crashReporter.log("Server error ${e.code()} while renewing the session")
        }
        null
    } catch (e: Exception) {
        crashReporter.recordException(e)
        null
    }

    private suspend fun open(newTokens: AuthTokens) {
        store.save(newTokens)
        tokens = newTokens
        mutableState.value = SessionState.SignedIn
    }

    /** Must be called with the lock held. */
    private suspend fun end() {
        store.clear()
        tokens = null
        cleaners.forEach { it.clear() }
        mutableState.value = SessionState.SignedOut
    }

    private suspend fun revokeOnServer(refreshToken: String) {
        try {
            service.logout(RefreshRequestDto(refreshToken))
        } catch (e: CancellationException) {
            throw e
        } catch (ioe: IOException) {
            crashReporter.log("Could not revoke the session on the server: offline")
        } catch (e: HttpException) {
            crashReporter.log("Server error ${e.code()} while revoking the session")
        }
    }

    private fun TokenResponseDto.toTokens() =
        AuthTokens(accessToken, refreshToken, clock.instant().plusSeconds(expiresInSeconds))

    private fun AuthTokens.expiresWithin(margin: Duration) = !clock.instant().plus(margin).isBefore(accessExpiresAt)
}
