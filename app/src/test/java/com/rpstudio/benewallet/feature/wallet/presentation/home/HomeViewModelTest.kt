package com.rpstudio.benewallet.feature.wallet.presentation.home

import com.rpstudio.benewallet.core.auth.FakeSessionManager
import com.rpstudio.benewallet.core.auth.SessionState
import com.rpstudio.benewallet.feature.wallet.FakeWalletRepository
import com.rpstudio.benewallet.feature.wallet.domain.model.Benefit
import com.rpstudio.benewallet.feature.wallet.domain.model.BenefitBalance
import com.rpstudio.benewallet.feature.wallet.domain.model.Money
import com.rpstudio.benewallet.feature.wallet.domain.model.RefreshResult
import com.rpstudio.benewallet.feature.wallet.domain.model.WalletBalances
import com.rpstudio.benewallet.feature.wallet.domain.model.WalletError
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val repository = FakeWalletRepository()
    private val session = FakeSessionManager()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `starts by refreshing without forcing`() = runTest {
        HomeViewModel(repository, session)

        assertEquals(
            "opening the screen must respect the TTL",
            listOf(false),
            repository.refreshCalls
        )
    }

    @Test
    fun `pull to refresh forces the network`() = runTest {
        val viewModel = HomeViewModel(repository, session)

        viewModel.onIntent(HomeIntent.Refresh)

        assertEquals(listOf(false, true), repository.refreshCalls)
    }

    @Test
    fun `state exposes balances and their total`() = runTest {
        val viewModel = HomeViewModel(repository, session)
        backgroundScope.launch(dispatcher) { viewModel.state.collect() }

        repository.balances.value = WalletBalances(
            items = listOf(
                BenefitBalance(Benefit.MEAL, Money(100_000)),
                BenefitBalance(Benefit.FOOD, Money(25_050))
            ),
            lastUpdatedAt = Instant.parse("2026-10-25T12:00:00Z")
        )

        assertEquals(Money(125_050), viewModel.state.value.total)
        assertEquals(false, viewModel.state.value.isRefreshing)
    }

    @Test
    fun `failed refresh emits error effect`() = runTest {
        repository.refreshResult = RefreshResult.Failure(WalletError.Network)
        val viewModel = HomeViewModel(repository, session)
        val effects = mutableListOf<HomeEffect>()
        backgroundScope.launch(dispatcher) { viewModel.effects.collect { effects.add(it) } }

        assertEquals(listOf<HomeEffect>(HomeEffect.ShowError(WalletError.Network)), effects)
    }

    @Test
    fun `asking to sign out only opens the confirmation`() = runTest {
        val viewModel = HomeViewModel(repository, session)
        backgroundScope.launch(dispatcher) { viewModel.state.collect() }

        viewModel.onIntent(HomeIntent.RequestLogout)

        assertTrue(viewModel.state.value.confirmingLogout)
        assertEquals("nothing may be wiped before the user confirms", 0, session.logoutCalls)
    }

    @Test
    fun `canceling the confirmation keeps the session`() = runTest {
        val viewModel = HomeViewModel(repository, session)
        backgroundScope.launch(dispatcher) { viewModel.state.collect() }
        viewModel.onIntent(HomeIntent.RequestLogout)

        viewModel.onIntent(HomeIntent.CancelLogout)

        assertFalse(viewModel.state.value.confirmingLogout)
        assertEquals(0, session.logoutCalls)
        assertEquals(SessionState.SignedIn, session.state.value)
    }

    @Test
    fun `confirming signs out and closes the dialog`() = runTest {
        val viewModel = HomeViewModel(repository, session)
        backgroundScope.launch(dispatcher) { viewModel.state.collect() }
        viewModel.onIntent(HomeIntent.RequestLogout)

        viewModel.onIntent(HomeIntent.ConfirmLogout)

        assertEquals(1, session.logoutCalls)
        assertEquals(SessionState.SignedOut, session.state.value)
        assertFalse(viewModel.state.value.confirmingLogout)
    }
}
