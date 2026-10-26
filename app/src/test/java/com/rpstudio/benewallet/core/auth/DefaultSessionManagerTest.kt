package com.rpstudio.benewallet.core.auth

import com.rpstudio.benewallet.feature.wallet.data.FakeCrashReporter
import com.rpstudio.benewallet.feature.wallet.data.MutableClock
import java.io.IOException
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class DefaultSessionManagerTest {
    private val clock = MutableClock(Instant.parse("2026-10-26T15:00:00Z"))
    private val service = FakeAuthService()
    private val store = FakeTokenStore()
    private val crashReporter = FakeCrashReporter()
    private var cleaned = 0
    private val manager = DefaultSessionManager(
        store,
        service,
        listOf(SessionDataCleaner { cleaned++ }),
        crashReporter,
        clock
    )

    private fun httpError(code: Int) =
        HttpException(Response.error<Any>(code, "".toResponseBody("text/plain".toMediaType())))

    private suspend fun signIn() {
        service.loginResult = { tokenResponse("access-1", "refresh-1") }
        manager.login("rodrigo@benewallet.demo", "pw")
    }

    @Test
    fun `starts loading, then follows what was saved`() = runTest {
        assertEquals(SessionState.Loading, manager.state.value)

        manager.restore()
        assertEquals(SessionState.SignedOut, manager.state.value)

        store.tokens = AuthTokens("a", "r", clock.instant().plusSeconds(900))
        manager.restore()
        assertEquals(SessionState.SignedIn, manager.state.value)
    }

    @Test
    fun `login saves the tokens and signs in`() = runTest {
        service.loginResult = { tokenResponse("access-1", "refresh-1", expiresInSeconds = 900) }

        val result = manager.login("  rodrigo@benewallet.demo ", "pw")

        assertEquals(LoginResult.Success, result)
        assertEquals(SessionState.SignedIn, manager.state.value)
        assertEquals(AuthTokens("access-1", "refresh-1", clock.instant().plusSeconds(900)), store.tokens)
        assertEquals("the e-mail is trimmed", "rodrigo@benewallet.demo", service.loginCalls.single().email)
        assertEquals("access-1", manager.accessToken())
    }

    @Test
    fun `login failures are told apart and leave no session`() = runTest {
        val cases = mapOf<() -> Nothing, LoginResult>(
            { throw httpError(401) } to LoginResult.InvalidCredentials,
            { throw httpError(429) } to LoginResult.TooManyAttempts,
            { throw IOException("offline") } to LoginResult.NetworkError,
            { throw httpError(503) } to LoginResult.UnexpectedError,
            { throw IllegalStateException("bug") } to LoginResult.UnexpectedError
        )

        cases.forEach { (failure, expected) ->
            service.loginResult = failure
            assertEquals(expected, manager.login("a", "b"))
        }

        assertNull(store.tokens)
        assertEquals("only the real bug is reported", 1, crashReporter.recorded.size)
        assertEquals("the 5xx leaves only a breadcrumb", 1, crashReporter.logs.size)
    }

    @Test
    fun `a fresh token is returned as it is`() = runTest {
        signIn()

        assertEquals("access-1", manager.accessToken())
        assertTrue(service.refreshCalls.isEmpty())
    }

    @Test
    fun `a token about to expire is renewed before use`() = runTest {
        signIn()
        service.refreshResult = { tokenResponse("access-2", "refresh-2") }

        clock.advance(Duration.ofSeconds(900 - 20))

        assertEquals("access-2", manager.accessToken())
        assertEquals(listOf("refresh-1"), service.refreshCalls)
        assertEquals("refresh-2", store.tokens?.refreshToken)
    }

    @Test
    fun `a rejected token is renewed and saved`() = runTest {
        signIn()
        service.refreshResult = { tokenResponse("access-2", "refresh-2") }

        assertEquals("access-2", manager.refresh("access-1"))

        assertEquals("access-2", store.tokens?.accessToken)
    }

    @Test
    fun `a second request rejected with the old token reuses the renewed one`() = runTest {
        signIn()
        service.refreshResult = { tokenResponse("access-2", "refresh-2") }
        manager.refresh("access-1")

        val second = manager.refresh("access-1")

        assertEquals("access-2", second)
        assertEquals("the refresh token is single-use, so it must be sent only once", 1, service.refreshCalls.size)
    }

    @Test
    fun `when the server refuses the refresh token the session ends and the data is wiped`() = runTest {
        signIn()
        service.refreshResult = { throw httpError(401) }

        assertNull(manager.refresh("access-1"))

        assertEquals(SessionState.SignedOut, manager.state.value)
        assertNull(store.tokens)
        assertEquals(1, cleaned)
        assertNull(manager.accessToken())
    }

    @Test
    fun `being offline while renewing does not sign the user out`() = runTest {
        signIn()
        service.refreshResult = { throw IOException("offline") }

        assertNull(manager.refresh("access-1"))

        assertEquals(SessionState.SignedIn, manager.state.value)
        assertEquals("refresh-1", store.tokens?.refreshToken)
        assertEquals(0, cleaned)
    }

    @Test
    fun `logout ends the session here and revokes the refresh token on the server`() = runTest {
        signIn()

        manager.logout()

        assertEquals(SessionState.SignedOut, manager.state.value)
        assertNull(store.tokens)
        assertEquals(1, cleaned)
        assertEquals(listOf("refresh-1"), service.logoutCalls)
    }

    @Test
    fun `logout finishes even when the caller is canceled halfway`() = runTest {
        // A store that suspends while clearing: the cancel arrives exactly in the middle of the sign-out
        val slowStore = object : TokenStore {
            var cleared = false

            override suspend fun read(): AuthTokens? = null

            override suspend fun save(tokens: AuthTokens) = Unit

            override suspend fun clear() {
                yield()
                cleared = true
            }
        }
        val slowManager = DefaultSessionManager(
            slowStore,
            service,
            listOf(SessionDataCleaner { cleaned++ }),
            crashReporter,
            clock
        )

        val job = launch(start = CoroutineStart.UNDISPATCHED) { slowManager.logout() }
        job.cancel()
        job.join()

        assertTrue("the tokens must be gone", slowStore.cleared)
        assertEquals("the cached data must be wiped", 1, cleaned)
        assertEquals(SessionState.SignedOut, slowManager.state.value)
    }

    @Test
    fun `logout works offline`() = runTest {
        signIn()
        service.logoutResult = { throw IOException("offline") }

        manager.logout()

        assertEquals(SessionState.SignedOut, manager.state.value)
        assertNull(store.tokens)
        assertTrue("offline is expected, nothing to report", crashReporter.recorded.isEmpty())
    }
}
