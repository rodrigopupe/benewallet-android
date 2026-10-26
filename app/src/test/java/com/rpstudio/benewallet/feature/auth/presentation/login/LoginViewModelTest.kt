package com.rpstudio.benewallet.feature.auth.presentation.login

import com.rpstudio.benewallet.core.auth.FakeSessionManager
import com.rpstudio.benewallet.core.auth.LoginResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// Values any sensible form rule accepts: these tests are about the flow, not about the rule (see LoginFormTest)
private const val EMAIL = "rodrigo@benewallet.demo"
private const val PASSWORD = "benewallet123"

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    private val session = FakeSessionManager()
    private val viewModel by lazy { LoginViewModel(session) }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun fill(email: String = EMAIL, password: String = PASSWORD) {
        viewModel.onIntent(LoginIntent.EmailChanged(email))
        viewModel.onIntent(LoginIntent.PasswordChanged(password))
    }

    @Test
    fun `typing updates the form`() = runTest {
        fill()

        assertEquals(EMAIL, viewModel.state.value.email)
        assertEquals(PASSWORD, viewModel.state.value.password)
    }

    @Test
    fun `an empty form is not sent`() = runTest {
        viewModel.onIntent(LoginIntent.Submit)
        fill(password = "")
        viewModel.onIntent(LoginIntent.Submit)
        fill(email = "   ")
        viewModel.onIntent(LoginIntent.Submit)

        assertTrue("no request may leave", session.loginCalls.isEmpty())
        assertFalse(viewModel.state.value.canSubmit)
    }

    @Test
    fun `submitting sends what was typed and then clears the form`() = runTest {
        fill()

        viewModel.onIntent(LoginIntent.Submit)

        assertEquals(listOf(EMAIL to PASSWORD), session.loginCalls)
        assertEquals("the password must not outlive the login", LoginState(), viewModel.state.value)
    }

    @Test
    fun `the form is locked while the request is in flight`() = runTest {
        session.loginGate = CompletableDeferred()
        fill()

        viewModel.onIntent(LoginIntent.Submit)

        assertTrue(viewModel.state.value.isLoading)
        assertFalse("a second tap must not send again", viewModel.state.value.canSubmit)
        viewModel.onIntent(LoginIntent.Submit)
        assertEquals(1, session.loginCalls.size)

        session.loginGate?.complete(Unit)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `each failure shows its own message and keeps what was typed`() = runTest {
        val cases = mapOf(
            LoginResult.InvalidCredentials to LoginError.INVALID_CREDENTIALS,
            LoginResult.TooManyAttempts to LoginError.TOO_MANY_ATTEMPTS,
            LoginResult.NetworkError to LoginError.NETWORK,
            LoginResult.UnexpectedError to LoginError.UNEXPECTED
        )
        fill()

        cases.forEach { (result, expected) ->
            session.loginResult = result
            viewModel.onIntent(LoginIntent.Submit)

            assertEquals(expected, viewModel.state.value.error)
            assertFalse(viewModel.state.value.isLoading)
            assertEquals(PASSWORD, viewModel.state.value.password)
        }
    }

    @Test
    fun `editing the form dismisses the error`() = runTest {
        session.loginResult = LoginResult.InvalidCredentials
        fill()
        viewModel.onIntent(LoginIntent.Submit)

        viewModel.onIntent(LoginIntent.PasswordChanged("$PASSWORD!"))

        assertNull(viewModel.state.value.error)
    }
}
