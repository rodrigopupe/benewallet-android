package com.rpstudio.benewallet.app

import com.rpstudio.benewallet.core.auth.FakeSessionManager
import com.rpstudio.benewallet.core.auth.SessionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {
    private val session = FakeSessionManager()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `opening the app reads the saved session once`() = runTest {
        AppViewModel(session)

        assertEquals(1, session.restoreCalls)
    }

    @Test
    fun `the root follows the session, including a sign-out that happens while the app is open`() = runTest {
        val viewModel = AppViewModel(session)
        assertEquals(SessionState.SignedIn, viewModel.sessionState.value)

        session.logout()

        assertEquals(SessionState.SignedOut, viewModel.sessionState.value)
    }
}
