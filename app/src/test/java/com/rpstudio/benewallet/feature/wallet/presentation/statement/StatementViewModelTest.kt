package com.rpstudio.benewallet.feature.wallet.presentation.statement

import com.rpstudio.benewallet.core.analytics.AnalyticsEvent
import com.rpstudio.benewallet.core.analytics.FakeAnalytics
import com.rpstudio.benewallet.feature.wallet.FakeWalletRepository
import com.rpstudio.benewallet.feature.wallet.domain.model.Category
import com.rpstudio.benewallet.feature.wallet.domain.model.RefreshResult
import com.rpstudio.benewallet.feature.wallet.domain.model.StatementFilter
import com.rpstudio.benewallet.feature.wallet.domain.model.WalletError
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StatementViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val repository = FakeWalletRepository()
    private val analytics = FakeAnalytics()
    private val clock = Clock.fixed(Instant.parse("2026-10-25T15:00:00Z"), ZoneOffset.UTC)

    private fun createViewModel() = StatementViewModel(repository, analytics, clock)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `starts by refreshing the default filter without forcing`() = runTest {
        createViewModel()

        assertEquals(listOf(StatementFilter() to false), repository.statementRefreshCalls)
    }

    @Test
    fun `selecting a category refreshes with that filter and updates state`() = runTest {
        val viewModel = createViewModel()
        backgroundScope.launch(dispatcher) { viewModel.state.collect() }

        viewModel.onIntent(StatementIntent.SelectCategory(Category.DELIVERY))

        assertEquals(
            StatementFilter(category = Category.DELIVERY),
            repository.statementRefreshCalls.last().first
        )
        assertEquals(Category.DELIVERY, viewModel.state.value.category)
    }

    @Test
    fun `selecting a period computes days from the clock`() = runTest {
        val viewModel = createViewModel()

        viewModel.onIntent(StatementIntent.SelectPeriod(PeriodFilter.LAST_7_DAYS))
        assertEquals(
            StatementFilter(
                from = LocalDate.of(2026, 10, 19),
                to = LocalDate.of(2026, 10, 25)
            ),
            repository.statementRefreshCalls.last().first
        )

        viewModel.onIntent(StatementIntent.SelectPeriod(PeriodFilter.THIS_MONTH))
        assertEquals(
            StatementFilter(
                from = LocalDate.of(2026, 10, 1),
                to = LocalDate.of(2026, 10, 25)
            ),
            repository.statementRefreshCalls.last().first
        )
    }

    @Test
    fun `failed load more offers retry and reports the error`() = runTest {
        repository.loadMoreResult = RefreshResult.Failure(WalletError.Network)
        val viewModel = createViewModel()
        val effects = mutableListOf<StatementEffect>()
        backgroundScope.launch(dispatcher) { viewModel.state.collect() }
        backgroundScope.launch(dispatcher) { viewModel.effects.collect { effects.add(it) } }

        viewModel.onIntent(StatementIntent.LoadMore)

        assertEquals(
            "the footer must switch to a retry button",
            true,
            viewModel.state.value.loadMoreFailed
        )
        assertEquals(
            listOf<StatementEffect>(StatementEffect.ShowError(WalletError.Network)),
            effects
        )
    }

    @Test
    fun `choosing filters tracks the event but opening the screen does not`() = runTest {
        val viewModel = createViewModel()
        assertEquals(
            "opening the screen is not a filter",
            emptyList<AnalyticsEvent>(),
            analytics.events
        )

        viewModel.onIntent(StatementIntent.SelectCategory(Category.DELIVERY))
        viewModel.onIntent(StatementIntent.SelectPeriod(PeriodFilter.LAST_7_DAYS))

        assertEquals(
            listOf(
                AnalyticsEvent.StatementFilterApplied("DELIVERY", "ALL"),
                AnalyticsEvent.StatementFilterApplied("DELIVERY", "LAST_7_DAYS")
            ),
            analytics.events
        )
    }
}
