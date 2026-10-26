package com.rpstudio.benewallet.feature.wallet.presentation.statement

import com.rpstudio.benewallet.core.analytics.FakeAnalytics
import com.rpstudio.benewallet.core.featureflags.FakeFeatureFlagsRepository
import com.rpstudio.benewallet.feature.wallet.data.FakeCrashReporter
import com.rpstudio.benewallet.feature.wallet.data.FakeWalletLocalDataSource
import com.rpstudio.benewallet.feature.wallet.data.FakeWalletService
import com.rpstudio.benewallet.feature.wallet.data.MutableClock
import com.rpstudio.benewallet.feature.wallet.data.remote.dto.BenefitDto
import com.rpstudio.benewallet.feature.wallet.data.remote.dto.CategoryDto
import com.rpstudio.benewallet.feature.wallet.data.remote.dto.TransactionDto
import com.rpstudio.benewallet.feature.wallet.data.remote.dto.TransactionsResponseDto
import com.rpstudio.benewallet.feature.wallet.data.repository.DefaultWalletRepository
import com.rpstudio.benewallet.feature.wallet.domain.model.Category
import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
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

/**
 * The "showing saved data of another filter, connect to update" banner is for being offline. These tests run the real
 * ViewModel and repository (only the network and Room are fakes) to check what the screen would show while a new
 * filter is still loading, and after it fails.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StatementPartialBannerTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val service = FakeWalletService()
    private val clock = MutableClock(Instant.parse("2026-10-25T15:00:00Z"))
    private val local = FakeWalletLocalDataSource()
    private val repository = DefaultWalletRepository(
        service = service,
        local = local,
        crashReporter = FakeCrashReporter(),
        ioDispatcher = dispatcher,
        clock = clock
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun page(vararg ids: Long, category: CategoryDto) = TransactionsResponseDto(
        items = ids.map {
            TransactionDto(
                it,
                BenefitDto.MEAL,
                category,
                "Merchant $it",
                -1000,
                "2026-10-25T14:00:00Z"
            )
        },
        nextCursor = null
    )

    private fun TestScope.openStatement(): StatementViewModel {
        service.transactionsResult = { page(1, 2, category = CategoryDto.RESTAURANT) }
        val viewModel = StatementViewModel(
            repository,
            FakeFeatureFlagsRepository(),
            FakeAnalytics(),
            clock
        )
        backgroundScope.launch(dispatcher) { viewModel.state.collect() }
        return viewModel
    }

    @Test
    fun `while the new filter loads the banner is not shown`() = runTest {
        val viewModel = openStatement()
        val network = CompletableDeferred<Unit>()
        service.transactionsResult = {
            network.await()
            page(8, category = CategoryDto.DELIVERY)
        }

        viewModel.onIntent(StatementIntent.SelectCategory(Category.DELIVERY))

        val loading = viewModel.state.value
        assertTrue("the request is still in flight", loading.isRefreshing)
        assertFalse("online and loading is not 'connect to update'", loading.isPartial)

        network.complete(Unit)
        val loaded = viewModel.state.value
        assertFalse(loaded.isRefreshing)
        assertFalse(loaded.isPartial)
        assertEquals(listOf(8L), loaded.items.map { it.id })
    }

    @Test
    fun `the banner does not come back in the gap between the request and the database notifying`() = runTest {
        val viewModel = openStatement()
        local.holdNotifications = true
        service.transactionsResult = { page(8, category = CategoryDto.DELIVERY) }

        // The request finishes and the snapshot is committed, but the observers have not been told yet
        viewModel.onIntent(StatementIntent.SelectCategory(Category.DELIVERY))

        val gap = viewModel.state.value
        assertFalse("the refresh succeeded: there is nothing to apologise for", gap.isPartial)
        assertTrue(
            "the list is empty here (old snapshot, new filter): it must keep showing the spinner",
            gap.items.isEmpty()
        )
        assertTrue(
            "an empty list that is not 'loading' would flash 'nothing found'",
            gap.isRefreshing
        )

        local.flushNotifications()
        val settled = viewModel.state.value
        assertFalse(settled.isPartial)
        assertEquals(listOf(8L), settled.items.map { it.id })
    }

    @Test
    fun `when the refresh fails the banner explains why the list is from another filter`() = runTest {
        val viewModel = openStatement()
        service.transactionsResult = { throw IOException("offline") }

        viewModel.onIntent(StatementIntent.SelectCategory(Category.DELIVERY))

        val offline = viewModel.state.value
        assertFalse(offline.isRefreshing)
        assertTrue(
            "now the banner is true: the snapshot is of another filter and cannot be updated",
            offline.isPartial
        )
    }
}
