package com.rpstudio.benewallet.bff

import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Base64

private val ACCESS_TTL: Duration = Duration.ofMinutes(15)
private val REFRESH_TTL: Duration = Duration.ofDays(7)
private val LOCKOUT: Duration = Duration.ofMinutes(1)
private const val TOKEN_BYTES = 32
private const val MAX_FAILED_LOGINS = 5
private const val DEFAULT_SEED = 42L
private const val SECOND_USER_SEED = 7L
internal const val DEMO_PASSWORD = "benewallet123"

internal class DemoUser(val id: String, val email: String, val passwordHash: PasswordHash, val seed: Long)

internal fun demoUsers(): List<DemoUser> = listOf(
    DemoUser("user-rodrigo", "rodrigo@benewallet.demo", PasswordHasher.hash(DEMO_PASSWORD), DEFAULT_SEED),
    DemoUser("user-bruno", "bruno@benewallet.demo", PasswordHasher.hash(DEMO_PASSWORD), SECOND_USER_SEED)
)

internal data class TokenPair(val accessToken: String, val refreshToken: String, val expiresInSeconds: Long)

internal sealed interface LoginResult {
    data class Success(val tokens: TokenPair) : LoginResult

    object InvalidCredentials : LoginResult

    object LockedOut : LoginResult
}

private class AccessEntry(val sessionId: String, val expiresAt: Instant)

private class RefreshEntry(val sessionId: String, val expiresAt: Instant, var used: Boolean = false)

private class FailedLogins(var count: Int = 0, var lockedUntil: Instant = Instant.MIN)

/**
 * In-memory sessions for the demo: opaque random tokens (not JWTs), so revoking is just forgetting them.
 * Refresh tokens rotate on every use; presenting one twice means it leaked, and the whole session is revoked.
 */
internal class AuthService(private val clock: Clock, users: List<DemoUser> = demoUsers()) {
    private val usersByEmail = users.associateBy { it.email }
    private val usersById = users.associateBy { it.id }
    private val random = SecureRandom()
    private val lock = Any()

    private val sessions = HashMap<String, String>() // session id -> user id
    private val accessTokens = HashMap<String, AccessEntry>()
    private val refreshTokens = HashMap<String, RefreshEntry>()
    private val failures = HashMap<String, FailedLogins>()

    // Verified when the e-mail is unknown, so a wrong user and a wrong password take the same time
    private val decoy = PasswordHasher.hash(newToken())

    fun seedFor(userId: String): Long = checkNotNull(usersById[userId]) { "unknown user" }.seed

    fun login(email: String, password: String): LoginResult = synchronized(lock) {
        val key = email.trim().lowercase()
        val now = clock.instant()
        val attempts = failures.getOrPut(key) { FailedLogins() }
        if (now.isBefore(attempts.lockedUntil)) return LoginResult.LockedOut

        val user = usersByEmail[key]
        val valid = PasswordHasher.verify(password, user?.passwordHash ?: decoy) && user != null
        if (!valid) {
            attempts.count++
            if (attempts.count >= MAX_FAILED_LOGINS) {
                attempts.count = 0
                attempts.lockedUntil = now.plus(LOCKOUT)
            }
            return LoginResult.InvalidCredentials
        }

        failures.remove(key)
        val sessionId = newToken()
        sessions[sessionId] = user.id
        LoginResult.Success(issue(sessionId))
    }

    /** Returns the new pair, or null when the token is unknown, expired, revoked or was already used. */
    fun refresh(refreshToken: String): TokenPair? = synchronized(lock) {
        val entry = refreshTokens[refreshToken] ?: return null
        if (entry.used) {
            revoke(entry.sessionId)
            return null
        }
        if (clock.instant().isAfter(entry.expiresAt) || entry.sessionId !in sessions) return null
        entry.used = true
        issue(entry.sessionId)
    }

    fun logout(refreshToken: String) = synchronized(lock) {
        refreshTokens[refreshToken]?.let { revoke(it.sessionId) }
    }

    /** User id of a valid access token, or null. */
    fun userIdFor(accessToken: String): String? = synchronized(lock) {
        val entry = accessTokens[accessToken] ?: return null
        if (clock.instant().isAfter(entry.expiresAt)) return null
        sessions[entry.sessionId]
    }

    private fun issue(sessionId: String): TokenPair {
        val now = clock.instant()
        val access = newToken()
        val refresh = newToken()
        accessTokens[access] = AccessEntry(sessionId, now.plus(ACCESS_TTL))
        refreshTokens[refresh] = RefreshEntry(sessionId, now.plus(REFRESH_TTL))
        return TokenPair(access, refresh, ACCESS_TTL.seconds)
    }

    private fun revoke(sessionId: String) {
        sessions.remove(sessionId)
        accessTokens.values.removeIf { it.sessionId == sessionId }
        refreshTokens.values.removeIf { it.sessionId == sessionId }
    }

    private fun newToken(): String {
        val bytes = ByteArray(TOKEN_BYTES).also(random::nextBytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
